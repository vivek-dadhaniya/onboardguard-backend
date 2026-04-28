package com.onboardguard.admin.dto;

import com.onboardguard.shared.common.enums.ActionType;
import com.onboardguard.shared.common.enums.RequestStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record PendingApprovalDto(
        Long id,

        // What is changing?
        ActionType actionType,       // e.g., CREATE, UPDATE, DELETE
        String targetEntityType,     // e.g., "SYSTEM_CONFIG", "WATCHLIST_ENTRY"
        Long targetEntityId,         // Nullable if it's a brand new creation

        // The proposed changes (Sent as a Map so the frontend can easily iterate and show "Old Value -> New Value")
        Map<String, Object> payload,

        // Maker Info
        Long requestedById,
        String requestedByName,      // Human-readable name for the UI
        Instant requestedAt,

        // Checker Info
        Long reviewedById,
        String reviewedByName,
        Instant reviewedAt,

        // Status & Overrides
        RequestStatus status,
        String rejectionReason,
        Boolean isBypass             // Flags if Super Admin used emergency override
) {}