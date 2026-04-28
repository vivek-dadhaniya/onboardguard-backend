package com.onboardguard.watchlist.entity;

import com.onboardguard.shared.common.entity.BaseEntity;
import com.onboardguard.shared.common.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "watchlist_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WatchlistSource extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code; // SEBI_DEBARRED, RBI_DEFAULTER

    @Column(nullable = false)
    private String name; // SEBI Debarred List

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    @Column(nullable = false)
    private Double credibilityWeight; // 1.0, 0.8, etc.

    @Builder.Default
    private Boolean active = true;
}
