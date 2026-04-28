package com.onboardguard.screening.service;


import com.onboardguard.candidate.entity.Candidate;
import com.onboardguard.candidate.repository.CandidateRepository;
import com.onboardguard.screening.dto.CandidateScreeningData;
import com.onboardguard.screening.dto.MatchDetailDto;
import com.onboardguard.screening.dto.ScreeningResultDto;
import com.onboardguard.screening.entity.ScreeningMatch;
import com.onboardguard.screening.entity.ScreeningResult;
import com.onboardguard.screening.enums.RiskLevel;
import com.onboardguard.screening.enums.ScreeningStatus;
import com.onboardguard.screening.mapper.ScreeningMapper;
import com.onboardguard.screening.repository.ScreeningResultRepository;
import com.onboardguard.screening.strategy.ScreeningStrategy;
import com.onboardguard.shared.config.service.SystemConfigService;
import com.onboardguard.watchlist.repository.WatchlistEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Orchestrates the full screening lifecycle:
 *
 *  1. Map Candidate → CandidateScreeningData  (via ScreeningMapper)
 *  2. Resolve active ScreeningStrategy at RUNTIME from SystemConfig  (Dynamic DI)
 *  3. Invoke strategy → get ScreeningResultDto
 *  4. Merge result into the PENDING entity    (via ScreeningMapper.updateScreeningResultFromDto)
 *  5. Map MatchDetailDtos → ScreeningMatch entities  (via ScreeningMapper)
 *  6. Persist everything, trigger alert, update candidate status
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningOrchestrationService {

    private static final String KEY_ACTIVE_STRATEGY = "screening.active.strategy";

    private final Map<String, ScreeningStrategy> strategyMap;

    private final CandidateRepository candidateRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final WatchlistEntryRepository watchlistEntryRepository;
    private final SystemConfigService systemConfigService;
    private final RiskScoringEngine riskScoringEngine;
    private final ScreeningMapper screeningMapper;

    @Transactional
    @PreAuthorize("hasAuthority('SCREENING_RESCREEN')")
    public ScreeningResultDto runScreening(Long candidateId) {
        log.info("Screening triggered for candidateId={}", candidateId);

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new EntityNotFoundException("Candidate not found: " + candidateId));

        if (screeningResultRepository.existsByCandidateIdAndStatus(candidateId, ScreeningStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Screening already in progress for candidate: " + candidateId);
        }

        // 1. Resolve strategy (Dynamic DI - reads DB config on every call)
        ScreeningStrategy strategy = resolveActiveStrategy();
        log.info("Using strategy={} for candidateId={}", strategy.strategyName(), candidateId);

        // 2. Build input DTO via mapper (no manual normalize() calls needed)
        CandidateScreeningData candidateData = screeningMapper.toCandidateScreeningData(candidate);

        // 3. Create PENDING result row immediately (candidate sees "In Progress")
        ScreeningResult pendingResult = createPendingResult(candidate, strategy.strategyName());

        // 4. Mark IN_PROGRESS
        pendingResult.setStatus(ScreeningStatus.IN_PROGRESS);
        pendingResult.setScreeningStartedAt(Instant.now());
        screeningResultRepository.save(pendingResult);

        candidate.setScreeningStatus(ScreeningStatus.IN_PROGRESS);
        candidateRepository.save(candidate);

        try {
            // 5. Run the strategy
            ScreeningResultDto resultDto = strategy.screen(candidateData);

            // 6. Merge strategy output into the existing PENDING entity via mapper
            //    (updates riskScore, riskLevel, status, timestamps - never touches id/thresholds)
            screeningMapper.updateScreeningResultFromDto(resultDto, pendingResult);

            // 7. Build and attach ScreeningMatch entities via mapper
            persistMatches(resultDto, pendingResult);

            // 8. Save the fully populated result
            ScreeningResult savedResult = screeningResultRepository.save(pendingResult);

            // 9. Update candidate status using mapper helper (riskLevelToStatus)
           ScreeningStatus newStatus = ScreeningStatus.valueOf(screeningMapper.riskLevelToStatus(resultDto.getRiskLevel()).name());
            candidate.setScreeningStatus(newStatus);
            candidateRepository.save(candidate);

            // 10. Trigger alert if MEDIUM or HIGH (Alert Module missing)
            // if (resultDto.getRiskLevel() == RiskLevel.MEDIUM
            //         || resultDto.getRiskLevel() == RiskLevel.HIGH) {
            //     alertService.createAlert(savedResult);
            // }

            log.info("Screening complete candidateId={} score={} level={}",
                    candidateId, resultDto.getRiskScore(), resultDto.getRiskLevel());

            // 11. Return summary DTO (no match list — caller fetches matches separately)
            return screeningMapper.toScreeningResultDtoSummary(savedResult);

        } catch (Exception ex) {
            pendingResult.setStatus(ScreeningStatus.PENDING);
            screeningResultRepository.save(pendingResult);
            candidate.setScreeningStatus(ScreeningStatus.PENDING);
            candidateRepository.save(candidate);
            log.error("Screening failed for candidateId={}", candidateId, ex);
            throw ex;
        }
    }

    // Dynamic DI
    private ScreeningStrategy resolveActiveStrategy() {
        String configured = systemConfigService.getString(KEY_ACTIVE_STRATEGY);
        if (configured == null) configured = "basic";
        String beanName = configured.toLowerCase() + "ScreeningStrategy";
        ScreeningStrategy strategy = strategyMap.get(beanName);
        if (strategy == null) {
            log.warn("Unknown strategy '{}' — falling back to BASIC", configured);
            strategy = strategyMap.get("basicScreeningStrategy");
        }
        return strategy;
    }

    // Persistence helpers
    private ScreeningResult createPendingResult(Candidate candidate, String strategyName) {
        return screeningResultRepository.save(
                ScreeningResult.builder()
                        .candidate(candidate)
                        .strategyUsed(strategyName)
                        .riskScore(0.0)
                        .riskLevel(RiskLevel.LOW)
                        .status(ScreeningStatus.PENDING)
                        // Threshold snapshots captured NOW — immutable from this point
                        .mediumThresholdSnapshot(riskScoringEngine.getMediumThreshold())
                        .highThresholdSnapshot(riskScoringEngine.getHighThreshold())
                        .fuzzyThresholdSnapshot(riskScoringEngine.getFuzzyThreshold())
                        .build()
        );
    }

    /**
     * Converts all MatchDetailDtos -> ScreeningMatch entities via mapper,
     * then wires the watchlistEntry proxy and attaches to the result.
     */
    private void persistMatches(ScreeningResultDto resultDto, ScreeningResult result) {
        if (resultDto.getMatches() == null || resultDto.getMatches().isEmpty()) return;

        for (MatchDetailDto dto : resultDto.getMatches()) {
            // Mapper handles all field + snapshot mapping
            ScreeningMatch match = screeningMapper.toScreeningMatchEntity(dto);

            // watchlistEntry is ignored by mapper — set manually via proxy (no extra SELECT)
            match.setWatchlistEntry(
                    watchlistEntryRepository.getReferenceById(dto.getWatchlistEntryId()));

            // addMatch() wires screeningResult + adds to list (bidirectional sync)
            result.addMatch(match);
        }
    }
}