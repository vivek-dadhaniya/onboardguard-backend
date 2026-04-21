package com.onboardguard.shared.common.events;

public record CaseResolvedEvent(
        String candidateEmail,
        String candidateName,
        boolean isCleared,
        String reason
) {
}
