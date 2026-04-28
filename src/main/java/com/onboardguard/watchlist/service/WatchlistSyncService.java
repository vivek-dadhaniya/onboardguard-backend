package com.onboardguard.watchlist.service;

import com.onboardguard.shared.common.events.WatchlistEntryCreatedEvent;
import com.onboardguard.shared.common.events.WatchlistEntryDeactivatedEvent;
import com.onboardguard.shared.common.events.WatchlistEntryUpdatedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public interface WatchlistSyncService {
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    void syncAllToElasticsearchOnStartup();

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleEntryCreatedOrUpdated(WatchlistEntryCreatedEvent event);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleEntryUpdated(WatchlistEntryUpdatedEvent event);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleEntryDeactivated(WatchlistEntryDeactivatedEvent event);

    void syncSingleEntryToElasticsearch(Long entryId);
}
