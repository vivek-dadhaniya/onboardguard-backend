package com.onboardguard.watchlist.service.impl;

import com.onboardguard.shared.common.events.WatchlistEntryCreatedEvent;
import com.onboardguard.shared.common.events.WatchlistEntryDeactivatedEvent;
import com.onboardguard.shared.common.events.WatchlistEntryUpdatedEvent;
import com.onboardguard.watchlist.elasticsearch.WatchlistDocument;
import com.onboardguard.watchlist.elasticsearch.WatchlistSearchRepository;
import com.onboardguard.watchlist.entity.WatchlistEntry;
import com.onboardguard.watchlist.repository.WatchlistAliasRepository;
import com.onboardguard.watchlist.repository.WatchlistEntryRepository;
import com.onboardguard.watchlist.service.WatchlistSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!test")
public class WatchlistSyncServiceImpl implements WatchlistSyncService {

    private final WatchlistEntryRepository entryRepository;
    private final WatchlistAliasRepository aliasRepository;
    private final WatchlistSearchRepository esRepository;

    // ══════════════════════════════════════════════════════════════
    // 1. BULK SYNC (Runs once at Startup)
    // ══════════════════════════════════════════════════════════════

    @Override
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void syncAllToElasticsearchOnStartup() {
        log.info("System Boot: Checking Elasticsearch Synchronization...");

        if (esRepository.count() > 0) {
            log.info("Elasticsearch already contains Watchlist data. Skipping bulk sync.");
            return;
        }

        log.warn("Elasticsearch is empty! Seeding data from PostgreSQL...");
        List<WatchlistEntry> allEntries = entryRepository.findAll();

        if (allEntries.isEmpty()) {
            log.info("No records found in PostgreSQL. Ensure Flyway scripts have run.");
            return;
        }

        // Use the shared helper method to map all entries
        List<WatchlistDocument> documents = allEntries.stream()
                .map(this::mapToDocument)
                .collect(Collectors.toList());

        esRepository.saveAll(documents); // BULK save is much faster for startup
        log.info("Successfully seeded {} Watchlist records into Elasticsearch.", documents.size());
    }

    // ══════════════════════════════════════════════════════════════
    // 2. REAL-TIME SYNC (Triggered by Events)
    // ══════════════════════════════════════════════════════════════

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEntryCreatedOrUpdated(WatchlistEntryCreatedEvent event) {
        log.info("Syncing New Entry ID {} to Elasticsearch", event.entryId());
        syncSingleEntryToElasticsearch(event.entryId());
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEntryUpdated(WatchlistEntryUpdatedEvent event) {
        syncSingleEntryToElasticsearch(event.entryId());
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEntryDeactivated(WatchlistEntryDeactivatedEvent event) {
        syncSingleEntryToElasticsearch(event.entryId());
    }

    @Override
    public void syncSingleEntryToElasticsearch(Long entryId) {
        WatchlistEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found in DB"));

        // Use the shared helper method for this single entry
        WatchlistDocument document = mapToDocument(entry);

        esRepository.save(document); // SINGLE save
        log.info("Successfully updated Watchlist Document {} in Elasticsearch", document.getId());
    }

    // ══════════════════════════════════════════════════════════════
    // 3. SHARED MAPPING LOGIC (Keeps code DRY)
    // ══════════════════════════════════════════════════════════════

    private WatchlistDocument mapToDocument(WatchlistEntry entry) {
        List<String> aliases = aliasRepository.findByEntryId(entry.getId()).stream()
                .map(alias -> alias.getAliasNameNormalized())
                .collect(Collectors.toList());

        return WatchlistDocument.builder()
                .id(entry.getId().toString())
                .primaryName(entry.getPrimaryNameNormalized())
                .aliases(aliases)
                .categoryCode(entry.getCategory().getCode().name())
                .severity(String.valueOf(entry.getSeverity()))
                .organizationName(entry.getOrganizationName())
                .isActive(entry.getIsActive())
                .build();
    }
}