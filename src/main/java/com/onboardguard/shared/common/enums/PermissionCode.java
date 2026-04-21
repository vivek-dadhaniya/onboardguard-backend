package com.onboardguard.shared.common.enums;

public enum PermissionCode {

    // User management
    USER_CREATE_ADMIN,
    USER_CREATE_OFFICER,
    USER_VIEW_ALL,
    USER_ACTIVATE_DEACTIVATE,

    // Role management
    ROLE_MANAGE,

    // Watchlist
    WATCHLIST_CREATE,
    WATCHLIST_EDIT,
    WATCHLIST_DELETE_SOFT,
    WATCHLIST_VIEW,
    WATCHLIST_EVIDENCE_VIEW,

    // Screening
    SCREENING_CONFIG_SWITCH,
    SCREENING_RESCREEN,

    // Alerts
    ALERT_VIEW,
    ALERT_CONVERT_TO_CASE,

    // Cases
    CASE_VIEW,
    CASE_MANAGE,
    CASE_ASSIGN_OFFICER,
    CASE_RESOLVE,
    CASE_ESCALATE,

    // Documents
    DOC_VERIFY,

    // Audit & Reports
    AUDIT_LOG_VIEW,
    REPORTS_VIEW,

    // System
    SYSTEM_CONFIG_MANAGE,

    // Candidate self-service
    CANDIDATE_FORM_SUBMIT,
    CANDIDATE_DOC_UPLOAD,
    CANDIDATE_STATUS_VIEW_OWN
}
