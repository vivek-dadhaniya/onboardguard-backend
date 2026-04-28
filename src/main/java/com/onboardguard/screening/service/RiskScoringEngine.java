package com.onboardguard.screening.service;

import com.onboardguard.screening.dto.MatchDetailDto;
import com.onboardguard.screening.enums.CorroborationLevel;
import com.onboardguard.screening.enums.RiskLevel;
import com.onboardguard.shared.common.enums.CategoryCode;
import com.onboardguard.shared.common.enums.SeverityLevel;
import com.onboardguard.shared.config.service.SystemConfigService;
import com.onboardguard.watchlist.entity.WatchlistEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts a list of MatchDetailDtos into a single risk score,
 * and classifies that score as LOW / MEDIUM / HIGH.
 *
 * Thresholds are read live from SystemConfig on every call so that
 * admin changes take effect immediately without a restart.
 *
 * Score formula per match:
 *   contribution = (basePoints × sourceCredibility × corroborationMultiplier) + categoryBonus
 *
 * Final score = sum of all contributions, capped at 100.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskScoringEngine {

    // Config keys stored in system_config table
    private static final String KEY_MEDIUM_THRESHOLD = "screening.threshold.medium";
    private static final String KEY_HIGH_THRESHOLD = "screening.threshold.high";
    private static final String KEY_FUZZY_THRESHOLD = "screening.threshold.fuzz";

    private final SystemConfigService systemConfigService;

    // Score calculation
    public double calculateScore(List<MatchDetailDto> matches) {
        if (matches == null || matches.isEmpty()) return 0.0;

        double total = matches.stream()
                .mapToDouble(MatchDetailDto::getScoreContribution)
                .sum();

        return Math.min(total, 100.0);
    }

    // Risk classification
    public RiskLevel classify(double score) {
        double highThreshold   = getThreshold(KEY_HIGH_THRESHOLD, 61.0);
        double mediumThreshold = getThreshold(KEY_MEDIUM_THRESHOLD, 31.0);

        if (score >= highThreshold)   return RiskLevel.HIGH;
        if (score >= mediumThreshold) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    // Multipliers
    public double corroborationMultiplier(CorroborationLevel level) {
        return switch (level) {
            case NAME_ONLY                  -> 0.5;
            case NAME_AND_ONE_ID            -> 0.8;
            case NAME_AND_TWO_IDS           -> 1.0;
            case NAME_AND_ORG               -> 0.75;
            case NAME_ORG_AND_DESIGNATION   -> 1.0;
        };
    }

    public double categoryBonus(WatchlistEntry entry) {
        double bonus = 0.0;

        try {
            CategoryCode code = entry.getCategory().getCode();
            if (code == CategoryCode.CRIMINAL || code == CategoryCode.PEP) {
                bonus += 15.0;
            }
        } catch (IllegalArgumentException e) {
            // categoryCode in DB doesn't match any CategoryCode enum value
            log.warn("Unknown category code '{}' on WatchlistEntry ID {} — no category bonus applied", entry.getCategory().getCode(), entry.getId());
        }

        if (entry.getSeverity() == SeverityLevel.HIGH) {
            bonus += 10.0;
        }

        return bonus;
    }

    // Config snapshot helpers
    public double getMediumThreshold() {
        return getThreshold(KEY_MEDIUM_THRESHOLD, 31.0);
    }
    public double getHighThreshold() {
        return getThreshold(KEY_HIGH_THRESHOLD, 61.0);
    }
    public double getFuzzyThreshold() {
        return getThreshold(KEY_FUZZY_THRESHOLD, 0.80);
    }

    private double getThreshold(String key, double defaultValue) {
        try {
            return systemConfigService.getBigDecimal(key).doubleValue();
        } catch (Exception e) {
            log.warn("Config key '{}' not found, using default={}", key, defaultValue);
            return defaultValue;
        }
    }
}