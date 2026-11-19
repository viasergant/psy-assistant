-- V77: Add org_id to the users table.
-- Done as a separate migration from V78 so that the User entity can be used by
-- RegistrationService, SoloUserInitRunner, and ClinicSetupController without loading
-- the full 24-table backfill in a single transaction.

-- Step 1: add nullable column
ALTER TABLE users ADD COLUMN org_id UUID REFERENCES organizations(id);

-- Step 2: backfill all existing users with the default org
UPDATE users SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;

-- Step 3: enforce NOT NULL
ALTER TABLE users ALTER COLUMN org_id SET NOT NULL;

-- Step 4: performance index
CREATE INDEX idx_users_org_id ON users(org_id);

-- Step 5: replace global UNIQUE(email) with org-scoped composite.
-- PostgreSQL auto-names inline UNIQUE constraints as <table>_<column>_key.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users ADD CONSTRAINT uq_users_org_email UNIQUE (org_id, email);
