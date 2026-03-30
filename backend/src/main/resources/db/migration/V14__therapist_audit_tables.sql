-- V14: Therapist profile audit tables.
-- Field-level immutable audit trail for all therapist profile changes.

CREATE TABLE IF NOT EXISTS therapist_profile_audit_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_name VARCHAR(255),
    event_type VARCHAR(64) NOT NULL,
    request_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS therapist_profile_audit_change (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_entry_id UUID NOT NULL REFERENCES therapist_profile_audit_entry(id) ON DELETE CASCADE,
    field_name VARCHAR(128) NOT NULL,
    old_value TEXT,
    new_value TEXT
);

-- Indexes for audit queries
CREATE INDEX IF NOT EXISTS idx_therapist_profile_audit_entry_profile_id_created_at
    ON therapist_profile_audit_entry(therapist_profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_therapist_profile_audit_change_entry_id
    ON therapist_profile_audit_change(audit_entry_id);
