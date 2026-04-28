package com.onboardguard.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardguard.admin.dto.SystemConfigResponseDto;
import com.onboardguard.admin.dto.UpdateSystemConfigDto;
import com.onboardguard.admin.entity.ApprovalRequest;
import com.onboardguard.admin.mapper.SystemConfigMapper;
import com.onboardguard.admin.repository.ApprovalRequestRepository;
import com.onboardguard.admin.service.SystemConfigAdminService;
import com.onboardguard.shared.common.enums.ActionType;
import com.onboardguard.shared.common.enums.RequestStatus;
import com.onboardguard.shared.common.enums.RoleCode;
import com.onboardguard.shared.common.events.BusinessLogEvent;
import com.onboardguard.shared.config.entity.SystemConfig;
import com.onboardguard.shared.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigAdminServiceImpl implements SystemConfigAdminService {

    private final SystemConfigRepository systemConfigRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemConfigMapper systemConfigMapper;

    /**
     * MAKER ACTION: An Admin requests to update a system configuration.
     * This does NOT update the config; it creates a PENDING approval request.
     */
    @Override
    @Transactional
    public void requestConfigUpdate(Long configId, UpdateSystemConfigDto updateDto,
                                    Long currentUserId, RoleCode currentUserRole) {

        log.info("Admin ID {} is requesting an update to System Config ID: {}", currentUserId, configId);

        // 1. Validate the target configuration actually exists
        SystemConfig config = systemConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("System configuration not found with ID: " + configId));

        // 2. Prevent duplicate pending requests
        // If there's already a pending request for this specific config, block a new one to prevent conflicts.
        boolean hasPendingRequest = approvalRequestRepository.existsByTargetEntityTypeAndTargetEntityIdAndStatus(
                "SYSTEM_CONFIG", configId, RequestStatus.PENDING);

        if (hasPendingRequest) {
            throw new IllegalStateException("A pending approval request already exists for this configuration. Please wait for Super Admin review.");
        }

        try {
            // 3. Serialize the DTO into a JSON Payload
            String jsonPayload = objectMapper.writeValueAsString(updateDto);

            // 4. Create the Maker-Checker Request
            ApprovalRequest request = ApprovalRequest.builder()
                    .actionType(ActionType.UPDATE)
                    .targetEntityType("SYSTEM_CONFIG")
                    .targetEntityId(configId)
                    .payload(jsonPayload)
                    .requestedBy(currentUserId)
                    .status(RequestStatus.PENDING)
                    .build();

            approvalRequestRepository.save(request);

            // 5. Fire an Audit Event to track that a request was made
            publishMakerAudit(configId, currentUserId, currentUserRole.name(),
                    "Requested change to value: " + (config.getIsSensitive() ? "********" : updateDto.configValue()));

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UpdateSystemConfigDto for config ID: {}", configId, e);
            throw new RuntimeException("System error: Could not process the configuration update payload.");
        }
    }

    /**
     * GET ALL CONFIGS: Fetches the system configurations for the Admin UI grid.
     * Automatically masks sensitive values (like API keys or passwords).
     */
    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigResponseDto> getAllConfigs() {
        return systemConfigRepository.findAll().stream()
                .map(systemConfigMapper::toResponseDto)
                .toList();
    }

    // ══════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ══════════════════════════════════════════════════════════════

    private void publishMakerAudit(Long configId, Long performedBy, String actorRole, String remarks) {
        eventPublisher.publishEvent(BusinessLogEvent.builder()
                .entityType("SYSTEM_CONFIG")
                .entityId(configId)
                .action("UPDATE_REQUESTED")
                .oldStatus("ACTIVE") // Standard fallback for configs
                .newStatus("PENDING_APPROVAL")
                .performedBy(performedBy)
                .actorRole(actorRole)
                .remarks(remarks)
                .build());
    }
}