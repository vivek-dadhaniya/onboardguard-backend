package com.onboardguard.officer.dto;

import com.onboardguard.shared.common.enums.NoteType;

import java.time.Instant;

public record CaseNoteDto(
        Long id,
        Long authorId,
        String content,
        NoteType noteType,
        Instant createdAt
) {}