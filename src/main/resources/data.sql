-- Description: Seeds the initial system administrators
-- Default Login Password for both accounts is: password123
-- BCrypt Hash used: $2a$10$wT5H5dK6X.t/b.4fL7hQv.uM.X6w9.e/B7/8iR.N9.B/3m.A1.M2.

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
    version      -- Added version column
) VALUES (
             'dev.vivek.dadhaniya@gmail.com',
             'Vivek Dadhaniya',
             '+911005550001',
             '$2a$10$zGRLkBrIBIKI2bReYnw9dejmVCf4UVarfKEDVuiZ4vFfoYvj3iY6q',
             'ROLE_SUPER_ADMIN',
             true,
             false,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             0    -- Initialize version to 0 for Hibernate
         ),
         (
             'vrundachavda112@gmail.com',
             'Vrunda Chavda',
             '+911005550002',
             '$2a$10$zGRLkBrIBIKI2bReYnw9dejmVCf4UVarfKEDVuiZ4vFfoYvj3iY6q',
             'ROLE_ADMIN',
             true,
             false,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             0    -- Initialize version to 0 for Hibernate
         )
    ON CONFLICT (email) DO NOTHING;


-- 1. Seed Sources
INSERT INTO watchlist_sources (id, code, name, type, credibility_weight, active, created_at, updated_at, version)
VALUES
    (1, 'UN_SC', 'UN Security Council', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2, 'INTERPOL', 'Interpol Red Notices', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3, 'CBI_INDIA', 'CBI Wanted List', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4, 'SEBI_DEBARRED', 'SEBI Debarred Entities', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (5, 'RBI_DEFAULTER', 'RBI Wilful Defaulters', 'OFFICIAL', 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (6, 'NEWS_MEDIA', 'Global News Media', 'UNVERIFIED', 0.4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (7, 'INTERNAL_HR', 'Internal HR Blacklist', 'INTERNAL', 0.8, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 2. Seed Categories (Updated to match CategoryCode enum and WatchlistCategory entity)
INSERT INTO watchlist_categories (id, code, name, description, base_risk_score, is_active, created_at, updated_at, version)
VALUES
    (1, 'CRIMINAL', 'Criminal Records', 'Individuals with criminal background or global sanctions', 100, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2, 'FRAUD', 'Known Fraudsters', 'Financial and cyber fraud offenders', 90, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3, 'BLACKLIST', 'Blacklisted Entities', 'Vendors or individuals banned due to misconduct', 80, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4, 'PEP', 'Politically Exposed Persons', 'Politicians and high-risk individuals', 70, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (5, 'PROFESSIONAL_MISCONDUCT', 'Debarred Professionals', 'Banned professionals with revoked licenses', 85, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (6, 'EMPLOYMENT_ISSUE', 'Employment Fraud', 'Fake employment or HR scams', 75, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 3. Seed Watchlist Entries (Added source_id mapping)
INSERT INTO watchlist_entries
(id, category_id, source_id, primary_name, primary_name_normalized, severity, pan_number, nationality, category_specific_data, is_active, created_at, updated_at, version)
VALUES

-- Global / Known
(1001, 1, 1, 'Osama Bin Laden', 'OSAMABINLADEN', 'HIGH', NULL, 'Afghan', '{"program":"Al-Qaida"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1002, 2, 6, 'Jordan Belfort', 'JORDANBELFORT', 'HIGH', 'ABCDE1234F', 'American', '{"crime":"Securities Fraud"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Indian High Profile
(1003, 2, 5, 'Vijay Mallya', 'VIJAYMALLYA', 'HIGH', 'AAACV1234A', 'Indian', '{"case":"Bank Fraud"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1004, 2, 5, 'Nirav Modi', 'NIRAVMODI', 'HIGH', 'AAACN2345B', 'Indian', '{"case":"PNB Scam"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1005, 2, 5, 'Mehul Choksi', 'MEHULCHOKSI', 'HIGH', 'AAACM6789D', 'Indian', '{"case":"Bank Fraud"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- USER PROVIDED NAMES
(1006, 1, 7, 'Smit Karbathiya', 'SMITKARBATHIYA', 'HIGH', NULL, 'Indian', '{"type":"Criminal"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1007, 2, 7, 'Divayrajsinh Sindhav', 'DIVAYRAJSINHSINDHAV', 'HIGH', NULL, 'Indian', '{"type":"Cyber Fraud"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1008, 5, 4, 'Neel Chhantbar', 'NEELCHHANTBAR', 'MEDIUM', NULL, 'Indian', '{"status":"Debarred Professional"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1009, 4, 6, 'Vivek Dadhaniya', 'VIVEKDADHANIYA', 'MEDIUM', NULL, 'Indian', '{"role":"PEP"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1010, 1, 3, 'Tanmay Jotangia', 'TANMAYJOTANGIA', 'HIGH', NULL, 'Indian', '{"record":"Criminal"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1011, 3, 7, 'Rahul Dave', 'RAHULDAVE', 'MEDIUM', NULL, 'Indian', '{"status":"Blacklisted Vendor"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1012, 6, 7, 'Bhavika Chhatbar', 'BHAVIKACHHATBAR', 'MEDIUM', NULL, 'Indian', '{"type":"Employment Fraud"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1013, 4, 6, 'Darshit Dhaduk', 'DARSHITDHADUK', 'LOW', NULL, 'Indian', '{"role":"PEP"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1014, 1, 7, 'Vrunda Chavda', 'VRUNDACHAVDA', 'MEDIUM', NULL, 'Indian', '{"note":"Under investigation"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Additional Indian Data
(1015, 2, 4, 'Rakesh Jhunjhunwala Fraud Case Ref', 'RAKESHJHUNJHUNWALA_REF', 'LOW', NULL, 'Indian', '{"note":"Reference flagged entity"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1016, 1, 3, 'Amit Kumar', 'AMITKUMAR', 'HIGH', NULL, 'Indian', '{"crime":"Kidney Racket"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1017, 2, 3, 'Sachin Waze', 'SACHINWAZE', 'HIGH', NULL, 'Indian', '{"case":"Extortion"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1018, 1, 2, 'Abu Salem', 'ABUSALEM', 'HIGH', NULL, 'Indian', '{"crime":"Organized Crime"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1019, 1, 2, 'Chhota Rajan', 'CHHOTARAJAN', 'HIGH', NULL, 'Indian', '{"crime":"Underworld"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Vendors / Fraud / Risk
(1020, 3, 7, 'Shree Traders Pvt Ltd', 'SHREETRADERS', 'MEDIUM', NULL, 'Indian', '{"issue":"Fake billing"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1021, 6, 7, 'QuickHire Solutions', 'QUICKHIRESOLUTIONS', 'MEDIUM', NULL, 'Indian', '{"issue":"Job Scam"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- PEP
(1022, 4, 6, 'Lalu Prasad Yadav', 'LALUPRASADYADAV', 'MEDIUM', NULL, 'Indian', '{"position":"Politician"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1023, 4, 6, 'Sharad Pawar', 'SHARADPAWAR', 'LOW', NULL, 'Indian', '{"position":"Politician"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Cyber / Fraud
(1024, 2, 3, 'Naresh Goyal Case Ref', 'NARESHGOYALREF', 'HIGH', NULL, 'Indian', '{"case":"Financial Fraud"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1025, 2, 4, 'Ketan Parekh', 'KETANPAREKH', 'HIGH', NULL, 'Indian', '{"case":"Stock Market Scam"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

-- Misc Additional
(1026, 1, 3, 'Ravi Pujari', 'RAVIPUJARI', 'HIGH', NULL, 'Indian', '{"crime":"Extortion"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1027, 5, 4, 'Suresh Accountant', 'SURESHACCOUNTANT', 'MEDIUM', NULL, 'Indian', '{"status":"License Revoked"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1028, 3, 7, 'Global Infra Vendor', 'GLOBALINFRAVENDOR', 'LOW', NULL, 'Indian', '{"issue":"Contract Violation"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1029, 6, 7, 'JobFast India', 'JOBFASTINDIA', 'MEDIUM', NULL, 'Indian', '{"issue":"Recruitment Scam"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(1030, 1, 3, 'Deepak Boxer', 'DEEPAKBOXER', 'HIGH', NULL, 'Indian', '{"crime":"Organized Crime"}', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 4. Seed Aliases (Added source_id)
INSERT INTO watchlist_aliases (id, entry_id, source_id, alias_name, alias_name_normalized, alias_type, created_at, updated_at, version)
VALUES
    (1, 1002, 6, 'The Wolf of Wall Street', 'THEWOLFOFWALLSTREET', 'AKA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2, 1018, 2, 'Abu S', 'ABUS', 'SHORT_NAME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3, 1019, 2, 'Rajan N', 'RAJANN', 'SHORT_NAME', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 5. Advance sequences so manual IDs don't clash with future auto-inserts
SELECT setval(pg_get_serial_sequence('watchlist_entries', 'id'), coalesce(max(id), 1)) FROM watchlist_entries;
SELECT setval(pg_get_serial_sequence('watchlist_sources', 'id'), coalesce(max(id), 1)) FROM watchlist_sources;
SELECT setval(pg_get_serial_sequence('watchlist_categories', 'id'), coalesce(max(id), 1)) FROM watchlist_categories;