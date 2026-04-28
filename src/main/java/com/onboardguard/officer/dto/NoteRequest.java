package com.onboardguard.officer.dto;

import jakarta.validation.constraints.NotBlank;

public record NoteRequest(
        @NotBlank(message = "Note content cannot be empty.")
        String content
) {}
