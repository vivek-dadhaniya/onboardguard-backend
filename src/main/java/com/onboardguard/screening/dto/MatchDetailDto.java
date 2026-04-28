package com.onboardguard.screening.dto;

import com.onboardguard.screening.enums.CorroborationLevel;
import com.onboardguard.screening.enums.MatchType;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents one individual match found during screening.
 * Collected by strategies and returned inside ScreeningResultDto.
 */
@Getter
@Builder
public class MatchDetailDto {

    private Long   watchlistEntryId;
    private String watchlistPrimaryName;
    private String watchlistCategory;
    private String watchlistSeverity;
    private String watchlistSourceName;
    private Double watchlistSourceCredibility;

    // What type of match was found
    private MatchType matchType;

    // The actual values compared
    private String candidateFieldValue;
    private String watchlistFieldValue;

    // Similarity score — null for exact matches
    private Double similarityScore;

    // Score breakdown
    private Double basePoints;
    private Double sourceCredibilityWeight;
    private Double corroborationMultiplier;
    private Double categoryBonus;
    private Double scoreContribution;

    private CorroborationLevel corroborationLevel;
}