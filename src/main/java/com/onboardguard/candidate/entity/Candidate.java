package com.onboardguard.candidate.entity;

import com.onboardguard.auth.entity.AppUser;
import com.onboardguard.candidate.enums.CandidateType;
import com.onboardguard.candidate.enums.OnboardingStatus;
import com.onboardguard.screening.enums.ScreeningStatus;
import com.onboardguard.shared.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Candidate extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    private CandidateType candidateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OnboardingStatus onboardingStatus;

    @Enumerated(EnumType.STRING)
    private ScreeningStatus screeningStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String formDraft;

    private Instant formSubmittedAt;

    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CandidatePersonalDetail personalDetail;

    @OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CandidateProfessionalDetail professionalDetail;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateDocument> documents = new ArrayList<>();

    @Column(name = "verification_locked_by")
    private Long verificationLockedBy;

    @Column(name = "verification_locked_at")
    private Instant verificationLockedAt;

    // Convenience Methods for Screening Mapper
    public String getFullName() {
        if (personalDetail == null) return null;
        String first = personalDetail.getFirstName() != null ? personalDetail.getFirstName() : "";
        String middle = personalDetail.getMiddleName() != null ? personalDetail.getMiddleName() : "";
        String last = personalDetail.getLastName() != null ? personalDetail.getLastName() : "";
        return String.join(" ", first, middle, last).replaceAll("\\s+", " ").trim();
    }

    public String getPanNumber() {
        return personalDetail != null ? personalDetail.getPanNumber() : null;
    }

    public String getAadhaarNumber() {
        return personalDetail != null ? personalDetail.getAdhaarNumber() : null;
    }

    public String getPassportNumber() {
        return personalDetail != null ? personalDetail.getPassportNumber() : null;
    }

    public String getOrganizationName() {
        return professionalDetail != null ? professionalDetail.getCurrentOrganization() : null;
    }

    public String getDesignation() {
        return professionalDetail != null ? professionalDetail.getCurrentDesignation() : null;
    }
}