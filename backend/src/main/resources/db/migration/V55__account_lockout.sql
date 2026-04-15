-- PA-18: Account lockout support
--
-- Adds two columns to `users` that track consecutive failed login attempts and
-- the timestamp until which the account is locked out.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until          TIMESTAMPTZ;

COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts since last reset.';
COMMENT ON COLUMN users.locked_until IS 'When not null and in the future, the account is locked until this timestamp.';

CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users (locked_until) WHERE locked_until IS NOT NULL;
