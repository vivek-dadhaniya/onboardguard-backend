package com.onboardguard.shared.common.events;

public record DocumentRejectedEvent(
        String candidateEmail,
        String candidateName,
        String documentType,
        String reason
) {
}
