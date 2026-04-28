package com.onboardguard.screening.repository;

import com.onboardguard.screening.entity.ScreeningMatch;
import com.onboardguard.screening.enums.MatchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScreeningMatchRepository extends JpaRepository<ScreeningMatch, Long> {

    // All matches for a given screening result (used in case detail view)
    List<ScreeningMatch> findByScreeningResultId(Long screeningResultId);

    // For analytics — which watchlist categories generate the most hits
    @Query("""
        SELECT sm.watchlistCategorySnapshot, COUNT(sm)
        FROM ScreeningMatch sm
        GROUP BY sm.watchlistCategorySnapshot
        ORDER BY COUNT(sm) DESC
    """)
    List<Object[]> countGroupedByCategory();

    // For analytics — match type distribution
    @Query("""
        SELECT sm.matchType, COUNT(sm)
        FROM ScreeningMatch sm
        GROUP BY sm.matchType
    """)
    List<Object[]> countGroupedByMatchType();

    // Check if a specific watchlist entry generated matches (before deactivation guard)
    boolean existsByWatchlistEntryId(Long watchlistEntryId);
}