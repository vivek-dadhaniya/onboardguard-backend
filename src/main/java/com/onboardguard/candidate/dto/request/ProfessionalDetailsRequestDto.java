package com.onboardguard.candidate.dto.request;

import java.math.BigDecimal;

public record ProfessionalDetailsRequestDto(
        String currentOrganization,
        String cinNumber,
        String dinNumber,
        String currentDesignation,
        BigDecimal totalExperienceYears,
        String previousOrganization,
        String previousDesignation,
        String vendorCompanyName,
        String vendorGstNumber,
        String highestQualification,
        String universityName,
        Integer graduationYear
) {}