package com.onboardguard.candidate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PersonalDetailsRequestDto(
        @NotBlank String firstName,
        String middleName,
        @NotBlank String lastName,
        @NotNull LocalDate dateOfBirth,
        @NotBlank String gender,
        @NotBlank String nationality,
        String panNumber,
        String adhaarNumber,
        String passportNumber,
        @NotBlank String addressLine1,
        @NotBlank String addressCity,
        @NotBlank String addressState,
        @NotBlank String addressPincode,
        String addressCountry
) {}