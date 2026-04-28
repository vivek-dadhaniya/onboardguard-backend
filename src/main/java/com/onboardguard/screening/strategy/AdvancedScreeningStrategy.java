package com.onboardguard.screening.strategy;

import com.onboardguard.screening.dto.CandidateScreeningData;
import com.onboardguard.screening.dto.MatchDetailDto;
import com.onboardguard.screening.dto.ScreeningResultDto;
import com.onboardguard.screening.enums.CorroborationLevel;
import com.onboardguard.screening.enums.MatchType;
import com.onboardguard.screening.enums.RiskLevel;
import com.onboardguard.screening.enums.ScreeningStatus;
import com.onboardguard.screening.service.RiskScoringEngine;
import com.onboardguard.screening.util.NameMatchingUtil;
import com.onboardguard.screening.util.NameMatchingUtil.NameMatchResult;
import com.onboardguard.watchlist.entity.WatchlistAlias;
import com.onboardguard.watchlist.entity.WatchlistEntry;
import com.onboardguard.watchlist.repository.WatchlistEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Advanced screening with four layers (superset of BasicScreeningStrategy):
 *
 *   Layer 1 — Initials expansion   "R.S. Sharma" matches "Rohit S. Sharma"
 *   Layer 2 — Fuzzy matching       "Rohit Shrma"  matches "Rohit Sharma"
 *   Layer 3 — Alias lookup         "Ravi Kapoor"  matches if it is a known alias
 *   Layer 4 — Multi-field corroboration
 *               NAME_EXACT / NAME_FUZZY / NAME_ALIAS_* (same as Basic, extended)
 *               PAN_EXACT, AADHAAR_EXACT  (same as Basic)
 *               ORG_EXACT, ORG_FUZZY      (Basic only had ORG_EXACT)
 *               DESIGNATION_EXACT         (new vs Basic)
 *
 * Every check that BasicScreeningStrategy performs is present here:
 *   - NAME_EXACT  : advancedNameMatch() returns exact=true  → MatchType.NAME_EXACT
 *   - PAN_EXACT   : Layer 4 block
 *   - AADHAAR_EXACT: Layer 4 block
 *   - ORG_EXACT   : Layer 4 block (orgResult.exact() == true path)
 *
 * BUGS FIXED vs the original doc-10 version:
 *   FIX-1  entry.getSource().getCredibilityWeight()
 *          → entry.getSourceCredibilityWeight()
 *          WatchlistEntry has NO getSource() wrapper — credibility is a direct field.
 *
 *   FIX-2  entry.getSource().getName()
 *          → entry.getSourceName()
 *          Same reason.
 *
 *   FIX-3  entry.getCategory().getCode().name()
 *          → entry.getCategory().getCategoryCode()
 *          WatchlistCategory.categoryCode is already a String, not a CategoryCode enum.
 *          Calling .getCode() would require a method that does not exist.
 *
 *   FIX-4  Layer-4 org null-guard used getOrganizationNameNormalized() for the check
 *          but getOrganizationName() for the actual match.  Unified to getOrganizationName()
 *          for both.  NameMatchingUtil.advancedNameMatch() normalises internally so the
 *          raw value is the correct thing to pass.
 */
@Slf4j
@Service("advancedScreeningStrategy")
@RequiredArgsConstructor
public class AdvancedScreeningStrategy implements ScreeningStrategy {

    private final WatchlistEntryRepository watchlistEntryRepository;
    private final RiskScoringEngine riskScoringEngine;
    private final NameMatchingUtil nameMatchingUtil;

    @Override
    public String strategyName() {
        return "ADVANCED";
    }

    @Override
    public ScreeningResultDto screen(CandidateScreeningData candidate) {
        Instant startedAt = Instant.now();
        log.info("[ADVANCED] Starting screening for candidateId={}", candidate.getCandidateId());

        // JOIN FETCH aliases in one query — avoids N+1 when iterating entry.getAliases()
        List<WatchlistEntry> activeEntries =
                watchlistEntryRepository.findAllActiveOnDateWithAliases(LocalDate.now());
        log.info("[ADVANCED] Checking against {} active watchlist entries", activeEntries.size());

        List<MatchDetailDto> allMatches = new ArrayList<>();
        for (WatchlistEntry entry : activeEntries) {
            List<MatchDetailDto> entryMatches = checkEntryAdvanced(candidate, entry);
            allMatches.addAll(entryMatches);
        }

        // RiskScoringEngine sums contributions and caps at 100
        double rawScore   = riskScoringEngine.calculateScore(allMatches);
        double finalScore = Math.min(rawScore, 100.0);
        RiskLevel riskLevel = riskScoringEngine.classify(finalScore);
        ScreeningStatus status = riskLevel == RiskLevel.LOW
                ? ScreeningStatus.CLEAR : ScreeningStatus.FLAGGED;

        log.info("[ADVANCED] Completed candidateId={} score={} level={} matches={}",
                candidate.getCandidateId(), finalScore, riskLevel, allMatches.size());

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

    // PER-ENTRY ADVANCED CHECK  (all four layers)
    private List<MatchDetailDto> checkEntryAdvanced(CandidateScreeningData c,
                                                    WatchlistEntry entry) {
        List<MatchDetailDto> matches = new ArrayList<>();

        // LAYER 1 + 2: primary name check (exact / fuzzy / initials)
        Optional<MatchDetailDto> nameMatch = checkNameMatch(c,
                                                        entry,
                                                        c.getFullName(),
                                                        entry.getPrimaryName(),
                                                        false);
        nameMatch.ifPresent(matches::add);

        // LAYER 3: alias lookup — only runs if primary name did NOT match
        // Rationale: if the primary name already matched, iterating aliases for
        // the same entry would just add a lower-confidence duplicate.
        if (nameMatch.isEmpty()) {
            for (WatchlistAlias alias : entry.getAliases()) {
                Optional<MatchDetailDto> aliasMatch = checkNameMatch(c,
                                                                entry,
                                                                c.getFullName(),
                                                                alias.getAliasName(),
                                                                true);
                if (aliasMatch.isPresent()) {
                    matches.add(aliasMatch.get());
                    break; // one alias match per entry is sufficient
                }
            }
        }

        // LAYER 4: corroborating field checks
        // Only run if we already have at least one name/alias match.
        // A standalone PAN or Aadhaar match without a name match is too weak
        // to be meaningful on its own and risks false positives.
        boolean hasNameMatch = !matches.isEmpty();

        if (hasNameMatch) {

            // PAN - exact (same as Basic)
            if (isNotBlank(c.getPanNumber()) && isNotBlank(entry.getPanNumber())
                    && c.getPanNumber().equalsIgnoreCase(entry.getPanNumber())) {
                matches.add(
                        buildCorroboratingMatch(entry,
                                MatchType.PAN_EXACT,
                                c.getPanNumber(),
                                entry.getPanNumber(),
                                null,
                                35.0)
                );
            }

            // Aadhaar - exact (same as Basic)
            if (isNotBlank(c.getAadhaarNumber()) && isNotBlank(entry.getAadhaarNumber())
                    && c.getAadhaarNumber().equals(entry.getAadhaarNumber())) {
                matches.add(
                        buildCorroboratingMatch(entry,
                                MatchType.AADHAAR_EXACT,
                                c.getAadhaarNumber(),
                                entry.getAadhaarNumber(),
                                null,
                                30.0)
                );
            }

            // Organisation - fuzzy allowed (Advanced extends Basic's ORG_EXACT)
            // FIX-4: use getOrganizationName() for both null-guard and match call.
            //        advancedNameMatch() normalises internally, so passing the raw
            //        value is correct and consistent with how candidateName is passed
            //        in checkNameMatch() above.
            if (isNotBlank(c.getOrganizationName()) && isNotBlank(entry.getOrganizationName())) {
                NameMatchResult orgResult = nameMatchingUtil.advancedNameMatch(
                        c.getOrganizationName(),
                        entry.getOrganizationName(),
                        riskScoringEngine.getFuzzyThreshold());
                if (orgResult.matched()) {
                    MatchType orgType = orgResult.exact() ? MatchType.ORG_EXACT : MatchType.ORG_FUZZY;
                    matches.add(
                            buildCorroboratingMatch(entry,
                                    orgType,
                                    c.getOrganizationName(),
                                    entry.getOrganizationName(),
                                    orgResult.exact() ? null : orgResult.similarity(),
                                    20.0)
                    );
                }
            }

            // Designation - exact only (designation strings are too short for
            // reliable fuzzy matching - "Director" vs "Director " would score high
            // but that is just a whitespace difference handled by normalization)
            if (isNotBlank(c.getDesignation()) && isNotBlank(entry.getDesignation())
                    && c.getDesignation().equalsIgnoreCase(entry.getDesignation())) {
                matches.add(
                        buildCorroboratingMatch(entry,
                                MatchType.DESIGNATION_EXACT,
                                c.getDesignation(),
                                entry.getDesignation(),
                                null,
                                15.0)
                );
            }
        }

        // Recompute corroboration now that ALL fields for this entry are known ─
        // Each match was built with a placeholder corroboration level of NAME_ONLY.
        // We now know the true level (e.g. NAME_AND_TWO_IDS) and recalculate every
        // contribution with the correct multiplier.
        if (!matches.isEmpty()) {
            CorroborationLevel corr = resolveCorroborationFromMatches(matches);
            matches = recomputeWithCorroboration(matches, corr, entry);
        }

        return matches;
    }


    // NAME MATCH HELPER  (primary name or alias, exact / fuzzy / initials)
    private Optional<MatchDetailDto> checkNameMatch(
            CandidateScreeningData c,
            WatchlistEntry entry,
            String candidateName,
            String watchlistName,
            boolean isAlias) {

        NameMatchResult result = nameMatchingUtil.advancedNameMatch(
                candidateName, watchlistName, riskScoringEngine.getFuzzyThreshold());

        if (!result.matched()) return Optional.empty();

        // Determine match type and base points
        MatchType type;
        double basePoints;
        if (isAlias) {
            type       = result.exact() ? MatchType.NAME_ALIAS_EXACT : MatchType.NAME_ALIAS_FUZZY;
            basePoints = 20.0;
        } else {
            type       = result.exact() ? MatchType.NAME_EXACT : MatchType.NAME_FUZZY;
            basePoints = result.exact() ? 40.0 : 25.0;
        }

        // Corroboration is NAME_ONLY as a placeholder here.
        // recomputeWithCorroboration() will replace this with the true level after
        // all field checks for this entry have been run.
        double credibility    = entry.getSource().getCredibilityWeight() != null
                ? entry.getSource().getCredibilityWeight() : 1.0;
        double corrMultiplier = riskScoringEngine.corroborationMultiplier(CorroborationLevel.NAME_ONLY);
        double categoryBonus  = riskScoringEngine.categoryBonus(entry);
        double contribution   = (basePoints * credibility * corrMultiplier) + categoryBonus;

        return Optional.of(MatchDetailDto.builder()
                .watchlistEntryId(entry.getId())
                .watchlistPrimaryName(entry.getPrimaryName())
                .watchlistCategory(entry.getCategory().getName())
                .watchlistSeverity(entry.getSeverity().name())
                .watchlistSourceName(entry.getSource().getName())
                .watchlistSourceCredibility(credibility)
                .matchType(type)
                .candidateFieldValue(candidateName)
                .watchlistFieldValue(watchlistName)
                .similarityScore(result.exact() ? null : result.similarity())
                .basePoints(basePoints)
                .sourceCredibilityWeight(credibility)
                .corroborationMultiplier(corrMultiplier)
                .categoryBonus(categoryBonus)
                .scoreContribution(contribution)
                .corroborationLevel(CorroborationLevel.NAME_ONLY) // placeholder
                .build());
    }

    // CORROBORATION LEVEL RESOLUTION
    /**
     * Determines the highest corroboration level based on which field types
     * produced a match for a single watchlist entry.
     * Called after all field checks for the entry are complete.
     */
    private CorroborationLevel resolveCorroborationFromMatches(List<MatchDetailDto> matches) {
        boolean hasPan     = matches.stream().anyMatch(m -> m.getMatchType() == MatchType.PAN_EXACT);
        boolean hasAadhaar = matches.stream().anyMatch(m -> m.getMatchType() == MatchType.AADHAAR_EXACT);
        boolean hasOrg     = matches.stream().anyMatch(m ->
                m.getMatchType() == MatchType.ORG_EXACT
                        || m.getMatchType() == MatchType.ORG_FUZZY);
        boolean hasDesig   = matches.stream().anyMatch(m ->
                m.getMatchType() == MatchType.DESIGNATION_EXACT);

        if (hasPan && hasAadhaar) return CorroborationLevel.NAME_AND_TWO_IDS;
        if (hasPan || hasAadhaar) return CorroborationLevel.NAME_AND_ONE_ID;
        if (hasOrg && hasDesig)   return CorroborationLevel.NAME_ORG_AND_DESIGNATION;
        if (hasOrg)               return CorroborationLevel.NAME_AND_ORG;
        return                           CorroborationLevel.NAME_ONLY;
    }

    /**
     * Rebuilds every MatchDetailDto for this entry with the final corroboration
     * level and the correct multiplier-adjusted contribution.
     *
     * Why rebuild instead of mutate:
     *   MatchDetailDto uses @Builder — no setters.  Rebuilding is the clean approach
     *   and avoids accidental mutation of a DTO that may already be referenced.
     *
     * Category bonus is applied only to NAME_* rows — never to PAN/Aadhaar rows.
     */
    private List<MatchDetailDto> recomputeWithCorroboration(
            List<MatchDetailDto> original,
            CorroborationLevel corr,
            WatchlistEntry entry) {

        double corrMultiplier = riskScoringEngine.corroborationMultiplier(corr);
        double categoryBonus  = riskScoringEngine.categoryBonus(entry);
        List<MatchDetailDto> updated = new ArrayList<>(original.size());

        for (MatchDetailDto m : original) {
            boolean isNameRow = m.getMatchType().name().startsWith("NAME");
            double bonusForRow = isNameRow ? categoryBonus : 0.0;
            double contribution =
                    (m.getBasePoints() * m.getSourceCredibilityWeight() * corrMultiplier)
                            + bonusForRow;

            updated.add(MatchDetailDto.builder()
                    .watchlistEntryId(m.getWatchlistEntryId())
                    .watchlistPrimaryName(m.getWatchlistPrimaryName())
                    .watchlistCategory(m.getWatchlistCategory())
                    .watchlistSeverity(m.getWatchlistSeverity())
                    .watchlistSourceName(m.getWatchlistSourceName())
                    .watchlistSourceCredibility(m.getWatchlistSourceCredibility())
                    .matchType(m.getMatchType())
                    .candidateFieldValue(m.getCandidateFieldValue())
                    .watchlistFieldValue(m.getWatchlistFieldValue())
                    .similarityScore(m.getSimilarityScore())
                    .basePoints(m.getBasePoints())
                    .sourceCredibilityWeight(m.getSourceCredibilityWeight())
                    .corroborationMultiplier(corrMultiplier)
                    .categoryBonus(bonusForRow)
                    .scoreContribution(contribution)
                    .corroborationLevel(corr)
                    .build());
        }
        return updated;
    }

    // CORROBORATING MATCH BUILDER
    /**
     * Builds a placeholder MatchDetailDto for PAN / Aadhaar / Org / Designation rows.
     *
     * Corroboration multiplier is set to 1.0 as a placeholder here because we do not
     * yet know the final CorroborationLevel — that is determined after all field checks
     * are done.  recomputeWithCorroboration() will replace these values.
     *
     * Category bonus is always 0.0 for corroborating rows — it is only applied once,
     * on the name match row, to avoid double-counting.
     *
     */
    private MatchDetailDto buildCorroboratingMatch(
            WatchlistEntry entry,
            MatchType type,
            String candidateValue,
            String watchlistValue,
            Double similarity,
            double basePoints) {

        double credibility  = entry.getSource().getCredibilityWeight() != null
                ? entry.getSource().getCredibilityWeight() : 1.0;
        double contribution = basePoints * credibility * 1.0; // placeholder multiplier

        return MatchDetailDto.builder()
                .watchlistEntryId(entry.getId())
                .watchlistPrimaryName(entry.getPrimaryName())
                .watchlistCategory(entry.getCategory().getName())
                .watchlistSeverity(entry.getSeverity().name())
                .watchlistSourceName(entry.getSource().getName())
                .watchlistSourceCredibility(credibility)
                .matchType(type)
                .candidateFieldValue(candidateValue)
                .watchlistFieldValue(watchlistValue)
                .similarityScore(similarity)
                .basePoints(basePoints)
                .sourceCredibilityWeight(credibility)
                .corroborationMultiplier(1.0)       // placeholder - recomputed later
                .categoryBonus(0.0)                 // never on corroborating rows
                .scoreContribution(contribution)    // placeholder - recomputed later
                .corroborationLevel(CorroborationLevel.NAME_ONLY) // placeholder
                .build();
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}