package com.onboardguard.officer.service.impl;

import com.onboardguard.officer.dto.CaseDetailDto;
import com.onboardguard.officer.dto.EscalateCaseDto;
import com.onboardguard.officer.dto.ResolveCaseDto;
import com.onboardguard.officer.entity.Case;
import com.onboardguard.officer.entity.CaseNote;
import com.onboardguard.officer.mapper.CaseMapper;
import com.onboardguard.officer.repository.CaseRepository;
import com.onboardguard.officer.service.CaseService;
import com.onboardguard.shared.common.enums.CaseStatus;
import com.onboardguard.shared.common.enums.NoteType;
import com.onboardguard.shared.common.exception.ResourceNotFoundException;
import com.onboardguard.shared.common.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private final CaseRepository caseRepository;
    private final CaseMapper caseMapper;
    private final ApplicationEventPublisher eventPublisher; // For notifying the Candidate module later

    @Override
    @Transactional(readOnly = true)
    public CaseDetailDto getCaseDetails(Long caseId) {
        Case investigationCase = getCaseById(caseId);
        return caseMapper.toDto(investigationCase);
    }

    /**
     * L1 ACTION: Escalate the case to the L2 Checker queue.
     */
    @Override
    @Transactional
    public void escalateCase(Long caseId, EscalateCaseDto dto, Long l1OfficerId) {
        Case investigationCase = getCaseById(caseId);

        // Rule: Only the L1 Officer who currently owns the IN_REVIEW case can escalate it
        if (investigationCase.getStatus() != CaseStatus.IN_REVIEW) {
            throw new IllegalStateException("Only IN_REVIEW cases can be escalated.");
        }
        validateCaseOwnership(investigationCase, l1OfficerId);

        // State Transition
        investigationCase.setStatus(CaseStatus.ESCALATED);
        investigationCase.setEscalatedTo(dto.escalatedTo());
        investigationCase.setEscalatedAt(Instant.now());
        investigationCase.setEscalationReason(dto.escalationReason());

        // Remove ownership lock so an L2 can claim it from the queue
        investigationCase.setAssignedOfficerId(null);

        // Lock the L1 Officer's memo into the permanent audit trail
        CaseNote escalationNote = CaseNote.builder()
                .investigationCase(investigationCase)
                .authorId(l1OfficerId)
                .content("ESCALATION MEMO: " + dto.escalationReason())
                .noteType(NoteType.ESCALATION)
                .build();

        investigationCase.getNotes().add(escalationNote);
        caseRepository.save(investigationCase);

        log.info("Case ID {} escalated to L2 queue by L1 Officer ID {}", caseId, l1OfficerId);
    }

    /**
     * L2 ACTION: Claim an escalated case from the queue to start reviewing.
     */
    @Override
    @Transactional
    public void claimEscalatedCase(Long caseId, Long l2OfficerId) {
        Case investigationCase = getCaseById(caseId);

        if (investigationCase.getStatus() != CaseStatus.ESCALATED) {
            throw new IllegalStateException("Only ESCALATED cases can be claimed by an L2 Checker.");
        }

        // Lock the case to this specific L2 Officer
        investigationCase.setAssignedOfficerId(l2OfficerId);
        caseRepository.save(investigationCase);

        log.info("ESCALATED Case ID {} claimed by L2 Officer ID {}", caseId, l2OfficerId);
    }

    /**
     * L2 ACTION: Make the final decision (CLEARED or REJECTED).
     */
    @Override
    @Transactional
    public void resolveCase(Long caseId, ResolveCaseDto dto, Long l2OfficerId) {
        Case investigationCase = getCaseById(caseId);

        // Ensure the case is ESCALATED and owned by this exact L2 Officer
        if (investigationCase.getStatus() != CaseStatus.ESCALATED) {
            throw new IllegalStateException("Case must be in ESCALATED state to be resolved.");
        }
        validateCaseOwnership(investigationCase, l2OfficerId);

        // Final State Transition
        investigationCase.setStatus(CaseStatus.RESOLVED);
        investigationCase.setOutcome(dto.outcome());
        investigationCase.setOutcomeReason(dto.outcomeReason());
        investigationCase.setResolvedBy(l2OfficerId);
        investigationCase.setResolvedAt(Instant.now());

        // Final system audit note
        CaseNote resolutionNote = CaseNote.builder()
                .investigationCase(investigationCase)
                .authorId(l2OfficerId)
                .content("FINAL DECISION (" + dto.outcome().name() + "): " + dto.outcomeReason())
                .noteType(NoteType.SYSTEM_ACTION)
                .build();

        investigationCase.getNotes().add(resolutionNote);
        caseRepository.save(investigationCase);

        log.info("Case ID {} RESOLVED with outcome {} by L2 Officer ID {}", caseId, dto.outcome(), l2OfficerId);

        // FUTURE: eventPublisher.publishEvent(new CaseResolvedEvent(investigationCase.getCandidateId(), dto.outcome()));
    }

    // ══════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ══════════════════════════════════════════════════════════════

    private Case getCaseById(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));
    }

    private void validateCaseOwnership(Case investigationCase, Long officerId) {
        if (!officerId.equals(investigationCase.getAssignedOfficerId())) {
            log.warn("Security Alert: Officer ID {} attempted to act on Case ID {} locked by Officer ID {}",
                    officerId, investigationCase.getId(), investigationCase.getAssignedOfficerId());
            throw new UnauthorizedAccessException("You cannot perform this action because the case is locked by another officer.");
        }
    }
}