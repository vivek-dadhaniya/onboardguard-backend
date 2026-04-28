package com.onboardguard.auth.dto.response;

import com.onboardguard.shared.common.enums.RoleCode;
import java.time.Instant;

public record OfficerResponseDto(
        Long id,
        String fullName,
        String email,
        String phone,
        RoleCode role,
        boolean active,
        boolean locked,
        Instant createdAt,
        Instant lastLoginAt
) {}