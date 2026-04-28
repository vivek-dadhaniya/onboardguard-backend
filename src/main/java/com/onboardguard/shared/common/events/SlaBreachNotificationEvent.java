package com.onboardguard.shared.common.events;

import java.time.Instant;

/**
 * Triggered by the SlaMonitoringJob when new SLA breaches are detected in the system.
 */
public record SlaBreachNotificationEvent(
        int newBreachedAlerts,
        int newBreachedCases,
        Instant timestamp
) {}