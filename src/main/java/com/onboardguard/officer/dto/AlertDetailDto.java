package com.onboardguard.officer.dto;

import com.onboardguard.shared.common.enums.AlertStatus;
import com.onboardguard.shared.common.enums.SeverityLevel;

import java.time.Instant;
import java.util.List;

public record AlertDetailDto(
        Long id,
        Long candidateId,
        Long screeningResultId,
        SeverityLevel severity,
        AlertStatus status,
        List<String> matchedCategories, // Perfectly mapped from your JSONB column
        Instant slaDeadline,
        Boolean isSlaBreached,
        Long acknowledgedBy,
        Instant acknowledgedAt,
        Instant createdAt
) {}