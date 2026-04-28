package com.onboardguard.shared.common.events;

/**
 * Fired ONLY when a candidate's final document is verified by an Officer,
 * meaning their identity is legally confirmed and they are ready for AML/KYC screening.
 */
public record DocumentVerificationCompletedEvent(
        Long candidateId
) {}