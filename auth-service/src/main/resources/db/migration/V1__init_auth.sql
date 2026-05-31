-- =============================================
-- CodePilot AI - Auth Service Schema
-- Version: 1
-- =============================================

-- Enum type for roles
CREATE TYPE user_role AS ENUM ('ADMIN', 'DEVELOPER', 'VIEWER');

-- ===== Users Table =====
CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email           VARCHAR(255) NOT NULL,
                       username        VARCHAR(100) NOT NULL,
                       password_hash   VARCHAR(255) NOT NULL,
                       full_name       VARCHAR(200),
                       avatar_url      VARCHAR(500),
                       is_active       BOOLEAN NOT NULL DEFAULT true,
                       is_verified     BOOLEAN NOT NULL DEFAULT false,
                       created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT uk_users_username UNIQUE (username)
);

-- ===== Workspaces Table =====
CREATE TABLE workspaces (
                            id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name            VARCHAR(200) NOT NULL,
                            slug            VARCHAR(200) NOT NULL,
                            description     TEXT,
                            owner_id        UUID NOT NULL,
                            created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                            updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                            CONSTRAINT uk_workspaces_slug UNIQUE (slug),
                            CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id)
                                REFERENCES users(id) ON DELETE CASCADE
);

-- ===== Memberships Table =====
CREATE TABLE memberships (
                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id         UUID NOT NULL,
                             workspace_id    UUID NOT NULL,
                             role            user_role NOT NULL DEFAULT 'DEVELOPER',
                             joined_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                             CONSTRAINT uk_memberships_user_workspace UNIQUE (user_id, workspace_id),
                             CONSTRAINT fk_memberships_user FOREIGN KEY (user_id)
                                 REFERENCES users(id) ON DELETE CASCADE,
                             CONSTRAINT fk_memberships_workspace FOREIGN KEY (workspace_id)
                                 REFERENCES workspaces(id) ON DELETE CASCADE
);

-- ===== Refresh Tokens Table =====
CREATE TABLE refresh_tokens (
                                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id         UUID NOT NULL,
                                token           VARCHAR(500) NOT NULL,
                                expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked         BOOLEAN NOT NULL DEFAULT false,
                                created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                                CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
                                    REFERENCES users(id) ON DELETE CASCADE
);

-- ===== Indexes =====
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_workspaces_owner ON workspaces(owner_id);
CREATE INDEX idx_workspaces_slug ON workspaces(slug);
CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_workspace ON memberships(workspace_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);