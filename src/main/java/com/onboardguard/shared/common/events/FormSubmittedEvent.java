package com.onboardguard.shared.common.events;

// Triggered when candidate completes step 4 of onboarding
public record FormSubmittedEvent(
        Long candidateId
) {
}
