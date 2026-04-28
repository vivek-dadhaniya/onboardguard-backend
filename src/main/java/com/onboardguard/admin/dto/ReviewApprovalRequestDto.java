package com.onboardguard.admin.dto;

import com.onboardguard.shared.common.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewApprovalRequestDto(

        @NotNull(message = "Review status is required")
        RequestStatus status, // APPROVED or REJECTED

        // Enforced in service layer if status == REJECTED
        String rejectionReason,

        // Primitive boolean naturally defaults to false if missing from the JSON payload
        boolean isBypass
) {}