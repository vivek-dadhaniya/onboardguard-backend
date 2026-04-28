package com.onboardguard.officer.dto;

import com.onboardguard.shared.common.enums.CaseOutcome;
import com.onboardguard.shared.common.enums.CaseStatus;

import java.time.Instant;
import java.util.List;

public record CaseDetailDto(
        Long id,
        Long alertId,
        Long candidateId,
        Long assignedOfficerId,
        Long assignedBy,
        Instant assignedAt,
        Instant slaDueDate,
        Boolean isSlaBreached,
        CaseStatus status,
        CaseOutcome outcome,
        String outcomeReason,
        Long resolvedBy,
        Instant resolvedAt,
        Long escalatedTo,
        Instant escalatedAt,
        String escalationReason,
        List<CaseNoteDto> notes, // Nested immutable list of the notes!
        Instant createdAt,
        Instant updatedAt
) {}