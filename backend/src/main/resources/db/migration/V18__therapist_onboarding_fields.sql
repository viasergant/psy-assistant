-- V18: Add fields for therapist onboarding workflow.
-- Adds must_change_password to users table for first-login security,
-- and profile_completion_status to therapist_profile for tracking onboarding progress.

-- Add must_change_password to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for efficient filtering of users requiring password change
CREATE INDEX IF NOT EXISTS idx_users_must_change_password 
    ON users(must_change_password) 
    WHERE must_change_password = TRUE;

-- Add profile_completion_status to therapist_profile table
ALTER TABLE therapist_profile ADD COLUMN IF NOT EXISTS profile_completion_status VARCHAR(20) NOT NULL DEFAULT 'INCOMPLETE';

-- Add check constraint to ensure valid status values
ALTER TABLE therapist_profile ADD CONSTRAINT IF NOT EXISTS chk_profile_completion_status 
    CHECK (profile_completion_status IN ('INCOMPLETE', 'COMPLETE'));

-- Add index for filtering incomplete profiles
CREATE INDEX IF NOT EXISTS idx_therapist_profile_completion_status 
    ON therapist_profile(profile_completion_status) 
    WHERE profile_completion_status = 'INCOMPLETE';

-- Add comment for documentation
COMMENT ON COLUMN users.must_change_password IS 'Indicates if user must change password on next login (typically set for newly created accounts)';
COMMENT ON COLUMN therapist_profile.profile_completion_status IS 'Tracks therapist profile onboarding completion: INCOMPLETE or COMPLETE';
