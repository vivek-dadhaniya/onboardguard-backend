package com.onboardguard.watchlist.dto;

import com.onboardguard.shared.common.enums.CategoryCode;
import com.onboardguard.shared.common.enums.SeverityLevel;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class WatchlistEntryResponseDto {

    private Long id;

    // Flattened Category Info
    private CategoryCode categoryCode;
    private String categoryName;

    private String primaryName;
    private SeverityLevel severity;
    private String sourceName;
    private Double sourceCredibilityWeight;

    // Government IDs
    private String panNumber;
    private String aadhaarNumber;
    private String dinNumber;
    private String cinNumber;

    // JSONB Data
    private Map<String, Object> categorySpecificData;

    private String organizationName;
    private String designation;
    private LocalDate dateOfBirth;
    private String nationality;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // Status & Audit
    private boolean isActive;
    private String notes;
    private String approvedBy;
    private Instant approvedAt;
    private String createdBy;
    private Instant createdAt;

    // Nested Collections for the Frontend UI
    private List<AliasDto> aliases;
    private List<EvidenceDto> evidenceDocuments;

    // --- NESTED CLASSES ---

    @Data
    public static class AliasDto {
        private Long id;
        private String aliasName;
        private String aliasType; // AKA, FKA
    }

    @Data
    public static class EvidenceDto {
        private Long id;
        private String documentTitle;
        private String documentType; // PDF, JPG
        private String cloudStorageKey; // S3 link for downloading
        private LocalDateTime uploadedAt;
    }
}