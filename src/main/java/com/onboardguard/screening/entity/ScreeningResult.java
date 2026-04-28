package com.onboardguard.screening.entity;

import com.onboardguard.candidate.entity.Candidate;
import com.onboardguard.screening.enums.RiskLevel;
import com.onboardguard.screening.enums.ScreeningStatus;
import com.onboardguard.shared.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screening_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ScreeningResult extends BaseEntity {

    // Which candidate this screening belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    // Which strategy was used (BASIC or ADVANCED)
    @Column(nullable = false)
    private String strategyUsed;

    // Final calculated risk score (0–100, capped)
    @Column(nullable = false)
    private Double riskScore;

    // LOW / MEDIUM / HIGH
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    // PENDING / IN_PROGRESS / CLEAR / FLAGGED / REVIEW_NEEDED / RE_SCREENED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScreeningStatus status;

    // Thresholds that were active at the time of THIS screening
    // Stored here so historical records don't shift if admin changes thresholds later
    @Column(nullable = false)
    private Double mediumThresholdSnapshot;

    @Column(nullable = false)
    private Double highThresholdSnapshot;

    // Fuzzy threshold used during this screening run
    @Column(nullable = false)
    private Double fuzzyThresholdSnapshot;

    // When screening actually started and completed
    private Instant screeningStartedAt;
    private Instant screeningCompletedAt;

    // All individual watchlist matches that contributed to the score
    // CascadeType.ALL: matches are owned by the result — delete result → delete matches
    @OneToMany(mappedBy = "screeningResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScreeningMatch> matches = new ArrayList<>();

    // Helper — keeps both sides of the relationship in sync
    public void addMatch(ScreeningMatch match) {
        match.setScreeningResult(this);
        this.matches.add(match);
    }
}