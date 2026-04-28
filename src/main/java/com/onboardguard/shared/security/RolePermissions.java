package com.onboardguard.shared.security;

import com.onboardguard.shared.common.enums.RoleCode;
import java.util.Set;

/**
 * To grant a new permission to a role:
 *   1. Add the permission string constant here
 *   2. Add it to the correct role's Set below
 *   3. Use it in @PreAuthorize("hasAuthority('YOUR_NEW_PERMISSION')")
 *   No DB migration needed. No Flyway file. Deploy once.
 */
public final class RolePermissions {

    private RolePermissions() {}

    // Define every permission as a constant so @PreAuthorize uses the same

    // Candidate permissions
    public static final String CANDIDATE_FORM_SUBMIT       = "CANDIDATE_FORM_SUBMIT";
    public static final String CANDIDATE_DOC_UPLOAD        = "CANDIDATE_DOC_UPLOAD";
    public static final String CANDIDATE_STATUS_VIEW_OWN   = "CANDIDATE_STATUS_VIEW_OWN";

    // Officer L1 permissions (maker — investigates, escalates)
    public static final String ALERT_VIEW                  = "ALERT_VIEW";
    public static final String ALERT_CONVERT_TO_CASE       = "ALERT_CONVERT_TO_CASE";
    public static final String CASE_VIEW                   = "CASE_VIEW";
    public static final String CASE_CREATE                 = "CASE_CREATE";
    public static final String CASE_ADD_NOTE               = "CASE_ADD_NOTE";
    public static final String CASE_UPDATE_STATUS          = "CASE_UPDATE_STATUS";
    public static final String CASE_ESCALATE               = "CASE_ESCALATE";
    public static final String DOC_VERIFY                  = "DOC_VERIFY";
    public static final String WATCHLIST_VIEW              = "WATCHLIST_VIEW";
    public static final String WATCHLIST_EVIDENCE_VIEW     = "WATCHLIST_EVIDENCE_VIEW";

    // Officer L2 permissions (checker — resolves, overrides)
    // L2 has everything L1 has, plus resolution rights
    public static final String CASE_RESOLVE                = "CASE_RESOLVE";

    // Admin permissions
    public static final String USER_CREATE                 = "USER_CREATE";
    public static final String USER_VIEW_ALL               = "USER_VIEW_ALL";
    public static final String USER_ACTIVATE_DEACTIVATE    = "USER_ACTIVATE_DEACTIVATE";
    public static final String WATCHLIST_CREATE            = "WATCHLIST_CREATE";
    public static final String WATCHLIST_EDIT              = "WATCHLIST_EDIT";
    public static final String WATCHLIST_SOFT_DELETE       = "WATCHLIST_SOFT_DELETE";
    public static final String SCREENING_CONFIG_SWITCH     = "SCREENING_CONFIG_SWITCH";
    public static final String SCREENING_RESCREEN          = "SCREENING_RESCREEN";
    public static final String CASE_ASSIGN_OFFICER         = "CASE_ASSIGN_OFFICER";
    public static final String REPORTS_VIEW                = "REPORTS_VIEW";
    public static final String AUDIT_LOG_VIEW              = "AUDIT_LOG_VIEW";
    public static final String SYSTEM_CONFIG_MANAGE        = "SYSTEM_CONFIG_MANAGE";
    public static final String APPROVAL_CREATE             = "APPROVAL_CREATE";  // maker

    // Super Admin permissions
    public static final String APPROVAL_APPROVE_REJECT     = "APPROVAL_APPROVE_REJECT"; // checker
    public static final String APPROVAL_BYPASS             = "APPROVAL_BYPASS";          // emergency
    public static final String ROLE_MANAGE                 = "ROLE_MANAGE";

    // Role -> permission mapping
    public static Set<String> getPermissions(RoleCode role) {
        return switch (role) {

            case ROLE_CANDIDATE -> Set.of(
                    CANDIDATE_FORM_SUBMIT,
                    CANDIDATE_DOC_UPLOAD,
                    CANDIDATE_STATUS_VIEW_OWN
            );

            case ROLE_OFFICER_L1 -> Set.of(
                    // Alert & case management — can investigate, cannot resolve
                    ALERT_VIEW,
                    ALERT_CONVERT_TO_CASE,
                    CASE_VIEW,
                    CASE_CREATE,
                    CASE_ADD_NOTE,
                    CASE_UPDATE_STATUS,
                    CASE_ESCALATE,          // <- L1 can escalate
                    DOC_VERIFY,
                    WATCHLIST_VIEW,
                    WATCHLIST_EVIDENCE_VIEW
                    // CASE_RESOLVE intentionally excluded — L1 cannot resolve
            );

            case ROLE_OFFICER_L2 -> Set.of(
                    // Everything L1 has, plus the resolution right
                    ALERT_VIEW,
                    ALERT_CONVERT_TO_CASE,
                    CASE_VIEW,
                    CASE_CREATE,
                    CASE_ADD_NOTE,
                    CASE_UPDATE_STATUS,
                    CASE_ESCALATE,
                    CASE_RESOLVE,           // <- L2 can resolve (the whole point)
                    DOC_VERIFY,
                    WATCHLIST_VIEW,
                    WATCHLIST_EVIDENCE_VIEW
            );

            case ROLE_ADMIN -> Set.of(
                    // All officer rights plus admin-only actions
                    ALERT_VIEW,
                    ALERT_CONVERT_TO_CASE,
                    CASE_VIEW,
                    CASE_CREATE,
                    CASE_ADD_NOTE,
                    CASE_UPDATE_STATUS,
                    CASE_ESCALATE,
                    CASE_RESOLVE,           // admins can also resolve escalated cases
                    CASE_ASSIGN_OFFICER,
                    DOC_VERIFY,
                    WATCHLIST_VIEW,
                    WATCHLIST_EVIDENCE_VIEW,
                    WATCHLIST_CREATE,
                    WATCHLIST_EDIT,
                    WATCHLIST_SOFT_DELETE,
                    USER_CREATE,
                    USER_VIEW_ALL,
                    USER_ACTIVATE_DEACTIVATE,
                    SCREENING_CONFIG_SWITCH,
                    SCREENING_RESCREEN,
                    REPORTS_VIEW,
                    AUDIT_LOG_VIEW,
                    SYSTEM_CONFIG_MANAGE,
                    APPROVAL_CREATE         // admin is the MAKER in config workflow
                    // APPROVAL_APPROVE_REJECT excluded — admin cannot approve own changes
                    // APPROVAL_BYPASS excluded — only super admin gets emergency bypass
            );

            case ROLE_SUPER_ADMIN -> Set.of(
                    // Everything admin has, plus checker rights and emergency bypass
                    ALERT_VIEW, ALERT_CONVERT_TO_CASE,
                    CASE_VIEW, CASE_CREATE, CASE_ADD_NOTE,
                    CASE_UPDATE_STATUS, CASE_ESCALATE, CASE_RESOLVE,
                    CASE_ASSIGN_OFFICER, DOC_VERIFY,
                    WATCHLIST_VIEW, WATCHLIST_EVIDENCE_VIEW,
                    WATCHLIST_CREATE, WATCHLIST_EDIT, WATCHLIST_SOFT_DELETE,
                    USER_CREATE, USER_VIEW_ALL, USER_ACTIVATE_DEACTIVATE,
                    SCREENING_CONFIG_SWITCH, SCREENING_RESCREEN,
                    REPORTS_VIEW, AUDIT_LOG_VIEW,
                    SYSTEM_CONFIG_MANAGE,
                    APPROVAL_CREATE,
                    APPROVAL_APPROVE_REJECT,    // <- super admin is the CHECKER
                    APPROVAL_BYPASS,            // <- emergency bypass only for super admin
                    ROLE_MANAGE
            );
        };
    }

    public static boolean isStaff(RoleCode role) {
        return role != RoleCode.ROLE_CANDIDATE;
    }
}