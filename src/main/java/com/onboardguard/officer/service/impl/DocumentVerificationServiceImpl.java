package com.onboardguard.officer.service.impl;

import com.onboardguard.candidate.dto.response.DocumentResponseDto;
import com.onboardguard.candidate.entity.Candidate;
import com.onboardguard.candidate.entity.CandidateDocument;
import com.onboardguard.candidate.enums.DocumentStatus;
import com.onboardguard.candidate.enums.OnboardingStatus;
import com.onboardguard.candidate.repository.CandidateDocumentRepository;
import com.onboardguard.candidate.repository.CandidateRepository;
import com.onboardguard.candidate.service.impl.CandidateDocumentServiceImpl;
import com.onboardguard.officer.dto.CandidateQueueItemDto;
import com.onboardguard.officer.dto.CandidateVerificationDashboardDto;
import com.onboardguard.officer.mapper.OfficerCandidateMapper;
import com.onboardguard.officer.service.DocumentVerificationService;
import com.onboardguard.screening.service.ScreeningOrchestrationService;
import com.onboardguard.shared.common.events.DocumentRejectedEvent;
import com.onboardguard.shared.common.events.DocumentVerificationCompletedEvent;
import com.onboardguard.shared.common.exception.BadRequestException;
import com.onboardguard.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVerificationServiceImpl implements DocumentVerificationService {

    private final CandidateDocumentRepository documentRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateDocumentServiceImpl candidateDocumentService;
    private final OfficerCandidateMapper officerCandidateMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ScreeningOrchestrationService screeningOrchestrationService;

    /**
     * Officer pulls all documents for a specific candidate to review them side-by-side.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getCandidateDocumentsForReview(Long candidateId) {
        return documentRepository.findByCandidateId(candidateId)
                .stream()
                .map(candidateDocumentService::mapToResponseWithUrl)
                .toList();
    }

    // ══════════════════════════════════════════════════════════════
    // QUEUE VIEW (GRID)
    // ══════════════════════════════════════════════════════════════

    /**
     * GET QUEUE: Returns a lightweight list of unlocked candidates for the Officer UI grid.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CandidateQueueItemDto> getPendingCandidatesQueue() {

        // Fetch unlocked candidates who are waiting for document verification
        List<Candidate> pendingCandidates = candidateRepository
                .findAvailableCandidatesForVerification(OnboardingStatus.DOCUMENTS_UPLOADED);

        // Map to lightweight DTOs for the frontend
        return pendingCandidates.stream()
                .map(officerCandidateMapper::toQueueItemDto)
                .toList();
    }

    // 1. QUEUE & CLAIM LOGIC
    /**
     * MANUAL PULL: Officer clicks a specific candidate in the grid to lock and claim them.
     */
    @Override
    @Transactional
    public void claimCandidateForVerification(Long candidateId, Long officerId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (candidate.getVerificationLockedBy() != null && !candidate.getVerificationLockedBy().equals(officerId)) {
            throw new IllegalStateException("This candidate is already being reviewed by another officer.");
        }

        lockCandidate(candidate, officerId);
    }

    /**
     * AUTO PUSH (FIFO): Automatically finds the oldest unlocked candidate, locks it, and returns the dashboard.
     */
    @Override
    @Transactional
    public CandidateVerificationDashboardDto claimNextAvailableCandidate(Long officerId) {
        Candidate nextCandidate = candidateRepository
                .findFirstByOnboardingStatusAndVerificationLockedByIsNullOrderByFormSubmittedAtAsc(OnboardingStatus.DOCUMENTS_UPLOADED)
                .orElseThrow(() -> new ResourceNotFoundException("No candidates currently waiting for verification!"));

        lockCandidate(nextCandidate, officerId);

        return getCandidateVerificationDetails(nextCandidate.getId());
    }

    // 2. DASHBOARD VIEW (MAPSTRUCT)
    /**
     * Fetches the candidate profile AND documents into a single JSON payload.
     */
    @Override
    @Transactional(readOnly = true)
    public CandidateVerificationDashboardDto getCandidateVerificationDetails(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with ID: " + candidateId));

        List<DocumentResponseDto> documents = documentRepository.findByCandidateId(candidateId)
                .stream()
                .map(candidateDocumentService::mapToResponseWithUrl)
                .toList();

        return officerCandidateMapper.toDashboardDto(candidate, documents);
    }

    // 3. DOCUMENT VERIFICATION LOGIC
    @Override
    @Transactional
    public void approveDocument(Long documentId, Long officerId) {
        CandidateDocument document = getDocumentById(documentId);
        validateLockOwnership(document.getCandidate(), officerId);

        if (document.getStatus() == DocumentStatus.VERIFIED) {
            throw new BadRequestException("Document is already verified.");
        }

        document.setStatus(DocumentStatus.VERIFIED);
        document.setVerifiedBy(officerId);
        document.setVerifiedAt(Instant.now());
        document.setRejectionReason(null);

        documentRepository.save(document);
        log.info("Document ID {} VERIFIED by Officer ID {}", documentId, officerId);

        checkAndAdvanceCandidateStatus(document.getCandidate().getId());
    }

    @Override
    @Transactional
    public void rejectDocument(Long documentId, String reason, Long officerId) {
        CandidateDocument document = getDocumentById(documentId);
        validateLockOwnership(document.getCandidate(), officerId);

        if (document.getStatus() == DocumentStatus.VERIFIED) {
            throw new BadRequestException("Cannot reject a document that has already been verified.");
        }

        document.setStatus(DocumentStatus.REJECTED);
        document.setRejectionReason(reason);
        document.setVerifiedBy(officerId);
        document.setVerifiedAt(Instant.now());
        documentRepository.save(document);

        Candidate candidate = document.getCandidate();

        // Lock candidate status and unlock the profile (so it's not stuck with the officer)
        candidate.setOnboardingStatus(OnboardingStatus.DOCUMENTS_REJECTED);
        candidate.setVerificationLockedBy(null);
        candidate.setVerificationLockedAt(null);
        candidateRepository.save(candidate);

        log.info("Document ID {} REJECTED by Officer ID {}. Reason: {}", documentId, officerId, reason);

        // Fire event to email the candidate
        DocumentRejectedEvent event = new DocumentRejectedEvent(
                candidate.getUser().getEmail(),
                candidate.getPersonalDetail().getFirstName() + " " + candidate.getPersonalDetail().getLastName(),
                document.getCandidateDocumentType().name(),
                reason
        );
        eventPublisher.publishEvent(event);
    }

    // PRIVATE HELPER METHODS
    private void lockCandidate(Candidate candidate, Long officerId) {
        candidate.setVerificationLockedBy(officerId);
        candidate.setVerificationLockedAt(Instant.now());
        candidateRepository.save(candidate);

        log.info("Candidate ID {} locked by Officer ID {}", candidate.getId(), officerId);
    }

    private void validateLockOwnership(Candidate candidate, Long officerId) {
        if (!officerId.equals(candidate.getVerificationLockedBy())) {
            throw new IllegalStateException("You cannot modify documents for a candidate you have not locked/claimed.");
        }
    }

    private CandidateDocument getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
    }

    private void checkAndAdvanceCandidateStatus(Long candidateId) {
        List<CandidateDocument> allDocs = documentRepository.findByCandidateId(candidateId);

        boolean allVerified = allDocs.stream().allMatch(doc -> doc.getStatus() == DocumentStatus.VERIFIED);

        if (allVerified) {
            Candidate candidate = candidateRepository.findById(candidateId).orElseThrow();
            candidate.setOnboardingStatus(OnboardingStatus.SCREENING_PENDING);

            // Release the lock, the officer is done!
            candidate.setVerificationLockedBy(null);
            candidate.setVerificationLockedAt(null);
            candidateRepository.save(candidate);

            log.info("Candidate ID {} has all documents verified. Ready for Screening Engine.", candidateId);

            eventPublisher.publishEvent(new DocumentVerificationCompletedEvent(candidateId));

            screeningOrchestrationService.runScreening(candidateId);
        }
    }
}