package com.onboardguard.candidate.repository;

import com.onboardguard.candidate.entity.Candidate;
import com.onboardguard.candidate.enums.OnboardingStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Optional<Candidate> findByUserId(Long userId);

    Long countByOnboardingStatus(OnboardingStatus onboardingStatus);

    /**
     * GET NEXT CANDIDATE (FIFO Queue with SKIP LOCKED):
     * Finds the oldest candidate waiting for document verification that is not currently locked.
     * This ensures multiple officers can pull from the queue simultaneously without database blocking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    Optional<Candidate> findFirstByOnboardingStatusAndVerificationLockedByIsNullOrderByFormSubmittedAtAsc(OnboardingStatus status);

    /**
     * Fetches candidates waiting for verification who are NOT currently locked by anyone.
     * Ordered by oldest first so SLAs are met.
     */
    @Query("SELECT c FROM Candidate c WHERE c.onboardingStatus = :status AND c.verificationLockedBy IS NULL ORDER BY c.formSubmittedAt ASC")
    List<Candidate> findAvailableCandidatesForVerification(@Param("status") OnboardingStatus status);
}