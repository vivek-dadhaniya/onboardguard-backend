package com.onboardguard.watchlist.repository;

import com.onboardguard.watchlist.entity.WatchlistEntry;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry , Long> {

    // Validates if entry is active AND today's date falls between effectiveFrom and effectiveTo
    @Query("SELECT e FROM WatchlistEntry e WHERE e.isActive = true " +
            "AND (e.effectiveFrom IS NULL OR e.effectiveFrom  <= CURRENT_DATE )" +
            "AND (e.effectiveTo IS NULL OR e.effectiveTo >= CURRENT_DATE )")
    Page<WatchlistEntry> findAllActiveAndEffective(Pageable pageable);

    Page<WatchlistEntry> findByIsActiveTrue(Pageable pageable);

    // ── Tier-1 exact ID match (called by WatchlistService / Screening) ────────
    //
    // FIX: Previous version only guarded on pan/aadhaar being null but still
    // passed null values to the query when only din/cin was provided, causing
    // all four parameters to be null and returning no rows.  The query itself
    // is correct (OR conditions handle nulls gracefully); the null-guard in the
    // service layer is what needed fixing — but we also add an active+effective
    // filter here so expired entries never return from Tier-1.

    @Query("SELECT e FROM WatchlistEntry e WHERE e.isActive = true " +
            "AND (e.effectiveFrom IS NULL OR e.effectiveFrom <= CURRENT_DATE) " +
            "AND (e.effectiveTo   IS NULL OR e.effectiveTo   >= CURRENT_DATE) " +
            "AND ((:pan     IS NOT NULL AND e.panNumber     = :pan)     OR " +
            "     (:aadhaar IS NOT NULL AND e.aadhaarNumber = :aadhaar) OR " +
            "     (:din     IS NOT NULL AND e.dinNumber     = :din)     OR " +
            "     (:cin     IS NOT NULL AND e.cinNumber     = :cin))")
    List<WatchlistEntry> findActiveExactIdMatch(
            @Param("pan")     String pan,
            @Param("aadhaar") String aadhaar,
            @Param("din")     String din,
            @Param("cin")     String cin);

    /**
     * Used by BasicScreeningStrategy.
     *
     * Returns all active watchlist entries whose effective window covers the
     * given date. No alias JOIN — basic strategy never checks aliases.
     */
    @Query("SELECT e FROM WatchlistEntry e " +
            "WHERE e.isActive = true " +
            "AND (e.effectiveFrom IS NULL OR e.effectiveFrom <= :screeningDate) " +
            "AND (e.effectiveTo   IS NULL OR e.effectiveTo   >= :screeningDate)")
    List<WatchlistEntry> findAllActiveOnDate(@Param("date") LocalDate date);

    /**
     * Used by AdvancedScreeningStrategy.
     *
     * Same active+effective filter as above, but JOIN FETCHes aliases in one
     * query so the strategy can iterate entry.getAliases() without triggering
     * N+1 lazy loads.
     *
     * DISTINCT is required because a LEFT JOIN FETCH on a collection produces
     * duplicate parent rows in JPQL — one row per alias per entry.
     */
    @Query("SELECT DISTINCT e FROM WatchlistEntry e " +
            "LEFT JOIN FETCH e.aliases " +
            "WHERE e.isActive = true " +
            "AND (e.effectiveFrom IS NULL OR e.effectiveFrom <= :screeningDate) " +
            "AND (e.effectiveTo   IS NULL OR e.effectiveTo   >= :screeningDate)")
    List<WatchlistEntry> findAllActiveOnDateWithAliases(@Param("date") LocalDate date);

    // This was the original Tier-1 method.  It is still wired in
    // WatchlistServiceImpl.findExactIdMatch() — that service method itself now
    // delegates to findActiveExactIdMatch() above once the service is updated.
    // Left here to avoid a compile error while the migration is in progress.
    List<WatchlistEntry> findByPanNumberOrAadhaarNumberOrDinNumberOrCinNumber(
            String pan, String aadhaar, String din, String cin);
}
