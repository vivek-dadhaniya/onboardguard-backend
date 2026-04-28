package com.onboardguard.screening.enums;

public enum ScreeningStatus {
    PENDING,         // Form submitted, screening not yet started
    IN_PROGRESS,     // Engine is running
    CLEAR,           // Score ≤ threshold — candidate cleared
    FLAGGED,         // Score > threshold — alert generated
    REVIEW_NEEDED,   // Officer requested re-screening / more info
    RE_SCREENED      // Admin manually triggered re-screening
}