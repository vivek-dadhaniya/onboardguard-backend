package com.onboardguard.shared.common.events;

import java.math.BigDecimal;

// Triggered when Screening Engine scores someone > 30
public record HighRiskDetectedEvent(
        Long candidateId,
        Long screeningResultId,
        String severity,
        BigDecimal riskScore
) {
}
