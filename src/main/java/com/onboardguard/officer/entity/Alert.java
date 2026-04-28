package com.onboardguard.officer.entity;

import com.onboardguard.shared.common.enums.AlertStatus;
import com.onboardguard.shared.common.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "screening_result_id", nullable = false)
    private Long screeningResultId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    // Hibernate 6 magic: Automatically maps Java List<String> to PostgreSQL JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_categories", columnDefinition = "jsonb")
    private List<String> matchedCategories;

    @Column(name = "sla_deadline", nullable = false)
    private Instant slaDeadline;

    @Column(name = "is_sla_breached", nullable = false)
    @Builder.Default
    private Boolean isSlaBreached = false;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy; // ID of the L1 Officer

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}