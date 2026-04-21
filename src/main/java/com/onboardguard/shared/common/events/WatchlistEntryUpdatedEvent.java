package com.onboardguard.shared.common.events;

// Triggered when Admin adds/edits a watchlist entry
public record WatchlistEntryUpdatedEvent(
        Long entryId
) {
}
