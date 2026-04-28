package com.onboardguard.auth.dto.response;

public record CandidateLoginResponseDto(
        String token,
        String email,
        String fullName,
        long expiresInSeconds
) {}