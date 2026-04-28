package com.onboardguard.officer.dto;

import com.onboardguard.shared.common.enums.CaseOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveCaseDto(

        @NotNull(message = "A final outcome (CLEARED or REJECTED) is mandatory.")
        CaseOutcome outcome,

        @NotBlank(message = "A resolution reason must be provided for audit purposes.")
        @Size(min = 10, message = "Resolution reason must be at least 10 characters long to ensure adequate explanation.")
        String outcomeReason

) {}