package com.onboardguard.screening.repository;

import com.onboardguard.screening.entity.ScreeningResult;
import com.onboardguard.screening.enums.ScreeningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {

    // Most recent screening for a candidate (for dashboard display)
    Optional<ScreeningResult> findTopByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    // All screenings for a candidate (history tab)
    List<ScreeningResult> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    // Used by analytics dashboard — count by status
    long countByStatus(ScreeningStatus status);

    // Used by analytics — flagged vs cleared breakdown
    @Query("SELECT sr.riskLevel, COUNT(sr) FROM ScreeningResult sr GROUP BY sr.riskLevel")
    List<Object[]> countGroupedByRiskLevel();

    // Used for re-screening check — is there an IN_PROGRESS run?
    boolean existsByCandidateIdAndStatus(Long candidateId, ScreeningStatus status);
}