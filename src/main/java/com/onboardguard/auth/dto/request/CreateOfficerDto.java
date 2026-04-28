package com.onboardguard.auth.dto.request;

import com.onboardguard.shared.common.enums.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOfficerDto(

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email
        String email,

        String phone,

        @NotNull(message = "Officer role is required")
        RoleCode role // Needed so the Admin can select L1 or L2
) {}