package com.onboardguard.shared.common.enums;

public enum RoleCode {
    ROLE_CANDIDATE,
    ROLE_OFFICER_L1,
    ROLE_OFFICER_L2,
    ROLE_ADMIN,
    ROLE_SUPER_ADMIN;

    // Convenience: returns the part after ROLE_ for display
    public String display() {
        return name().replace("ROLE_", "");
    }
}
