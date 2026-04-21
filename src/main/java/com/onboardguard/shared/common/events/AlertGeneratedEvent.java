package com.onboardguard.shared.common.events;

public record AlertGeneratedEvent(
        String officerEmail,
        Long alertId,
        String candidateName,
        String severity
) {
}
