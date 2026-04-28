package com.onboardguard.officer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalateCaseDto(

        // This can be nullable if your system automatically assigns the next available L2.
        // If the L1 is allowed to pick a specific L2 from a dropdown, add @NotNull.
        Long escalatedTo,

        @NotBlank(message = "An escalation reason is strictly required.")
        @Size(min = 10, message = "Please provide a detailed reason for escalating this to an L2 officer.")
        String escalationReason

) {}