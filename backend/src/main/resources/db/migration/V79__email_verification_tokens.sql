-- V79: Email verification for self-service (saas) registration + generic one-time tokens.

-- Existing users are treated as verified; new saas registrations set this to false.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE one_time_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose     VARCHAR(50)  NOT NULL,
    token_hash  CHAR(64)     NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_one_time_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_one_time_tokens_user_purpose ON one_time_tokens(user_id, purpose);
