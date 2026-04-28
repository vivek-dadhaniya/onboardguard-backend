package com.onboardguard.officer.service.impl;


import com.onboardguard.officer.dto.AlertDetailDto;
import com.onboardguard.officer.entity.Alert;
import com.onboardguard.officer.entity.Case;
import com.onboardguard.officer.entity.CaseNote;
import com.onboardguard.officer.mapper.AlertMapper;
import com.onboardguard.officer.repository.AlertRepository;
import com.onboardguard.officer.repository.CaseRepository;
import com.onboardguard.officer.service.AlertService;
import com.onboardguard.shared.common.enums.AlertStatus;
import com.onboardguard.shared.common.enums.CaseStatus;
import com.onboardguard.shared.common.enums.NoteType;
import com.onboardguard.shared.common.exception.BadRequestException;
import com.onboardguard.shared.common.exception.ResourceNotFoundException;
import com.onboardguard.shared.common.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final CaseRepository caseRepository;
    private final AlertMapper alertMapper;


    @Override
    @Transactional
    public AlertDetailDto acknowledgeAlert(Long alertId, Long officerId){
        Alert alert = getAlertById(alertId);

        if(alert.getStatus() != AlertStatus.OPEN){
            throw new BadRequestException("Only OPEN alerts can be acknowledged.");
        }

        alert.setStatus(AlertStatus.IN_REVIEW);  // for locking the alert when it is reviewing by an officer , so that other officer can’t be able to see that  same alert
        alert.setAcknowledgedBy(officerId);
        alert.setAcknowledgedAt(Instant.now());

        log.info("Alert ID {} claimed and locked by L1 Officer ID {}", alertId, officerId);
        return alertMapper.toDto(alertRepository.save(alert));
    }

    /**
     * GET NEXT ALERT: Fetches the most urgent OPEN alert and instantly locks it for the officer.
     */
    @Override
    @Transactional
    public AlertDetailDto claimNextAvailableAlert(Long officerId) {

        // 1. Fetch the oldest open alert safely with a Pessimistic DB Lock
        Alert oldestOpenAlert = alertRepository.findFirstByStatusOrderBySlaDeadlineAsc(AlertStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("The queue is completely empty. Great job!"));

        // 2. Lock and Claim the alert for this specific officer
        oldestOpenAlert.setStatus(AlertStatus.IN_REVIEW);
        oldestOpenAlert.setAcknowledgedBy(officerId);
        oldestOpenAlert.setAcknowledgedAt(Instant.now());

        log.info("L1 Officer ID {} used 'Get Next' and was assigned Alert ID {}", officerId, oldestOpenAlert.getId());

        // 3. Save and return the mapped DTO to the frontend
        return alertMapper.toDto(alertRepository.save(oldestOpenAlert));
    }

    @Override
    @Transactional
    public void dismissAlert(Long alertId, Long officerId, String reason) {
        Alert alert = getAlertById(alertId);

        validateAlertOwnership(alert, officerId);

        alert.setStatus(AlertStatus.CLOSED);
        alertRepository.save(alert);

        log.info("Alert ID {} CLOSED as false positive by L1 Officer ID {}. Reason: {}", alertId, officerId, reason);

        // Note: Publish an event here if you want the Screening Engine to know the alert was dismissed
    }

    @Override
    @Transactional
    public Long convertToCase(Long alertId, Long officerId){
       Alert alert = getAlertById(alertId);

       validateAlertOwnership(alert, officerId);

       alert.setStatus(AlertStatus.CONVERTED_TO_CASE);
       alertRepository.save(alert);

        // 2. Start the Case Clock (e.g., 5-day SLA)
        Case investigationCase = Case.builder()
                .alertId(alert.getId())
                .candidateId(alert.getCandidateId())
                .assignedOfficerId(officerId) // The L1 who converted it automatically owns the new Case
                .assignedBy(officerId)
                .assignedAt(Instant.now())
                .status(CaseStatus.IN_REVIEW) // Bypasses OPEN because the officer is already actively working on it
                .slaDueDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .isSlaBreached(false)
                .build();

        // 3. Auto-generate the immutable system note
        CaseNote systemNote = CaseNote.builder()
                .investigationCase(investigationCase)
                .authorId(officerId)
                .content("Case automatically generated from Alert ID " + alertId + " by L1 Officer.")
                .noteType(NoteType.SYSTEM_ACTION)
                .build();

        investigationCase.getNotes().add(systemNote);

        Case savedCase = caseRepository.save(investigationCase);
        log.info("Alert ID {} converted to Case ID {} by L1 Officer ID {}", alertId, savedCase.getId(), officerId);

        return savedCase.getId();

    }

    private Alert getAlertById(Long alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with ID: " + alertId));
    }

    /**
     * Guarantees that only the officer who claimed the alert can close or convert it.
     */
    private void validateAlertOwnership(Alert alert, Long officerId) {
        if (alert.getStatus() != AlertStatus.IN_REVIEW) {
            throw new IllegalStateException("Alert must be IN_REVIEW before it can be processed. Please claim it first.");
        }

        if (!officerId.equals(alert.getAcknowledgedBy())) {
            log.warn("Security Alert: Officer ID {} attempted to modify Alert ID {} owned by Officer ID {}",
                    officerId, alert.getId(), alert.getAcknowledgedBy());
            throw new UnauthorizedAccessException("You cannot process this alert because it is locked by another officer.");
        }
    }
}
