package com.onboardguard.watchlist.entity;

import com.onboardguard.shared.common.entity.BaseEntity;
import com.onboardguard.shared.common.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "watchlist_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
public class WatchlistEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private WatchlistCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private WatchlistSource source;

    @Column(nullable = false)
    private String primaryName;

    @Column(nullable = false)
    private String primaryNameNormalized;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityLevel severity;

    // Identifiers
    private String panNumber;
    private String aadhaarNumber;
    private String passportNumber;
    private String dinNumber;
    private String cinNumber;

    // Flexible JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_specific_data", columnDefinition = "jsonb")
    private Map<String, Object> categorySpecificData;

    // Profile Info
    private String organizationName;
    private String designation;
    private LocalDate dateOfBirth;
    private String nationality;

    // Validity
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // Approval Workflow
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    private String notes;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<WatchlistAlias> aliases = new ArrayList<>();
    
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<WatchlistEvidenceDocument> evidenceDocuments = new ArrayList<>();
}