-- H2-SPECIFIC TEST DATA SCRIPT
-- This file provides test data specifically for H2 in-memory database
-- Named 'test-data.sql' to avoid conflict with main 'data.sql' (PostgreSQL syntax)
-- Default Login Password: password123
-- BCrypt Hash: $2a$10$zGRLkBrIBIKI2bReYnw9dejmVCf4UVarfKEDVuiZ4vFfoYvj3iY6q

-- H2 doesn't support PostgreSQL's ON CONFLICT syntax
-- Use simple INSERT instead (H2 auto-increments ID if email doesn't exist)
DELETE FROM users WHERE email IN ('dev.vivek.dadhaniya@gmail.com', 'vrundachavda112@gmail.com');

INSERT INTO users (
    email,
    full_name,
    phone,
    password_hash,
    role,
    is_active,
    is_locked,
    created_at,
    updated_at,
    version
) VALUES
    ('dev.vivek.dadhaniya@gmail.com', 'Vivek Dadhaniya', '+911005550001', '$2a$10$zGRLkBrIBIKI2bReYnw9dejmVCf4UVarfKEDVuiZ4vFfoYvj3iY6q', 'ROLE_SUPER_ADMIN', true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    ('vrundachavda112@gmail.com', 'Vrunda Chavda', '+911005550002', '$2a$10$zGRLkBrIBIKI2bReYnw9dejmVCf4UVarfKEDVuiZ4vFfoYvj3iY6q', 'ROLE_ADMIN', true, false, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0);

-- 1. Seed Sources
INSERT INTO watchlist_sources (id, code, name, type, credibility_weight, active, created_at, updated_at, version)
VALUES
    (1, 'UN_SC', 'UN Security Council', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (2, 'INTERPOL', 'Interpol Red Notices', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (3, 'CBI_INDIA', 'CBI Wanted List', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (4, 'SEBI_DEBARRED', 'SEBI Debarred Entities', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (5, 'RBI_DEFAULTER', 'RBI Wilful Defaulters', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (6, 'NEWS_MEDIA', 'Global News Media', 'UNVERIFIED', 0.4, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (7, 'INTERNAL_HR', 'Internal HR Blacklist', 'INTERNAL', 0.8, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0);

-- 2. Seed Categories
INSERT INTO watchlist_categories (id, code, name, description, base_risk_score, is_active, created_at, updated_at, version)
VALUES
    (1, 'CRIMINAL', 'Criminal Records', 'Individuals with criminal background or global sanctions', 100, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (2, 'FRAUD', 'Known Fraudsters', 'Financial and cyber fraud offenders', 90, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (3, 'BLACKLIST', 'Blacklisted Entities', 'Vendors or individuals banned due to misconduct', 80, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (4, 'PEP', 'Politically Exposed Persons', 'Politicians and high-risk individuals', 70, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (5, 'PROFESSIONAL_MISCONDUCT', 'Debarred Professionals', 'Banned professionals with revoked licenses', 85, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
    (6, 'EMPLOYMENT_ISSUE', 'Employment Fraud', 'Fake employment or HR scams', 75, true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0);

-- 3. Seed Watchlist Entries
INSERT INTO watchlist_entries
(id, category_id, source_id, primary_name, primary_name_normalized, severity, pan_number, nationality, category_specific_data, is_active, created_at, updated_at, version)
VALUES
(1001, 1, 1, 'Osama Bin Laden', 'OSAMABINLADEN', 'HIGH', NULL, 'Afghan', '{"program":"Al-Qaida"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1002, 2, 6, 'Jordan Belfort', 'JORDANBELFORT', 'HIGH', 'ABCDE1234F', 'American', '{"crime":"Securities Fraud"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1003, 2, 5, 'Vijay Mallya', 'VIJAYMALLYA', 'HIGH', 'AAACV1234A', 'Indian', '{"case":"Bank Fraud"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1004, 2, 5, 'Nirav Modi', 'NIRAVMODI', 'HIGH', 'AAACN2345B', 'Indian', '{"case":"PNB Scam"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1005, 2, 5, 'Mehul Choksi', 'MEHULCHOKSI', 'HIGH', 'AAACM6789D', 'Indian', '{"case":"Bank Fraud"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1006, 1, 7, 'Smit Karbathiya', 'SMITKARBATHIYA', 'HIGH', NULL, 'Indian', '{"type":"Criminal"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1007, 2, 7, 'Divayrajsinh Sindhav', 'DIVAYRAJSINHSINDHAV', 'HIGH', NULL, 'Indian', '{"type":"Cyber Fraud"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1008, 5, 4, 'Neel Chhantbar', 'NEELCHHANTBAR', 'MEDIUM', NULL, 'Indian', '{"status":"Debarred Professional"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1009, 4, 6, 'Vivek Dadhaniya', 'VIVEKDADHANIYA', 'MEDIUM', NULL, 'Indian', '{"role":"PEP"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
(1010, 1, 3, 'Tanmay Jotangia', 'TANMAYJOTANGIA', 'HIGH', NULL, 'Indian', '{"record":"Criminal"}', true, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0);

-- 4. Seed Aliases (comment out since we don't have all requisite entries)
-- INSERT INTO watchlist_aliases (id, entry_id, source_id, alias_name, alias_name_normalized, alias_type, created_at, updated_at, version)
-- VALUES
--     (1, 1002, 6, 'The Wolf of Wall Street', 'THEWOLFOFWALLSTREET', 'AKA', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0),
--     (2, 1018, 2, 'Abu S', 'ABUS', 'SHORT_NAME', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 0);

