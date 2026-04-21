-- V1: Create user, role, permission tables

CREATE TABLE IF NOT EXISTS app_users (
    id                        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username                  VARCHAR(100) UNIQUE NOT NULL,
    email                     VARCHAR(255) UNIQUE NOT NULL,
    password_hash             VARCHAR(255) NOT NULL,
    full_name                 VARCHAR(255) NOT NULL,
    phone                     VARCHAR(20),
    is_active                 BOOLEAN     NOT NULL DEFAULT TRUE,
    is_locked                 BOOLEAN     NOT NULL DEFAULT FALSE,
    failed_login_count        INTEGER     NOT NULL DEFAULT 0,
    last_login_at             TIMESTAMPTZ,
    password_reset_token      VARCHAR(255),
    password_reset_expires_at TIMESTAMPTZ,
    created_by                UUID        REFERENCES app_users(id),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS roles (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code      VARCHAR(50) UNIQUE NOT NULL,
    role_name      VARCHAR(100) NOT NULL,
    description    TEXT,
    is_system_role BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS permissions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    permission_name VARCHAR(150) NOT NULL,
    module          VARCHAR(50)  NOT NULL,
    description     TEXT
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       UUID        NOT NULL REFERENCES roles(id),
    permission_id UUID        NOT NULL REFERENCES permissions(id),
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by    UUID        NOT NULL REFERENCES app_users(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id     UUID        NOT NULL REFERENCES app_users(id),
    role_id     UUID        NOT NULL REFERENCES roles(id),
    assigned_by UUID        NOT NULL REFERENCES app_users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    PRIMARY KEY (user_id, role_id)
);
