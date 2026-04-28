package com.onboardguard.watchlist.service.impl;

import com.onboardguard.shared.common.enums.CategoryCode;
import com.onboardguard.watchlist.dto.WatchlistCategoryDto;
import com.onboardguard.watchlist.dto.WatchlistEntryResponseDto;
import com.onboardguard.watchlist.elasticsearch.WatchlistDocument;
import com.onboardguard.watchlist.entity.WatchlistCategory;
import com.onboardguard.watchlist.entity.WatchlistEntry;
import com.onboardguard.watchlist.mapper.WatchlistMapper;
import com.onboardguard.watchlist.repository.*;
import com.onboardguard.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Crucial: Disables Hibernate dirty checking for massive speed boosts
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistEntryRepository entryRepository;
    private final WatchlistCategoryRepository categoryRepository;
    private final WatchlistAliasRepository aliasRepository;
    private final WatchlistEvidenceDocRepository evidenceRepository;

    private final ElasticsearchOperations elasticsearchOperations;
    private final WatchlistMapper watchlistMapper;

    // ══════════════════════════════════════════════════════════════
    // UI ENDPOINTS (Called by WatchlistController)
    // ══════════════════════════════════════════════════════════════

    /**
     * Gets a paginated list of all active entries for the Admin Data Grid.
     */
    @Override
    public Page<WatchlistEntryResponseDto> getAllActiveEntries(Pageable pageable) {
        log.debug("Fetching paginated watchlist entries");
        return entryRepository.findAllActiveAndEffective(pageable)
                .map(watchlistMapper::toResponseDto);
    }

    /**
     * Gets a single profile, fetching related aliases and evidence automatically.
     */
    @Override
    public WatchlistEntryResponseDto getEntryDetails(Long entryId) {
        log.debug("Fetching details for Watchlist Entry ID: {}", entryId);
        WatchlistEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist entry not found"));

        WatchlistEntryResponseDto response = watchlistMapper.toResponseDto(entry);

        // Append related nested data
        response.setAliases(aliasRepository.findByEntryId(entryId).stream()
                .map(watchlistMapper::toAliasDto).collect(Collectors.toList()));

        response.setEvidenceDocuments(evidenceRepository.findByEntryId(entryId).stream()
                .map(watchlistMapper::toEvidenceDto).collect(Collectors.toList()));

        return response;
    }

    /**
     * Gets dropdown data for the UI filters.
     */
    @Override
    public List<WatchlistCategoryDto> getActiveCategories() {
        return categoryRepository.findAll().stream()
                .filter(WatchlistCategory::getIsActive)
                .map(watchlistMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════
    // INTERNAL API (Called by Screening Module & Manual Search API)
    // ══════════════════════════════════════════════════════════════

    /**
     * TIER 1: Exact ID Match
     * Called by the Screening module to check for undeniable government ID hits.
     */
    @Override
    public List<WatchlistEntryResponseDto> findExactIdMatch(String panNumber, String aadhaarNumber, String cin, String din) {
        log.info("Searching DB for exact PAN/Aadhaar/cin/din match...");

        if (panNumber == null && aadhaarNumber == null) {
            return List.of();
        }

        List<WatchlistEntry> exactMatches = entryRepository.findByPanNumberOrAadhaarNumberOrDinNumberOrCinNumber(panNumber, aadhaarNumber , cin ,din);

        return exactMatches.stream()
                .map(watchlistMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * TIER 2: Fuzzy Name Match
     * Used by the Frontend "Manual Search" bar AND the automated Screening module.
     */
    @Override
    public List<WatchlistEntryResponseDto> searchRawDictionary(String searchName) {
        log.info("Executing fuzzy Elasticsearch match for: {}", searchName);

        NativeQuery fuzzyQuery = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(m -> m
                                .fields("primaryName", "aliases")
                                .query(searchName)
                                .fuzziness("AUTO") // Automatically handles character typos
                        )
                )
                .build();

        SearchHits<WatchlistDocument> esHits = elasticsearchOperations.search(fuzzyQuery, WatchlistDocument.class);

        // Extract internal Postgres IDs from the Elasticsearch results
        List<Long> matchedIds = esHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .filter(WatchlistDocument::getIsActive) // Only return active, verified entries
                .map(doc -> Long.valueOf(doc.getId()))
                .collect(Collectors.toList());

        if (matchedIds.isEmpty()) {
            return List.of();
        }

        // Fetch full rich data from Postgres using the matched IDs
        List<WatchlistEntry> fullEntries = entryRepository.findAllById(matchedIds);
        return fullEntries.stream()
                .map(watchlistMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}