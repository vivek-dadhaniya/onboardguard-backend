package com.onboardguard.officer.entity;

import com.onboardguard.shared.common.enums.CaseOutcome;
import com.onboardguard.shared.common.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true)
    private Long alertId;

    // Added for query performance
    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    // Assignment Tracking
    @Column(name = "assigned_officer_id")
    private Long assignedOfficerId; // Current owner

    @Column(name = "assigned_by")
    private Long assignedBy; // Who assigned it (could be self or a manager)

    @Column(name = "assigned_at")
    private Instant assignedAt;

    // SLA Tracking (Crucial for your SlaMonitoringJob)
    @Column(name = "sla_due_date", nullable = false)
    private Instant slaDueDate;

    @Column(name = "is_sla_breached", nullable = false)
    @Builder.Default
    private Boolean isSlaBreached = false;

    // Status & Decision
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private CaseOutcome outcome;

    @Column(name = "outcome_reason", columnDefinition = "TEXT")
    private String outcomeReason;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    // Escalation Tracking
    @Column(name = "escalated_to")
    private Long escalatedTo; // L2 Officer ID

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    // Notes mapping
    @OneToMany(mappedBy = "investigationCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseNote> notes = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

}