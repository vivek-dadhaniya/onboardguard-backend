package com.onboardguard.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardguard.admin.dto.PendingApprovalDto;
import com.onboardguard.admin.dto.ReviewApprovalRequestDto;
import com.onboardguard.admin.dto.UpdateSystemConfigDto;
import com.onboardguard.admin.entity.ApprovalRequest;
import com.onboardguard.admin.repository.ApprovalRequestRepository;
import com.onboardguard.admin.service.MakerCheckerService;
import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.auth.repository.AppUserRepository;
import com.onboardguard.shared.common.enums.RequestStatus;
import com.onboardguard.shared.common.events.BusinessLogEvent;
import com.onboardguard.shared.common.exception.UnauthorizedAccessException;
import com.onboardguard.shared.config.entity.SystemConfig;
import com.onboardguard.shared.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MakerCheckerServiceImpl implements MakerCheckerService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 1. GET INBOX: Fetches all pending requests for the Super Admin Dashboard.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PendingApprovalDto> getPendingInbox() {
        return approvalRequestRepository.findByStatusOrderByRequestedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(this::mapToPendingDto)
                .toList();
    }

    /**
     * 2. PROCESS REVIEW: The Super Admin makes their decision.
     */
    @Override
    @Transactional
    public void processReview(Long requestId, ReviewApprovalRequestDto reviewDto, String checkerEmail) {
        log.info("Processing Maker-Checker review for Request ID: {}", requestId);

        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("This request has already been processed.");
        }

        AppUser checker = userRepository.findByEmail(checkerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Checker not found"));

        // Rule 1: Separation of Duties (A Maker cannot be their own Checker)
        // Note: Super Admins can bypass this if they use emergency override, but normally it's blocked.
        if (request.getRequestedBy().equals(checker.getId()) && !reviewDto.isBypass()) {
            throw new UnauthorizedAccessException("Separation of Duties violated: You cannot approve your own request.");
        }

        // Rule 2: Enforce Rejection Reason
        if (reviewDto.status() == RequestStatus.REJECTED &&
                (reviewDto.rejectionReason() == null || reviewDto.rejectionReason().isBlank())) {
            throw new IllegalArgumentException("A rejection reason must be provided.");
        }

        // Execute business logic if approved
        if (reviewDto.status() == RequestStatus.APPROVED) {
            applyApprovedPayload(request);
        }

        // Update the Approval Request Ledger
        request.setStatus(reviewDto.status());
        request.setRejectionReason(reviewDto.rejectionReason());
        request.setReviewedBy(checker.getId());
        request.setReviewedAt(Instant.now());

        approvalRequestRepository.save(request);

        // Fire Audit Log for the Target Entity (The Diary)
        publishReviewAudit(request, checker, reviewDto.status(), reviewDto.rejectionReason());
    }

    // ══════════════════════════════════════════════════════════════
    // PRIVATE EXECUTION METHODS
    // ══════════════════════════════════════════════════════════════

    /**
     * Translates the JSON payload into actual database changes.
     */
    private void applyApprovedPayload(ApprovalRequest request) {
        try {
            switch (request.getTargetEntityType()) {
                case "SYSTEM_CONFIG" -> applySystemConfigUpdate(request);
                case "WATCHLIST_ENTRY" -> log.info("Watchlist entry handler would go here");
                default -> throw new ResourceNotFoundException("Unknown entity type for approval: " + request.getTargetEntityType());
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Maker-Checker JSON payload", e);
            throw new RuntimeException("Failed to apply the approved changes due to payload corruption.");
        }
    }

    private void applySystemConfigUpdate(ApprovalRequest request) throws JsonProcessingException {
        SystemConfig config = systemConfigRepository.findById(request.getTargetEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Target System Config not found"));

        // Deserialize the JSON string back into the DTO the Admin originally sent
        UpdateSystemConfigDto payload = objectMapper.readValue(request.getPayload(), UpdateSystemConfigDto.class);

        // Apply changes
        config.setConfigValue(payload.configValue());
        config.setDescription(payload.description());
//        config.set(payload.isActive());
//        config.setIsSensitive(payload.isSensitive());

        systemConfigRepository.save(config);
    }

    // ══════════════════════════════════════════════════════════════
    // HELPER & MAPPING METHODS
    // ══════════════════════════════════════════════════════════════

    private PendingApprovalDto mapToPendingDto(ApprovalRequest request) {
        try {
            // Convert JSON string to a Map so Angular can iterate over "Field -> New Value" easily
            Map<String, Object> payloadMap = objectMapper.readValue(request.getPayload(), new TypeReference<>() {});

            String makerName = userRepository.findById(request.getRequestedBy())
                    .map(AppUser::getFullName).orElse("Unknown Maker");

            return new PendingApprovalDto(
                    request.getId(),
                    request.getActionType(),
                    request.getTargetEntityType(),
                    request.getTargetEntityId(),
                    payloadMap,
                    request.getRequestedBy(),
                    makerName,
                    request.getRequestedAt(),
                    null, null, null, // Checker fields are null because it's PENDING
                    request.getStatus(),
                    null, null
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse request payload for UI rendering");
        }
    }

    private void publishReviewAudit(ApprovalRequest request, AppUser checker, RequestStatus status, String remarks) {
        String action = status == RequestStatus.APPROVED ? "MAKER_CHECKER_APPROVED" : "MAKER_CHECKER_REJECTED";
        String finalRemarks = remarks != null ? remarks : "Authorized by Super Admin";

        eventPublisher.publishEvent(BusinessLogEvent.builder()
                .entityType(request.getTargetEntityType()) // Log against the SYSTEM_CONFIG, not the request
                .entityId(request.getTargetEntityId())
                .action(action)
                .oldStatus("PENDING_APPROVAL")
                .newStatus(status.name())
                .performedBy(checker.getId())
                .actorRole(checker.getRole().name())
                .remarks(finalRemarks)
                .build());
    }
}