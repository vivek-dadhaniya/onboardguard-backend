package com.onboardguard.officer.dto;

import com.onboardguard.candidate.enums.OnboardingStatus;
import java.time.Instant;

public record CandidateQueueItemDto(
        Long candidateId,
        String firstName,
        String lastName,
        String email,
        OnboardingStatus status,
        Instant formSubmittedAt
) {}