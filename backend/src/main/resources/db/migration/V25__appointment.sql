-- V25: Appointment table with conflict detection support

-- Enable btree_gist extension for UUID support in GIST indexes
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS appointment (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id    UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE RESTRICT,
    client_id               UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    session_type_id         UUID NOT NULL REFERENCES session_type(id) ON DELETE RESTRICT,
    
    -- Scheduling fields
    start_time              TIMESTAMPTZ NOT NULL,
    duration_minutes        INTEGER NOT NULL CHECK (duration_minutes > 0 AND duration_minutes <= 480),
    timezone                VARCHAR(64) NOT NULL DEFAULT 'UTC',
    
    -- Status and tracking
    status                  VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    is_conflict_override    BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Cancellation fields
    cancellation_type       VARCHAR(50),
    cancellation_reason     VARCHAR(1000),
    cancelled_at            TIMESTAMPTZ,
    cancelled_by            UUID REFERENCES users(id),
    
    -- Reschedule tracking
    reschedule_reason       VARCHAR(1000),
    original_start_time     TIMESTAMPTZ,
    rescheduled_at          TIMESTAMPTZ,
    rescheduled_by          UUID REFERENCES users(id),
    
    -- Notes and metadata
    notes                   TEXT,
    
    -- Optimistic locking
    version                 BIGINT NOT NULL DEFAULT 0,
    
    -- Audit fields (Spring Data JPA auditing)
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    
    -- Constraints
    CONSTRAINT chk_appointment_duration_increment CHECK (duration_minutes % 15 = 0),
    CONSTRAINT chk_appointment_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_appointment_cancellation_type CHECK (
        cancellation_type IS NULL OR cancellation_type IN ('CLIENT_INITIATED', 'THERAPIST_INITIATED', 'LATE_CANCELLATION')
    ),
    CONSTRAINT chk_appointment_cancelled_fields CHECK (
        (status = 'CANCELLED' AND cancellation_type IS NOT NULL AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
        OR (status != 'CANCELLED' AND cancellation_type IS NULL AND cancelled_at IS NULL AND cancelled_by IS NULL)
    )
);

-- Helper function to compute appointment time range (marked IMMUTABLE for index usage)
CREATE OR REPLACE FUNCTION appointment_time_range(start_time TIMESTAMPTZ, duration_minutes INTEGER)
RETURNS tstzrange
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT tstzrange(start_time, start_time + (duration_minutes * INTERVAL '1 minute'));
$$;

-- CRITICAL: GIST index for O(log n) conflict detection using PostgreSQL tstzrange
-- This index enables fast overlap queries: WHERE appointment_time_range(start_time, duration_minutes) && target_range
CREATE INDEX idx_appointment_conflict_detection ON appointment 
USING GIST (
    therapist_profile_id, 
    appointment_time_range(start_time, duration_minutes)
)
WHERE status != 'CANCELLED';

-- Unique constraint: prevent exact duplicate bookings (same therapist at same time)
-- Partial index excludes cancelled appointments from uniqueness check
CREATE UNIQUE INDEX idx_appointment_unique_slot ON appointment (therapist_profile_id, start_time)
WHERE status != 'CANCELLED';

-- Performance indexes
CREATE INDEX idx_appointment_therapist_date ON appointment (therapist_profile_id, start_time DESC);
CREATE INDEX idx_appointment_client ON appointment (client_id, start_time DESC);
CREATE INDEX idx_appointment_status ON appointment (status);
CREATE INDEX idx_appointment_session_type ON appointment (session_type_id);

-- Comments
COMMENT ON TABLE appointment IS 'Appointment bookings with automatic conflict detection';
COMMENT ON COLUMN appointment.version IS 'Optimistic locking version for concurrent modification detection';
COMMENT ON COLUMN appointment.is_conflict_override IS 'TRUE if created with explicit conflict override permission';
COMMENT ON COLUMN appointment.duration_minutes IS 'Duration in minutes, must be multiple of 15';
COMMENT ON COLUMN appointment.status IS 'Appointment lifecycle status';
COMMENT ON COLUMN appointment.cancellation_type IS 'Who initiated cancellation: CLIENT_INITIATED, THERAPIST_INITIATED, or LATE_CANCELLATION';
COMMENT ON COLUMN appointment.timezone IS 'IANA timezone identifier for start_time interpretation';
COMMENT ON COLUMN appointment.original_start_time IS 'Original start time before reschedule (null if never rescheduled)';
COMMENT ON INDEX idx_appointment_conflict_detection IS 'GIST index for fast overlapping appointment detection using tstzrange';
COMMENT ON INDEX idx_appointment_unique_slot IS 'Ensures no double booking for same therapist at exact same time (excludes cancelled)';
