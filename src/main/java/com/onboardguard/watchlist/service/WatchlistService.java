package com.onboardguard.watchlist.service;

import com.onboardguard.watchlist.dto.WatchlistCategoryDto;
import com.onboardguard.watchlist.dto.WatchlistEntryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WatchlistService {
    Page<WatchlistEntryResponseDto> getAllActiveEntries(Pageable pageable);

    WatchlistEntryResponseDto getEntryDetails(Long entryId);

    List<WatchlistCategoryDto> getActiveCategories();

    List<WatchlistEntryResponseDto> findExactIdMatch(String panNumber, String aadhaarNumber, String cin, String din);

    List<WatchlistEntryResponseDto> searchRawDictionary(String searchName);
}
