-- V30: Create session_record table for clinical session documentation
--
-- This migration establishes the foundation for session records that are automatically
-- created when appointments transition to terminal or active states.
--
-- Key constraints:
-- 1. One-to-one relationship with appointments (unique constraint on appointment_id)
-- 2. Immutable context fields (client, therapist, date, time, type, duration)
-- 3. Full audit trail via BaseEntity inheritance

CREATE TABLE session_record (
    -- Primary key (inherited from BaseEntity)
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign key to appointment (unique - one session per appointment)
    appointment_id UUID NOT NULL,

    -- Immutable context fields (copied from appointment at creation)
    client_id UUID NOT NULL,
    therapist_id UUID NOT NULL,
    session_date DATE NOT NULL,
    scheduled_start_time TIME NOT NULL,
    session_type_id UUID NOT NULL REFERENCES session_type(id),
    planned_duration INTERVAL NOT NULL,

    -- Mutable status fields
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    cancellation_reason VARCHAR(1000),

    -- Audit fields (inherited from BaseEntity)
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    -- Unique constraint to prevent duplicate sessions for same appointment
    CONSTRAINT uk_session_record_appointment UNIQUE (appointment_id)
);

-- Index for efficient queries by client
CREATE INDEX idx_session_record_client_id ON session_record(client_id);

-- Index for efficient queries by therapist
CREATE INDEX idx_session_record_therapist_id ON session_record(therapist_id);

-- Index for efficient queries by session date
CREATE INDEX idx_session_record_session_date ON session_record(session_date);

-- Index for efficient queries by status
CREATE INDEX idx_session_record_status ON session_record(status);

-- Add column comments for documentation
COMMENT ON TABLE session_record IS 'Clinical session records linked to appointments. Created automatically when appointments reach terminal/active status.';
COMMENT ON COLUMN session_record.appointment_id IS 'Foreign key to appointment (unique - one session per appointment)';
COMMENT ON COLUMN session_record.client_id IS 'Immutable snapshot from appointment at creation';
COMMENT ON COLUMN session_record.therapist_id IS 'Immutable snapshot from appointment at creation';
COMMENT ON COLUMN session_record.session_date IS 'Immutable snapshot from appointment at creation';
COMMENT ON COLUMN session_record.scheduled_start_time IS 'Immutable snapshot from appointment at creation';
COMMENT ON COLUMN session_record.session_type_id IS 'Immutable snapshot from appointment at creation';
COMMENT ON COLUMN session_record.planned_duration IS 'Immutable snapshot from appointment at creation';
COMMENT ON COLUMN session_record.status IS 'Current lifecycle status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN session_record.cancellation_reason IS 'Reason for cancellation (null unless status is CANCELLED)';
