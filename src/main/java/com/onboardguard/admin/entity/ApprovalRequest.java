package com.onboardguard.admin.entity;

import com.onboardguard.shared.common.enums.ActionType;
import com.onboardguard.shared.common.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "approval_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(name = "target_entity_type", nullable = false, length = 50)
    private String targetEntityType;

    // Can be null for CREATE actions since the entity doesn't have an ID yet
    @Column(name = "target_entity_id")
    private Long targetEntityId;

    // Maps directly to PostgreSQL JSONB using Hibernate 6+
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy; // Maker's User ID

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy; // Checker's User ID

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "is_bypass", nullable = false)
    @Builder.Default
    private Boolean isBypass = false;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Automatically sets the requested timestamp right before Hibernate
     * inserts the record into the database.
     */
    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
        if (this.isBypass == null) {
            this.isBypass = false;
        }
    }
}