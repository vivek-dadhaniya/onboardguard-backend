package com.onboardguard.screening.strategy;

import com.onboardguard.screening.dto.CandidateScreeningData;
import com.onboardguard.screening.dto.MatchDetailDto;
import com.onboardguard.screening.dto.ScreeningResultDto;
import com.onboardguard.screening.enums.*;
import com.onboardguard.screening.service.RiskScoringEngine;
import com.onboardguard.screening.util.NameMatchingUtil;
import com.onboardguard.watchlist.entity.WatchlistEntry;
import com.onboardguard.watchlist.repository.WatchlistEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs simple, deterministic matching:
 *  - Name: case-insensitive exact match (normalized)
 *  - PAN: exact string match
 *  - Aadhaar: exact string match
 *  - Organization: exact string match
 *
 * Fast but will miss initials, typos, and aliases.
 */
@Slf4j
@Service("basicScreeningStrategy")
@RequiredArgsConstructor
public class BasicScreeningStrategy implements ScreeningStrategy {

    private final WatchlistEntryRepository watchlistEntryRepository;
    private final RiskScoringEngine riskScoringEngine;
    private final NameMatchingUtil nameMatchingUtil;

    @Override
    public String strategyName() {
        return "BASIC";
    }

    @Override
    public ScreeningResultDto screen(CandidateScreeningData candidate) {
        Instant startedAt = Instant.now();
        log.info("[BASIC] Starting screening for candidateId={}", candidate.getCandidateId());

        // Fetch only ACTIVE watchlist entries whose effective date window covers today
        List<WatchlistEntry> activeEntries = watchlistEntryRepository.findAllActiveOnDate(LocalDate.now());
        log.info("[BASIC] Checking against {} active watchlist entries", activeEntries.size());

        List<MatchDetailDto> allMatches = new ArrayList<>();

        for (WatchlistEntry entry : activeEntries) {
            List<MatchDetailDto> entryMatches = checkEntryBasic(candidate, entry);
            allMatches.addAll(entryMatches);
        }

        double rawScore = riskScoringEngine.calculateScore(allMatches);
        double finalScore = Math.min(rawScore, 100.0); // cap at 100

        RiskLevel riskLevel = riskScoringEngine.classify(finalScore);
        ScreeningStatus status = riskLevel == RiskLevel.LOW ? ScreeningStatus.CLEAR : ScreeningStatus.FLAGGED;

        log.info("[BASIC] Completed candidateId={} score={} level={}", candidate.getCandidateId(), finalScore, riskLevel);

        return ScreeningResultDto.builder()
                .candidateId(candidate.getCandidateId())
                .strategyUsed(strategyName())
                .riskScore(finalScore)
                .riskLevel(riskLevel)
                .status(status)
                .matches(allMatches)
                .totalEntriesChecked(activeEntries.size())
                .screeningStartedAt(startedAt)
                .screeningCompletedAt(Instant.now())
                .build();
    }

    // Per-entry basic checks
    private List<MatchDetailDto> checkEntryBasic(CandidateScreeningData c, WatchlistEntry entry) {
        List<MatchDetailDto> matches = new ArrayList<>();

        // Name: exact, case-insensitive
        if (nameMatchingUtil.isExactMatch(c.getFullName(), entry.getPrimaryName())) {
            CorroborationLevel corr = resolveCorroboration(c, entry);
            matches.add(
                    buildMatch(c,
                            entry,
                            MatchType.NAME_EXACT,
                            c.getFullName(),
                            entry.getPrimaryName(),
                            null,           // no similarity score for exact
                            40.0,
                            corr,
                            true)
            );
        }

        // PAN: exact
        if (isNotBlank(c.getPanNumber()) && isNotBlank(entry.getPanNumber())
                && c.getPanNumber().equalsIgnoreCase(entry.getPanNumber())) {
            matches.add(
                    buildMatch(c,
                            entry,
                            MatchType.PAN_EXACT,
                            c.getPanNumber(),
                            entry.getPanNumber(),
                            null,
                            35.0,
                            CorroborationLevel.NAME_ONLY,
                            false)
            ); // standalone PAN match — corroboration handled in scoring engine
        }

        // Aadhaar: exact
        if (isNotBlank(c.getAadhaarNumber()) && isNotBlank(entry.getAadhaarNumber())
                && c.getAadhaarNumber().equals(entry.getAadhaarNumber())) {
            matches.add(
                    buildMatch(c,
                            entry,
                            MatchType.AADHAAR_EXACT,
                            c.getAadhaarNumber(),
                            entry.getAadhaarNumber(),
                            null,
                            30.0,
                            CorroborationLevel.NAME_ONLY,
                            false)
            );
        }

        // Organization: exact
        if (isNotBlank(c.getOrganizationName()) && isNotBlank(entry.getOrganizationName())
                && nameMatchingUtil.isExactMatch(c.getOrganizationName(), entry.getOrganizationName())) {
            CorroborationLevel corr = resolveCorroboration(c, entry);
            matches.add(
                    buildMatch(c,
                            entry,
                            MatchType.ORG_EXACT,
                            c.getOrganizationName(),
                            entry.getOrganizationName(),
                            null,
                            20.0,
                            corr,
                            false)
            );
        }

        return matches;
    }

    /**
     * Determine the corroboration level for this entry based on how many
     * ID fields also match. This is used by the scoring engine to pick the
     * right multiplier (0.5 -> 1.0).
     */
    private CorroborationLevel resolveCorroboration(CandidateScreeningData c, WatchlistEntry entry) {
        boolean panMatch    = isNotBlank(c.getPanNumber())
                && c.getPanNumber().equalsIgnoreCase(entry.getPanNumber());
        boolean aadhaarMatch = isNotBlank(c.getAadhaarNumber())
                && c.getAadhaarNumber().equals(entry.getAadhaarNumber());
        boolean orgMatch    = isNotBlank(c.getOrganizationName())
                && nameMatchingUtil.isExactMatch(c.getOrganizationName(), entry.getOrganizationName());
        boolean desigMatch  = isNotBlank(c.getDesignation())
                && c.getDesignation().equalsIgnoreCase(entry.getDesignation());

        if (panMatch && aadhaarMatch)                return CorroborationLevel.NAME_AND_TWO_IDS;
        if (panMatch || aadhaarMatch)                return CorroborationLevel.NAME_AND_ONE_ID;
        if (orgMatch && desigMatch)                  return CorroborationLevel.NAME_ORG_AND_DESIGNATION;
        if (orgMatch)                                return CorroborationLevel.NAME_AND_ORG;
        return                                              CorroborationLevel.NAME_ONLY;
    }


    private MatchDetailDto buildMatch(CandidateScreeningData c, WatchlistEntry entry,
                                      MatchType type,           String candidateValue,
                                      String watchlistValue,    Double similarity,
                                      double basePoints,        CorroborationLevel corr,
                                      boolean isNameRow) {

        double credibility    = entry.getSource().getCredibilityWeight();
        double corrMultiplier = riskScoringEngine.corroborationMultiplier(corr);
        double categoryBonus  = isNameRow ? riskScoringEngine.categoryBonus(entry) : 0.0;
        double contribution   = (basePoints * credibility * corrMultiplier) + categoryBonus;

        return MatchDetailDto.builder()
                .watchlistEntryId(entry.getId())
                .watchlistPrimaryName(entry.getPrimaryName())
                .watchlistCategory(entry.getCategory().getCode().name())
                .watchlistSeverity(entry.getSeverity().name())
                .watchlistSourceName(entry.getSource().getName())
                .watchlistSourceCredibility(credibility)
                .matchType(type)
                .candidateFieldValue(candidateValue)
                .watchlistFieldValue(watchlistValue)
                .similarityScore(similarity)
                .basePoints(basePoints)
                .sourceCredibilityWeight(credibility)
                .corroborationMultiplier(corrMultiplier)
                .categoryBonus(categoryBonus)
                .scoreContribution(contribution)
                .corroborationLevel(corr)
                .build();
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}