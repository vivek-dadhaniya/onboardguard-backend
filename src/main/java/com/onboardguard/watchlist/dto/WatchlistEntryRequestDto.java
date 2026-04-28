package com.onboardguard.watchlist.dto;

import com.onboardguard.shared.common.enums.CategoryCode;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

@Data
public class WatchlistEntryRequestDto {

    @NotBlank(message = "Category code is strictly required")
    private CategoryCode categoryCode; // e.g., CRIMINAL_RECORDS

    @NotBlank(message = "Primary name cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String primaryName;

    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$", message = "Severity must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String severity;

    @NotBlank(message = "Source name (e.g., Interpol, CBI) is required")
    private String sourceName;

    @DecimalMin(value = "0.0", message = "Credibility weight cannot be negative")
    @DecimalMax(value = "1.0", message = "Credibility weight max is 1.0")
    private Double sourceCredibilityWeight;

    // --- REGEX VALIDATIONS FOR INDIAN COMPLIANCE ---

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN Card format")
    private String panNumber;

    @Pattern(regexp = "^\\d{12}$", message = "Invalid Aadhaar format (must be 12 digits)")
    private String aadhaarNumber;

    @Pattern(regexp = "^[0-9]{8}$", message = "DIN must be an 8-digit numeric value")
    private String dinNumber;

    @Pattern(regexp = "^[A-Z0-9]{21}$", message = "CIN must be 21 alphanumeric characters")
    private String cinNumber;

    // --- ADDITIONAL INFO ---

    private String organizationName;
    private String designation;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String nationality;

    // JSONB payload for category-specific data (no strict validation here, validated in Service)
    private Map<String, Object> categorySpecificData;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    private String notes;
}