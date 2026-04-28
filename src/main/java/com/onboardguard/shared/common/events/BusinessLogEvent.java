package com.onboardguard.shared.common.events;

import lombok.Builder;

/**
 * Represents a business action that occurred in the system.
 * Implemented as a record because Domain Events must be immutable.
 */
@Builder
public record BusinessLogEvent(
        String entityType,  // e.g., "CANDIDATE", "WATCHLIST_ENTRY"
        Long entityId,
        String action,      // e.g., "APPROVED", "ESCALATED"
        String oldStatus,
        String newStatus,
        Long performedBy, // ID of the user who performed the action
        String actorRole,   // e.g., "ROLE_OFFICER_L2"
        String remarks      // e.g., "ID is blurry"
) {}