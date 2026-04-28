package com.onboardguard.officer.dto;

import com.onboardguard.candidate.dto.response.DocumentResponseDto;
import com.onboardguard.candidate.enums.OnboardingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CandidateVerificationDashboardDto(
        Long candidateId,
        String email,
        OnboardingStatus status,
        Instant formSubmittedAt,

        PersonalInfoDto personalInfo,
        AddressInfoDto addressInfo,
        ProfessionalInfoDto professionalInfo,

        List<DocumentResponseDto> documents
) {
    public record PersonalInfoDto(
            String firstName,
            String lastName,
            String dateOfBirth,
            String gender,
            String panNumber,
            String adhaarNumber,
            String passportNumber
    ) {}

    public record AddressInfoDto(
            String addressLine1,
            String city,
            String state,
            String pincode,
            String country
    ) {}

    public record ProfessionalInfoDto(
            String currentOrganization,
            String currentDesignation,
            BigDecimal totalExperienceYears,
            String highestQualification
    ) {}
}