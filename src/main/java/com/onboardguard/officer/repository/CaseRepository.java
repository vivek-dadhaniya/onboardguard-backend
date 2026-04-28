package com.onboardguard.officer.repository;

import com.onboardguard.officer.entity.Case;
import com.onboardguard.shared.common.enums.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {

    /**
     * Bulk updates all unresolved cases where the SLA due date has passed.
     */
    @Modifying
    @Query("UPDATE Case c SET c.isSlaBreached = true WHERE c.isSlaBreached = false AND c.status != :resolvedStatus AND c.slaDueDate < :now")
    int markBreachedCases(@Param("resolvedStatus") CaseStatus resolvedStatus, @Param("now") Instant now);

}
