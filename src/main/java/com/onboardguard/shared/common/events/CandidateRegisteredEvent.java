package com.onboardguard.shared.common.events;

public record CandidateRegisteredEvent(
        String candidateEmail,
        String candidateName
) {
}
