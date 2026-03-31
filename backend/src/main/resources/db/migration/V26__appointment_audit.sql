-- V26: Immutable appointment audit trail

CREATE TABLE IF NOT EXISTS appointment_audit (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id      UUID NOT NULL, -- Foreign key intentionally NOT enforced to preserve audit even if appointment deleted
    
    -- Action metadata
    action_type         VARCHAR(50) NOT NULL,
    action_timestamp    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_user_id       UUID NOT NULL REFERENCES user_account(id),
    actor_name          VARCHAR(255) NOT NULL,
    
    -- Request context
    request_id          VARCHAR(100),
    ip_address          VARCHAR(45), -- IPv6 max length
    user_agent          VARCHAR(500),
    
    -- Change details
    field_name          VARCHAR(100),
    old_value           TEXT,
    new_value           TEXT,
    
    -- Metadata
    notes               TEXT,
    metadata            JSONB, -- Additional context (e.g., conflict override details)
    
    -- Constraints
    CONSTRAINT chk_appointment_audit_action_type CHECK (
        action_type IN (
            'CREATED', 
            'RESCHEDULED', 
            'CANCELLED', 
            'STATUS_CHANGED', 
            'NOTES_UPDATED',
            'CONFLICT_OVERRIDE'
        )
    )
);

-- Indexes for audit queries
CREATE INDEX idx_appointment_audit_appointment ON appointment_audit (appointment_id, action_timestamp DESC);
CREATE INDEX idx_appointment_audit_actor ON appointment_audit (actor_user_id, action_timestamp DESC);
CREATE INDEX idx_appointment_audit_action_type ON appointment_audit (action_type, action_timestamp DESC);
CREATE INDEX idx_appointment_audit_timestamp ON appointment_audit (action_timestamp DESC);

-- JSONB index for efficient metadata queries
CREATE INDEX idx_appointment_audit_metadata ON appointment_audit USING GIN (metadata);

-- Comments
COMMENT ON TABLE appointment_audit IS 'Immutable audit log for all appointment changes (no updates or deletes allowed)';
COMMENT ON COLUMN appointment_audit.action_type IS 'Type of action: CREATED, RESCHEDULED, CANCELLED, STATUS_CHANGED, NOTES_UPDATED, CONFLICT_OVERRIDE';
COMMENT ON COLUMN appointment_audit.actor_user_id IS 'User who performed the action';
COMMENT ON COLUMN appointment_audit.actor_name IS 'Snapshot of actor name at time of action';
COMMENT ON COLUMN appointment_audit.request_id IS 'Correlation ID for tracing related audit entries';
COMMENT ON COLUMN appointment_audit.ip_address IS 'Client IP address (IPv4 or IPv6)';
COMMENT ON COLUMN appointment_audit.field_name IS 'Specific field that changed (for granular tracking)';
COMMENT ON COLUMN appointment_audit.old_value IS 'Value before change (JSON or string)';
COMMENT ON COLUMN appointment_audit.new_value IS 'Value after change (JSON or string)';
COMMENT ON COLUMN appointment_audit.metadata IS 'Additional structured context (e.g., conflicting appointment details)';

-- Revoke DELETE and UPDATE to enforce immutability at database level
-- Only application service account should have INSERT permission
-- Commented out for now as we don't have separate service roles yet:
-- REVOKE UPDATE, DELETE ON appointment_audit FROM PUBLIC;
-- GRANT INSERT ON appointment_audit TO psy_assistant_app;
