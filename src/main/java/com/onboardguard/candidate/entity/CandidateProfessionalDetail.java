package com.onboardguard.candidate.entity;

import com.onboardguard.shared.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "candidate_professional_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CandidateProfessionalDetail extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    private String currentOrganization;
    private String cinNumber;
    private String dinNumber;
    private String currentDesignation;

    @Column(precision = 4, scale = 2)
    private BigDecimal totalExperienceYears;

    private String previousOrganization;
    private String previousDesignation;

    private String vendorCompanyName;
    private String vendorGstNumber;

    private String highestQualification;
    private String universityName;
    private Integer graduationYear;
}