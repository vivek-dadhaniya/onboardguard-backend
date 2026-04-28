package com.onboardguard.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CandidateMatchRequestDto {

    // 1. Core Identifiers
    @NotBlank(message = "Candidate primary name is required for matching")
    private String candidateName;

    // 2. Government IDs
    private String panNumber;
    private String aadhaarNumber;
    private String passportNumber;
    private String dinNumber;
    private String cinNumber;

    // 3. Contextual Data (For False Positive reduction)
    private LocalDate dateOfBirth;
    private String nationality;
    private String countryOfResidence;
    private String city;

    // 4. Search Controls
    private String filterCategoryCode;
    private BigDecimal minimumMatchScore; // e.g., 0.80 for 80% similarity
}