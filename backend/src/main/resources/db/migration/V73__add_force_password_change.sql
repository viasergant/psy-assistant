-- Architecture D1.2: force_password_change concept is implemented as must_change_password (added in V18).
-- This migration is a safe no-op that documents the column's architectural role and confirms its presence.
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN users.must_change_password IS 'When true, user must change password before accessing the application. Set to true on solo first-boot and clinic first-admin creation.';

