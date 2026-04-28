package com.onboardguard.watchlist.entity;

import com.onboardguard.shared.common.entity.BaseEntity;
import com.onboardguard.shared.common.enums.EvidenceType;
import com.onboardguard.shared.common.enums.FileFormat;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Table(
    name = "watchlist_evidence_documents",
    indexes = {
        @Index(name = "idx_entry_id", columnList = "entry_id"),
        @Index(name = "idx_source_id", columnList = "source_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
public class WatchlistEvidenceDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private WatchlistEntry entry;

    @Column(nullable = false)
    private String cloudStorageKey; // S3 / GCS key

    @Column(nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileFormat fileFormat;

    @Enumerated(EnumType.STRING)
    private EvidenceType evidenceType; 
    // COURT_ORDER, NEWS_ARTICLE, REGULATORY_NOTICE

    // Link to source (VERY IMPORTANT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private WatchlistSource source;
}