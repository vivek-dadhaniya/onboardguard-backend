package com.onboardguard.watchlist.entity;

import com.onboardguard.shared.common.entity.BaseEntity;
import com.onboardguard.shared.common.enums.AliasType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Table(
    name = "watchlist_aliases",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"entry_id", "alias_name_normalized"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Audited
public class WatchlistAlias extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private WatchlistEntry entry;

    @Column(nullable = false)
    private String aliasName;

    @Column(nullable = false)
    private String aliasNameNormalized;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AliasType aliasType;

    // Important: where alias came from
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private WatchlistSource source;
}