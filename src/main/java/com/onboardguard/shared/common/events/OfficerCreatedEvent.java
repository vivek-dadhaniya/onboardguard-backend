package com.onboardguard.shared.common.events;

public record OfficerCreatedEvent(
        String officerEmail,
        String officerName,
        String plainPassword,   // sent once, never stored
        String role,
        String createdByEmail
) {}