package com.onboardguard.watchlist.entity;

import com.onboardguard.shared.common.entity.BaseEntity;
import com.onboardguard.shared.common.enums.CategoryCode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "watchlist_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WatchlistCategory extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private CategoryCode code;

    @Column(nullable = false)
    private String name; // Fraud, Criminal, etc.

    @Column(nullable = false)
    private Integer baseRiskScore; // 50, 70, 90

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}