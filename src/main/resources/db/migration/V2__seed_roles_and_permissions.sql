-- V2: Seed system roles and all permissions, then assign permissions to roles

-- ============================================================
-- 1. Seed system roles (is_system_role = TRUE — cannot be deleted)
-- ============================================================
INSERT INTO roles (id, role_code, role_name, description, is_system_role)
VALUES
    (gen_random_uuid(), 'SUPER_ADMIN',        'Super Administrator', 'Platform owner with full access. Created via DB seed only.',          TRUE),
    (gen_random_uuid(), 'ADMIN',              'Administrator',       'HR Administrator / System Admin. Created by SUPER_ADMIN or ADMIN.',   TRUE),
    (gen_random_uuid(), 'COMPLIANCE_OFFICER', 'Compliance Officer',  'HR Compliance Executive. Created only by ADMIN.',                     TRUE),
    (gen_random_uuid(), 'CANDIDATE',          'Candidate',           'New employee / vendor / contractor. Self-registers on public page.',  TRUE)
ON CONFLICT (role_code) DO NOTHING;

-- ============================================================
-- 2. Seed all permissions
-- ============================================================
INSERT INTO permissions (id, permission_code, permission_name, module)
VALUES
    -- User management
    (gen_random_uuid(), 'USER_CREATE_ADMIN',          'Create Administrator',           'USER'),
    (gen_random_uuid(), 'USER_CREATE_OFFICER',        'Create Compliance Officer',      'USER'),
    (gen_random_uuid(), 'USER_VIEW_ALL',              'View All Users',                 'USER'),
    (gen_random_uuid(), 'USER_ACTIVATE_DEACTIVATE',   'Activate / Deactivate User',     'USER'),
    -- Role management
    (gen_random_uuid(), 'ROLE_MANAGE',                'Manage Roles',                   'ROLE'),
    -- Watchlist
    (gen_random_uuid(), 'WATCHLIST_CREATE',           'Create Watchlist Entry',         'WATCHLIST'),
    (gen_random_uuid(), 'WATCHLIST_EDIT',             'Edit Watchlist Entry',           'WATCHLIST'),
    (gen_random_uuid(), 'WATCHLIST_DELETE_SOFT',      'Soft-Delete Watchlist Entry',    'WATCHLIST'),
    (gen_random_uuid(), 'WATCHLIST_VIEW',             'View Watchlist',                 'WATCHLIST'),
    (gen_random_uuid(), 'WATCHLIST_EVIDENCE_VIEW',    'View Watchlist Evidence',        'WATCHLIST'),
    -- Screening
    (gen_random_uuid(), 'SCREENING_CONFIG_SWITCH',    'Switch Screening Configuration', 'SCREENING'),
    (gen_random_uuid(), 'SCREENING_RESCREEN',         'Re-screen Subject',              'SCREENING'),
    -- Alerts
    (gen_random_uuid(), 'ALERT_VIEW',                 'View Alerts',                    'ALERT'),
    (gen_random_uuid(), 'ALERT_CONVERT_TO_CASE',      'Convert Alert to Case',          'ALERT'),
    -- Cases
    (gen_random_uuid(), 'CASE_VIEW',                  'View Cases',                     'CASE'),
    (gen_random_uuid(), 'CASE_MANAGE',                'Manage Cases',                   'CASE'),
    (gen_random_uuid(), 'CASE_ASSIGN_OFFICER',        'Assign Officer to Case',         'CASE'),
    (gen_random_uuid(), 'CASE_RESOLVE',               'Resolve Case',                   'CASE'),
    (gen_random_uuid(), 'CASE_ESCALATE',              'Escalate Case',                  'CASE'),
    -- Documents
    (gen_random_uuid(), 'DOC_VERIFY',                 'Verify Document',                'DOCUMENT'),
    -- Audit & Reports
    (gen_random_uuid(), 'AUDIT_LOG_VIEW',             'View Audit Logs',                'AUDIT'),
    (gen_random_uuid(), 'REPORTS_VIEW',               'View Reports',                   'REPORT'),
    -- System
    (gen_random_uuid(), 'SYSTEM_CONFIG_MANAGE',       'Manage System Configuration',    'SYSTEM'),
    -- Candidate self-service
    (gen_random_uuid(), 'CANDIDATE_FORM_SUBMIT',      'Submit Candidate Form',          'CANDIDATE'),
    (gen_random_uuid(), 'CANDIDATE_DOC_UPLOAD',       'Upload Candidate Documents',     'CANDIDATE'),
    (gen_random_uuid(), 'CANDIDATE_STATUS_VIEW_OWN',  'View Own Onboarding Status',     'CANDIDATE')
ON CONFLICT (permission_code) DO NOTHING;

-- ============================================================
-- 3. Assign permissions to roles
--    Note: role_permissions.granted_by requires a valid app_users row.
--    For seed data we use a sentinel UUID that represents the system.
--    The super-admin user created later should carry this UUID, OR
--    we temporarily allow NULL by using a deferred constraint approach.
--    Here we insert a system bootstrap user first, then clean up if needed.
-- ============================================================

-- Bootstrap system user (used only as the grant actor for seed data)
INSERT INTO app_users (id, username, email, password_hash, full_name, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'system',
    'system@onboardguard.internal',
    '$2a$12$PLACEHOLDER_SYSTEM_HASH_NOT_FOR_LOGIN',
    'System',
    FALSE
)
ON CONFLICT (id) DO NOTHING;

-- Helper: resolve role UUIDs by code into a local variable via a WITH clause,
-- then insert into role_permissions.

WITH
  r AS (
    SELECT id, role_code FROM roles WHERE role_code IN ('SUPER_ADMIN','ADMIN','COMPLIANCE_OFFICER','CANDIDATE')
  ),
  p AS (
    SELECT id, permission_code FROM permissions
  ),
  system_user AS (
    SELECT id FROM app_users WHERE id = '00000000-0000-0000-0000-000000000001'
  ),
  grants (role_code, permission_code) AS (
    VALUES
      -- SUPER_ADMIN gets everything
      ('SUPER_ADMIN', 'USER_CREATE_ADMIN'),
      ('SUPER_ADMIN', 'USER_CREATE_OFFICER'),
      ('SUPER_ADMIN', 'USER_VIEW_ALL'),
      ('SUPER_ADMIN', 'USER_ACTIVATE_DEACTIVATE'),
      ('SUPER_ADMIN', 'ROLE_MANAGE'),
      ('SUPER_ADMIN', 'WATCHLIST_CREATE'),
      ('SUPER_ADMIN', 'WATCHLIST_EDIT'),
      ('SUPER_ADMIN', 'WATCHLIST_DELETE_SOFT'),
      ('SUPER_ADMIN', 'WATCHLIST_VIEW'),
      ('SUPER_ADMIN', 'WATCHLIST_EVIDENCE_VIEW'),
      ('SUPER_ADMIN', 'SCREENING_CONFIG_SWITCH'),
      ('SUPER_ADMIN', 'SCREENING_RESCREEN'),
      ('SUPER_ADMIN', 'ALERT_VIEW'),
      ('SUPER_ADMIN', 'ALERT_CONVERT_TO_CASE'),
      ('SUPER_ADMIN', 'CASE_VIEW'),
      ('SUPER_ADMIN', 'CASE_MANAGE'),
      ('SUPER_ADMIN', 'CASE_ASSIGN_OFFICER'),
      ('SUPER_ADMIN', 'CASE_RESOLVE'),
      ('SUPER_ADMIN', 'CASE_ESCALATE'),
      ('SUPER_ADMIN', 'DOC_VERIFY'),
      ('SUPER_ADMIN', 'AUDIT_LOG_VIEW'),
      ('SUPER_ADMIN', 'REPORTS_VIEW'),
      ('SUPER_ADMIN', 'SYSTEM_CONFIG_MANAGE'),
      ('SUPER_ADMIN', 'CANDIDATE_FORM_SUBMIT'),
      ('SUPER_ADMIN', 'CANDIDATE_DOC_UPLOAD'),
      ('SUPER_ADMIN', 'CANDIDATE_STATUS_VIEW_OWN'),
      -- ADMIN (all except ROLE_MANAGE and candidate self-service)
      ('ADMIN', 'USER_CREATE_ADMIN'),
      ('ADMIN', 'USER_CREATE_OFFICER'),
      ('ADMIN', 'USER_VIEW_ALL'),
      ('ADMIN', 'USER_ACTIVATE_DEACTIVATE'),
      ('ADMIN', 'WATCHLIST_CREATE'),
      ('ADMIN', 'WATCHLIST_EDIT'),
      ('ADMIN', 'WATCHLIST_DELETE_SOFT'),
      ('ADMIN', 'WATCHLIST_VIEW'),
      ('ADMIN', 'WATCHLIST_EVIDENCE_VIEW'),
      ('ADMIN', 'SCREENING_CONFIG_SWITCH'),
      ('ADMIN', 'SCREENING_RESCREEN'),
      ('ADMIN', 'ALERT_VIEW'),
      ('ADMIN', 'ALERT_CONVERT_TO_CASE'),
      ('ADMIN', 'CASE_VIEW'),
      ('ADMIN', 'CASE_MANAGE'),
      ('ADMIN', 'CASE_ASSIGN_OFFICER'),
      ('ADMIN', 'CASE_RESOLVE'),
      ('ADMIN', 'CASE_ESCALATE'),
      ('ADMIN', 'DOC_VERIFY'),
      ('ADMIN', 'AUDIT_LOG_VIEW'),
      ('ADMIN', 'REPORTS_VIEW'),
      ('ADMIN', 'SYSTEM_CONFIG_MANAGE'),
      -- COMPLIANCE_OFFICER
      ('COMPLIANCE_OFFICER', 'WATCHLIST_VIEW'),
      ('COMPLIANCE_OFFICER', 'WATCHLIST_EVIDENCE_VIEW'),
      ('COMPLIANCE_OFFICER', 'ALERT_VIEW'),
      ('COMPLIANCE_OFFICER', 'ALERT_CONVERT_TO_CASE'),
      ('COMPLIANCE_OFFICER', 'CASE_VIEW'),
      ('COMPLIANCE_OFFICER', 'CASE_MANAGE'),
      ('COMPLIANCE_OFFICER', 'CASE_RESOLVE'),
      ('COMPLIANCE_OFFICER', 'CASE_ESCALATE'),
      ('COMPLIANCE_OFFICER', 'DOC_VERIFY'),
      -- CANDIDATE
      ('CANDIDATE', 'CANDIDATE_FORM_SUBMIT'),
      ('CANDIDATE', 'CANDIDATE_DOC_UPLOAD'),
      ('CANDIDATE', 'CANDIDATE_STATUS_VIEW_OWN')
  )
INSERT INTO role_permissions (role_id, permission_id, granted_by)
SELECT r.id, p.id, system_user.id
FROM grants g
JOIN r ON r.role_code = g.role_code
JOIN p ON p.permission_code = g.permission_code
CROSS JOIN system_user
ON CONFLICT (role_id, permission_id) DO NOTHING;
