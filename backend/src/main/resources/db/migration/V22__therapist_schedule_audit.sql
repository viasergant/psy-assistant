-- V22: Audit tables for schedule change tracking.

CREATE TABLE IF NOT EXISTS therapist_schedule_audit_entry (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    entity_type          VARCHAR(64) NOT NULL, -- 'RECURRING_SCHEDULE', 'OVERRIDE', 'LEAVE'
    entity_id            UUID NOT NULL, -- ID of the modified entity
    actor_user_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_name           VARCHAR(255),
    event_type           VARCHAR(64) NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT'
    request_id           VARCHAR(64),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS therapist_schedule_audit_change (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id    UUID NOT NULL REFERENCES therapist_schedule_audit_entry(id) ON DELETE CASCADE,
    field_name  VARCHAR(128) NOT NULL,
    old_value   TEXT,
    new_value   TEXT
);

CREATE INDEX IF NOT EXISTS idx_tsae_therapist_id 
    ON therapist_schedule_audit_entry(therapist_profile_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tsae_entity 
    ON therapist_schedule_audit_entry(entity_type, entity_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tsac_entry_id 
    ON therapist_schedule_audit_change(entry_id);

COMMENT ON TABLE therapist_schedule_audit_entry IS 
    'Audit log header for all schedule-related changes (recurring, overrides, leave).';

COMMENT ON TABLE therapist_schedule_audit_change IS 
    'Field-level before/after values for schedule audit entries.';

COMMENT ON COLUMN therapist_schedule_audit_entry.entity_type IS 
    'Type of schedule entity modified: RECURRING_SCHEDULE, OVERRIDE, or LEAVE';

COMMENT ON COLUMN therapist_schedule_audit_entry.event_type IS 
    'Action performed: CREATE, UPDATE, DELETE, APPROVE (leave), REJECT (leave)';
