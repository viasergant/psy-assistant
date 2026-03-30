-- V12: Therapist credentials table.
-- Stores professional credentials with expiry tracking.

CREATE TABLE IF NOT EXISTS therapist_credential (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    credential_type VARCHAR(128) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255)
);

-- Indexes for filtering and expiry checks
CREATE INDEX IF NOT EXISTS idx_therapist_credential_profile_id ON therapist_credential(therapist_profile_id);
CREATE INDEX IF NOT EXISTS idx_therapist_credential_expiry_date ON therapist_credential(expiry_date);
