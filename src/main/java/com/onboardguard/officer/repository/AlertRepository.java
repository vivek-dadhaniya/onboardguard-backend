package com.onboardguard.officer.repository;

import com.onboardguard.officer.entity.Alert;
import com.onboardguard.shared.common.enums.AlertStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * PULL QUEUE: Finds the oldest OPEN alert based on SLA deadline.
     * * @Lock(PESSIMISTIC_WRITE) issues a SELECT ... FOR UPDATE.
     * @QueryHints(timeout = "-2") instructs Hibernate to append SKIP LOCKED.
     * * This guarantees high-throughput concurrency: If Officer A locks the first row,
     * Officer B's query will instantly bypass it and grab the second row without blocking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    Optional<Alert> findFirstByStatusOrderBySlaDeadlineAsc(AlertStatus status);

    /**
     * Bulk updates all active alerts where the SLA deadline has passed.
     * Returns the integer count of exactly how many rows were updated.
     */
    @Modifying
    @Query("UPDATE Alert a SET a.isSlaBreached = true WHERE a.isSlaBreached = false AND a.status IN :activeStatuses AND a.slaDeadline < :now")
    int markBreachedAlerts(@Param("activeStatuses") List<AlertStatus> activeStatuses, @Param("now") Instant now);
}