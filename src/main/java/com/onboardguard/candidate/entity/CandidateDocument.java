package com.onboardguard.candidate.entity;

import com.onboardguard.candidate.enums.CandidateDocumentType;
import com.onboardguard.candidate.enums.DocumentStatus;
import com.onboardguard.shared.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;

@Entity
@Table(name = "candidate_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CandidateDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateDocumentType candidateDocumentType;

    @Column(nullable = false)
    private String cloudStorageKey;
    private String originalFilename;
    private String mimeType;
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    private String rejectionReason;
    private Long verifiedBy;
    private Instant verifiedAt;
    private Instant uploadedAt;
}