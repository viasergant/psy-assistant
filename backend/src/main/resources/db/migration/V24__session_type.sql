-- V24: Session types lookup table

CREATE TABLE IF NOT EXISTS session_type (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed data
INSERT INTO session_type (code, name, description, is_active)
VALUES
    ('IN_PERSON', 'In-Person Session', 'Face-to-face session at therapist''s office', TRUE),
    ('ONLINE', 'Online Session', 'Remote session via video call', TRUE),
    ('INTAKE', 'Initial Intake Session', 'First diagnostic session with new client', TRUE),
    ('FOLLOW_UP', 'Follow-Up Session', 'Regular therapeutic session', TRUE),
    ('GROUP', 'Group Therapy Session', 'Session with multiple clients', TRUE)
ON CONFLICT (code) DO NOTHING;

-- Index for active session types lookup
CREATE INDEX idx_session_type_active ON session_type (is_active) WHERE is_active = TRUE;

-- Comments
COMMENT ON TABLE session_type IS 'Lookup table for appointment session types';
COMMENT ON COLUMN session_type.code IS 'Unique identifier code for programmatic reference';
COMMENT ON COLUMN session_type.name IS 'Display name for UI';
COMMENT ON COLUMN session_type.is_active IS 'Soft delete flag - inactive types hidden in UI';
