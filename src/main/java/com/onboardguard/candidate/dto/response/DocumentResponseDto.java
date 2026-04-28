package com.onboardguard.candidate.dto.response;

import com.onboardguard.candidate.enums.DocumentStatus;
import com.onboardguard.candidate.enums.CandidateDocumentType;
import java.time.Instant;

public record DocumentResponseDto(
        Long id,
        CandidateDocumentType candidateDocumentType,
        String originalFilename,
        Long fileSizeBytes,
        DocumentStatus status,
        String rejectionReason,
        Instant uploadedAt,
        String fileUrl // NEW: The 15-minute presigned URL for Angular to display
) {}