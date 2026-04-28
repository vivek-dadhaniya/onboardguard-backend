package com.onboardguard.officer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDocumentRequestDto(
        @NotBlank(message = "A rejection reason must be provided.")
        @Size(min = 10, message = "Please provide a clear reason so the candidate knows how to fix it (e.g., 'Passport image is too blurry to read').")
        String reason
) {}
