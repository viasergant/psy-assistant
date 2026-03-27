-- V4: User management — add full_name to users and password reset tokens table.

-- Add full_name column (nullable; existing rows will have NULL until updated)
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

-- Password reset tokens
-- token_hash: SHA-256 hex of the raw random token (never stored in plain text)
-- used_at: set when the token is consumed; subsequent use attempts are rejected
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prt_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_prt_user_id    ON password_reset_tokens(user_id);
