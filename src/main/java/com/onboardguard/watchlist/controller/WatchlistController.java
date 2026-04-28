package com.onboardguard.watchlist.controller;

import com.onboardguard.shared.common.dto.ApiResponse;
import com.onboardguard.watchlist.dto.WatchlistCategoryDto;
import com.onboardguard.watchlist.dto.WatchlistEntryResponseDto;
import com.onboardguard.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    /**
     * 1. GET ALL ENTRIES (Browse the Catalog)
     * Used by the UI Data Grid to show all currently active restricted entities.
     * Supports pagination via ?page=0&size=20
     */
    @GetMapping
    // @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_OFFICER_L2', 'ROLE_ADMIN'
    // , 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<WatchlistEntryResponseDto>>> getAllActiveEntries(Pageable pageable) {
        Page<WatchlistEntryResponseDto> pageData = watchlistService.getAllActiveEntries(pageable);
        return ResponseEntity.ok(ApiResponse.success("Fetched watchlist entries", pageData));
    }

    /**
     * 2. SEARCH WATCHLIST (Manual Dictionary Lookup)
     * This is a simple fuzzy search for an Officer who wants to manually check a
     * name
     * outside of the automated candidate onboarding flow.
     */
    @GetMapping("/search")
    // @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_OFFICER_L2', 'ROLE_ADMIN'
    // , 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<WatchlistEntryResponseDto>>> manualSearch(
            @RequestParam String name) {

        List<WatchlistEntryResponseDto> results = watchlistService.searchRawDictionary(name);
        return ResponseEntity.ok(ApiResponse.success("Search complete", results));
    }

    /**
     * 3. GET ENTRY DETAILS
     * Fetches the full profile (including aliases and evidence documents) of a
     * specific flagged entry.
     */
    @GetMapping("/{entryId}")
    // @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_OFFICER_L2', 'ROLE_ADMIN'
    // , 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<WatchlistEntryResponseDto>> getEntryDetails(@PathVariable Long entryId) {
        WatchlistEntryResponseDto details = watchlistService.getEntryDetails(entryId);
        return ResponseEntity.ok(ApiResponse.success("Fetched entry details", details));
    }

    /**
     * 4. GET CATEGORIES
     * Used to populate dropdown filters on the frontend UI.
     */
    @GetMapping("/categories")
    // @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1', 'ROLE_OFFICER_L2', 'ROLE_ADMIN'
    // , 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<WatchlistCategoryDto>>> getCategories() {
        List<WatchlistCategoryDto> categories = watchlistService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Fetched categories", categories));
    }

    // /**
    // * THE MATCHING ENGINE TRIGGER
    // * Takes candidate data and runs it through Elasticsearch to find fuzzy
    // matches.
    // * Uses POST because the candidate payload can be large and contains PII.
    // */
    // @PostMapping("/match")
    // @PreAuthorize("hasAnyRole('ROLE_OFFICER_L1' ,'ROLE_OFFICER_L2', 'ROLE_ADMIN',
    // 'ROLE_SUPER_ADMIN')")
    // public ResponseEntity<ApiResponse<List<WatchlistEntryResponseDto>>>
    // matchCandidate(
    // @RequestBody @Valid CandidateMatchRequestDto request) {
    //
    // List<WatchlistEntryResponseDto> matches =
    // watchlistService.findMatches(request);
    // return ResponseEntity.ok(ApiResponse.success("Matching completed", matches));
    // }

    // @PostMapping("/request")
    // @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    // public ResponseEntity<String> requestCreateEntry(@RequestBody
    // WatchlistEntryRequestDto requestDto) throws Exception {
    // // Extract Maker ID from Spring Security
    // String currentUser =
    // SecurityContextHolder.getContext().getAuthentication().getName();
    // requestDto.setRequestedBy(currentUser);
    //
    // watchlistService.requestCreateEntry(requestDto);
    // return ResponseEntity.accepted().body("Watchlist entry creation requested.
    // Pending Checker approval.");
    // }
    //
    // @PostMapping("/approve/{pendingId}")
    // @PreAuthorize("hasRole('SUPER_ADMIN')")
    // public ResponseEntity<String> approveEntry(@PathVariable Long pendingId)
    // throws Exception {
    // String currentUser =
    // SecurityContextHolder.getContext().getAuthentication().getName();
    // watchlistService.approveWatchlistEntry(pendingId, currentUser);
    //
    // return ResponseEntity.ok("Watchlist entry approved and synchronized to search
    // cluster.");
    // }
    //
    // @PostMapping("/{entryId}/aliases")
    // @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    // public ResponseEntity<String> addAlias(@PathVariable Long entryId,
    // @RequestParam String aliasName, @RequestParam String type) {
    // watchlistService.addAlias(entryId, aliasName, type);
    // return ResponseEntity.ok("Alias added successfully.");
    // }

}