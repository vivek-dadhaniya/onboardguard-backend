package com.onboardguard.candidate.dto.response;

import com.onboardguard.candidate.enums.CandidateType;
import com.onboardguard.candidate.enums.OnboardingStatus;

public record CandidateStatusResponseDto(
        CandidateType candidateType,
        OnboardingStatus onboardingStatus,
        boolean isSubmitted
) {}