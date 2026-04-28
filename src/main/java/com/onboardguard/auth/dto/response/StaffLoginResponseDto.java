package com.onboardguard.auth.dto.response;

public record StaffLoginResponseDto(
        String token,
        String email,
        String fullName,
        String roleCode,
        long expiresInSeconds
) {}