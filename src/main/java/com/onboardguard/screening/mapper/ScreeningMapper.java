package com.onboardguard.screening.mapper;

import com.onboardguard.candidate.entity.Candidate;
import com.onboardguard.screening.dto.CandidateScreeningData;
import com.onboardguard.screening.dto.MatchDetailDto;
import com.onboardguard.screening.dto.ScreeningResultDto;
import com.onboardguard.screening.entity.ScreeningMatch;
import com.onboardguard.screening.entity.ScreeningResult;
import com.onboardguard.screening.enums.RiskLevel;
import com.onboardguard.screening.enums.ScreeningStatus;
import org.mapstruct.*;

import java.util.List;

/**
 * Single MapStruct mapper for the entire screening module.
 *
 * Responsibilities:
 *  1.  Candidate entity         → CandidateScreeningData   (strategy input)
 *  2.  MatchDetailDto           → ScreeningMatch entity     (persist after screening)
 *  3.  ScreeningResult entity   → ScreeningResultDto        (full, with matches)
 *  4.  ScreeningResult entity   → ScreeningResultDto        (summary, no matches list)
 *  5.  ScreeningMatch entity    → MatchDetailDto            (case detail view)
 *  6.  ScreeningResultDto       → ScreeningResult entity    (update/merge — @MappingTarget)
 *  7.  List variants of 2, 3, 4, 5
 *  8.  riskLevelToStatus()      helper used by orchestration service
 *
 */
@Mapper(
        componentModel       = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ScreeningMapper {

    // =========================================================================
    // 1.  Candidate entity → CandidateScreeningData
    //     Called by: ScreeningOrchestrationService.buildCandidateData()
    // =========================================================================

    /**
     * Maps the Candidate JPA entity to the flat DTO consumed by every strategy.
     *
     * candidateType: MapStruct maps Enum → String by calling toString() by default.
     * We use an expression to guarantee name() is called (not a custom toString).
     *
     * Normalized fields (lowercase + trimmed + digits-only) cannot be derived
     * by MapStruct directly — they are computed in the @AfterMapping below.
     */
    @Mapping(target = "candidateId",                source = "id")
    @Mapping(target = "fullName",                   source = "fullName")
    @Mapping(target = "panNumber",                  ignore = true)   // normalized in @AfterMapping
    @Mapping(target = "aadhaarNumber",              ignore = true)   // digits-only in @AfterMapping
    @Mapping(target = "passportNumber",             source = "passportNumber")
    @Mapping(target = "organizationName",           source = "organizationName")
    @Mapping(target = "designation",                source = "designation")
    @Mapping(target = "candidateType",              expression = "java(candidate.getCandidateType() != null ? candidate.getCandidateType().name() : null)")
    @Mapping(target = "fullNameNormalized",         ignore = true)   // @AfterMapping
    @Mapping(target = "organizationNameNormalized", ignore = true)   // @AfterMapping
    @Mapping(target = "designationNormalized",      ignore = true)   // @AfterMapping
    CandidateScreeningData toCandidateScreeningData(Candidate candidate);

    /**
     * Fills every normalized / cleaned field after the primary mapping completes.
     *
     * Why @AfterMapping on the Builder?
     * CandidateScreeningData uses @Builder (Lombok immutable) — MapStruct targets
     * the builder, not the final object. We receive the builder here and set the
     * derived fields before build() is called.
     */
    @AfterMapping
    default void normalizeCandidateFields(
            Candidate candidate,
            @MappingTarget CandidateScreeningData.CandidateScreeningDataBuilder builder) {

        // fullNameNormalized — lowercase, trim, collapse spaces
        if (candidate.getFullName() != null) {
            builder.fullNameNormalized(normalize(candidate.getFullName()));
        }

        // panNumber — uppercase, trimmed (e.g. "bbbps1234c " → "BBBPS1234C")
        if (candidate.getPanNumber() != null) {
            builder.panNumber(candidate.getPanNumber().trim().toUpperCase());
        }

        // aadhaarNumber — digits only (removes spaces/dashes the candidate may have typed)
        if (candidate.getAadhaarNumber() != null) {
            builder.aadhaarNumber(candidate.getAadhaarNumber().replaceAll("\\D", ""));
        }

        // organizationNameNormalized
        if (candidate.getOrganizationName() != null) {
            builder.organizationNameNormalized(normalize(candidate.getOrganizationName()));
        }

        // designationNormalized
        if (candidate.getDesignation() != null) {
            builder.designationNormalized(normalize(candidate.getDesignation()));
        }
    }

    // =========================================================================
    // 2.  MatchDetailDto → ScreeningMatch entity
    //     Called by: ScreeningOrchestrationService.persistResult()
    //
    //     Two fields are intentionally left for the service to set manually:
    //       screeningResult → wired via ScreeningResult.addMatch() (bidirectional)
    //       watchlistEntry  → set via watchlistEntryRepository.getReferenceById()
    //     These require a DB lookup / bidirectional wiring that MapStruct cannot do.
    // =========================================================================

    @Mapping(target = "id",                                ignore = true)  // BaseEntity — DB generated
    @Mapping(target = "createdAt",                         ignore = true)  // BaseEntity — JPA audit
    @Mapping(target = "updatedAt",                         ignore = true)  // BaseEntity — JPA audit
    @Mapping(target = "createdBy",                         ignore = true)  // BaseEntity — Spring Security audit
    @Mapping(target = "updatedBy",                         ignore = true)  // BaseEntity — Spring Security audit
    @Mapping(target = "version",                           ignore = true)  // BaseEntity — JPA optimistic locking
    @Mapping(target = "screeningResult",                   ignore = true)  // set by ScreeningResult.addMatch()
    @Mapping(target = "watchlistEntry",                    ignore = true)  // set via getReferenceById in service
    @Mapping(target = "matchType",                         source = "matchType")
    @Mapping(target = "candidateFieldValue",               source = "candidateFieldValue")
    @Mapping(target = "watchlistFieldValue",               source = "watchlistFieldValue")
    @Mapping(target = "similarityScore",                   source = "similarityScore")
    @Mapping(target = "basePoints",                        source = "basePoints")
    @Mapping(target = "sourceCredibilityWeight",           source = "sourceCredibilityWeight")
    @Mapping(target = "corroborationMultiplier",           source = "corroborationMultiplier")
    @Mapping(target = "categoryBonus",                     source = "categoryBonus")
    @Mapping(target = "scoreContribution",                 source = "scoreContribution")
    @Mapping(target = "corroborationLevel",                source = "corroborationLevel")
    // Snapshot fields — DTO name differs from entity column name
    @Mapping(target = "watchlistEntryPrimaryNameSnapshot", source = "watchlistPrimaryName")
    @Mapping(target = "watchlistCategorySnapshot",         source = "watchlistCategory")
    @Mapping(target = "watchlistSeveritySnapshot",         source = "watchlistSeverity")
    @Mapping(target = "watchlistSourceNameSnapshot",       source = "watchlistSourceName")
    ScreeningMatch toScreeningMatchEntity(MatchDetailDto dto);

    /** Bulk variant — used when persisting all matches from a single result. */
    List<ScreeningMatch> toScreeningMatchEntities(List<MatchDetailDto> dtos);

    // =========================================================================
    // 3.  ScreeningResult entity → ScreeningResultDto  (FULL — includes matches)
    //     Called by: ScreeningOrchestrationService.toDto()
    //                ScreeningController.getLatest()
    //
    //     Delegates match list mapping to toMatchDetailDto() below (method #5).
    //     totalEntriesChecked is a runtime value not stored on the entity — the
    //     caller sets it manually after mapping if needed.
    // =========================================================================

    @Mapping(target = "screeningResultId",    source = "id")
    @Mapping(target = "candidateId",          source = "candidate.id")
    @Mapping(target = "strategyUsed",         source = "strategyUsed")
    @Mapping(target = "riskScore",            source = "riskScore")
    @Mapping(target = "riskLevel",            source = "riskLevel")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "matches",              source = "matches")       // → toMatchDetailDto() list
    @Mapping(target = "screeningStartedAt",   source = "screeningStartedAt")
    @Mapping(target = "screeningCompletedAt", source = "screeningCompletedAt")
    @Mapping(target = "totalEntriesChecked",  ignore = true)            // runtime only
    ScreeningResultDto toScreeningResultDto(ScreeningResult entity);

    // =========================================================================
    // 4.  ScreeningResult entity → ScreeningResultDto  (SUMMARY — no matches)
    //     Called by: ScreeningController.getHistory()  (list endpoint)
    //
    //     Omitting matches avoids loading a lazy collection for every row
    //     in the history list — N+1 query prevention.
    // =========================================================================
    @Named("summary")
    @Mapping(target = "screeningResultId",    source = "id")
    @Mapping(target = "candidateId",          source = "candidate.id")
    @Mapping(target = "strategyUsed",         source = "strategyUsed")
    @Mapping(target = "riskScore",            source = "riskScore")
    @Mapping(target = "riskLevel",            source = "riskLevel")
    @Mapping(target = "status",               source = "status")
    @Mapping(target = "matches",              ignore = true)            // intentionally omitted
    @Mapping(target = "screeningStartedAt",   source = "screeningStartedAt")
    @Mapping(target = "screeningCompletedAt", source = "screeningCompletedAt")
    @Mapping(target = "totalEntriesChecked",  ignore = true)
    ScreeningResultDto toScreeningResultDtoSummary(ScreeningResult entity);

    /** Bulk summary — used in getHistory() list response. */
    @IterableMapping(qualifiedByName = "summary")
    List<ScreeningResultDto> toScreeningResultDtoSummaryList(List<ScreeningResult> entities);

    // =========================================================================
    // 5.  ScreeningMatch entity → MatchDetailDto
    //     Called by: ScreeningController.getMatchDetails()
    //                Nested automatically inside toScreeningResultDto() (method #3)
    //
    //     Snapshot fields restore their original DTO names.
    //     watchlistSourceCredibility = sourceCredibilityWeight (same value, different field name).
    // =========================================================================

    @Mapping(target = "watchlistEntryId",          source = "watchlistEntry.id")
    @Mapping(target = "watchlistPrimaryName",       source = "watchlistEntryPrimaryNameSnapshot")
    @Mapping(target = "watchlistCategory",          source = "watchlistCategorySnapshot")
    @Mapping(target = "watchlistSeverity",          source = "watchlistSeveritySnapshot")
    @Mapping(target = "watchlistSourceName",        source = "watchlistSourceNameSnapshot")
    @Mapping(target = "watchlistSourceCredibility", source = "sourceCredibilityWeight")
    @Mapping(target = "matchType",                  source = "matchType")
    @Mapping(target = "candidateFieldValue",        source = "candidateFieldValue")
    @Mapping(target = "watchlistFieldValue",        source = "watchlistFieldValue")
    @Mapping(target = "similarityScore",            source = "similarityScore")
    @Mapping(target = "basePoints",                 source = "basePoints")
    @Mapping(target = "sourceCredibilityWeight",    source = "sourceCredibilityWeight")
    @Mapping(target = "corroborationMultiplier",    source = "corroborationMultiplier")
    @Mapping(target = "categoryBonus",              source = "categoryBonus")
    @Mapping(target = "scoreContribution",          source = "scoreContribution")
    @Mapping(target = "corroborationLevel",         source = "corroborationLevel")
    MatchDetailDto toMatchDetailDto(ScreeningMatch entity);

    /** Bulk — used in the match-detail endpoint (officer case detail view). */
    List<MatchDetailDto> toMatchDetailDtos(List<ScreeningMatch> entities);

    // =========================================================================
    // 6.  ScreeningResultDto → ScreeningResult entity  (UPDATE / MERGE)
    //     Called by: ScreeningOrchestrationService.persistResult()
    //
    //     @MappingTarget = update existing entity in place (no new object created).
    //     This preserves the DB row ID and the threshold snapshots that were
    //     captured when the PENDING row was first created.
    //
    //     Fields that must NEVER be overwritten:
    //       id, candidate, matches          — entity identity / relationships
    //       *ThresholdSnapshot fields       — captured at PENDING time, must not shift
    //       createdAt / createdBy           — JPA audit — immutable after insert
    //       candidateId / totalEntriesChecked — DTO-only fields, no entity target
    // =========================================================================

    @Mapping(target = "id",                      ignore = true)
    @Mapping(target = "createdAt",               ignore = true)
    @Mapping(target = "updatedAt",               ignore = true)
    @Mapping(target = "createdBy",               ignore = true)
    @Mapping(target = "updatedBy",               ignore = true)
    @Mapping(target = "version",                 ignore = true)
    @Mapping(target = "candidate",               ignore = true)
    @Mapping(target = "matches",                 ignore = true)
    @Mapping(target = "mediumThresholdSnapshot", ignore = true)
    @Mapping(target = "highThresholdSnapshot",   ignore = true)
    @Mapping(target = "fuzzyThresholdSnapshot",  ignore = true)
    @Mapping(target = "strategyUsed",            source = "strategyUsed")
    @Mapping(target = "riskScore",               source = "riskScore")
    @Mapping(target = "riskLevel",               source = "riskLevel")
    @Mapping(target = "status",                  source = "status")
    @Mapping(target = "screeningStartedAt",      source = "screeningStartedAt")
    @Mapping(target = "screeningCompletedAt",    source = "screeningCompletedAt")
    void updateScreeningResultFromDto(ScreeningResultDto dto,
                                      @MappingTarget ScreeningResult entity);

    // =========================================================================
    // 7.  Convenience helper — used by ScreeningOrchestrationService
    //     Converts a RiskLevel to the corresponding ScreeningStatus.
    //     Declared here so it lives in one place and is testable.
    // =========================================================================

    default ScreeningStatus riskLevelToStatus(RiskLevel level) {
        if (level == null) return ScreeningStatus.PENDING;
        if (level == RiskLevel.LOW) return ScreeningStatus.CLEAR;
        if (level == RiskLevel.MEDIUM) return ScreeningStatus.REVIEW_NEEDED;
        return ScreeningStatus.FLAGGED;
    }

    // =========================================================================
    // Shared normalization helper
    // Used by @AfterMapping normalizeCandidateFields and any future @AfterMappings.
    // Declared as a default method so it is available inside the interface
    // without a separate utility class dependency.
    // =========================================================================

    default String normalize(String s) {
        if (s == null) return null;
        return s.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}