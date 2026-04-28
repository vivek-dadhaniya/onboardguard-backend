package com.onboardguard.screening.strategy;

import com.onboardguard.screening.dto.CandidateScreeningData;
import com.onboardguard.screening.dto.ScreeningResultDto;

/**
 * Strategy interface for the screening engine.
 *
 * Two implementations exist:
 *  - BasicScreeningStrategy  -> exact matching only
 *  - AdvancedScreeningStrategy -> initials expansion + fuzzy + alias + multi-field corroboration
 *
 * The active implementation is resolved at runtime from SystemConfig by
 * ScreeningOrchestrationService - this is the Dynamic DI requirement.
 */
public interface ScreeningStrategy {

    /**
     * Run the screening check and return a complete result DTO.
     * The DTO is NOT yet persisted at this point - persistence is handled
     * by ScreeningOrchestrationService to keep strategies stateless.
     */
    ScreeningResultDto screen(CandidateScreeningData candidate);

    /**
     * Short name used to identify this strategy in logs and DB records.
     * e.g. "BASIC" or "ADVANCED"
     */
    String strategyName();
}