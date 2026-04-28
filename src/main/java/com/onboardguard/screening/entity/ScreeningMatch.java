package com.onboardguard.screening.entity;

import com.onboardguard.screening.enums.CorroborationLevel;
import com.onboardguard.screening.enums.MatchType;
import com.onboardguard.shared.common.entity.BaseEntity;
import com.onboardguard.watchlist.entity.WatchlistEntry;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "screening_matches",
        indexes = {
                @Index(name = "idx_sm_result_id", columnList = "screening_result_id"),
                @Index(name = "idx_sm_entry_id", columnList = "watchlist_entry_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ScreeningMatch extends BaseEntity {

    // Parent result
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_result_id", nullable = false)
    private ScreeningResult screeningResult;

    // The watchlist entry that was matched
    // NOT_AUDITED because we store a snapshot — we don't want Hibernate Envers
    // to try to revision-track the watchlist entry through this relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_entry_id", nullable = false)
    private WatchlistEntry watchlistEntry;

    // What matched

    // NAME_EXACT / NAME_FUZZY / NAME_ALIAS_EXACT / NAME_ALIAS_FUZZY /
    // PAN_EXACT / AADHAAR_EXACT / ORG_EXACT / ORG_FUZZY / DESIGNATION_EXACT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType;

    // The exact field value from the candidate that matched
    // e.g. "R.S. Sharma" or "BBBPS1234C"
    @Column(nullable = false)
    private String candidateFieldValue;

    // The value from the watchlist entry that was matched against
    // e.g. "Rohit S. Sharma" or the alias "Ravi Kapoor"
    @Column(nullable = false)
    private String watchlistFieldValue;

    // Jaro-Winkler similarity score for fuzzy matches (null for exact matches)
    private Double similarityScore;

    // Scoring snapshot

    // Base points from the scoring table (e.g. +40 for exact name, +25 for fuzzy)
    @Column(nullable = false)
    private Double basePoints;

    // Source credibility weight at time of screening (e.g. 1.0, 0.8, 0.6, 0.4)
    @Column(nullable = false)
    private Double sourceCredibilityWeight;

    // Corroboration multiplier applied (0.5, 0.75, 0.8, 1.0)
    @Column(nullable = false)
    private Double corroborationMultiplier;

    // Category bonus added for CRIMINAL/PEP category (+15) or HIGH severity (+10)
    @Column(nullable = false)
    private Double categoryBonus;

    // Final score contribution = (basePoints × credibility × corroboration) + categoryBonus
    @Column(nullable = false)
    private Double scoreContribution;

    // Corroboration context
    // NAME_ONLY / NAME_AND_ONE_ID / NAME_AND_TWO_IDS / NAME_AND_ORG / NAME_ORG_DESIGNATION
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CorroborationLevel corroborationLevel;

    // Snapshot of watchlist entry's primary name at match time
    // (entry may be modified later — this preserves audit integrity)
    @Column(nullable = false)
    private String watchlistEntryPrimaryNameSnapshot;

    // Snapshot of which category the entry belonged to
    @Column(nullable = false)
    private String watchlistCategorySnapshot;

    // Snapshot of severity at match time
    @Column(nullable = false)
    private String watchlistSeveritySnapshot;

    // Snapshot of source name
    @Column(nullable = false)
    private String watchlistSourceNameSnapshot;
}