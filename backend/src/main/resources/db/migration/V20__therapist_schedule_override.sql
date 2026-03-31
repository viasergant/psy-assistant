-- V20: Therapist schedule override table for one-off date exceptions.

CREATE TABLE IF NOT EXISTS therapist_schedule_override (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    override_date        DATE NOT NULL,
    is_available         BOOLEAN NOT NULL DEFAULT false, -- false = unavailable, true = custom hours
    start_time           TIME, -- NULL when is_available = false
    end_time             TIME, -- NULL when is_available = false
    reason               VARCHAR(500), -- Optional explanation for the override
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(255),
    
    CONSTRAINT chk_override_time_order 
        CHECK (is_available = false OR (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)),
    CONSTRAINT chk_override_time_increment 
        CHECK (is_available = false OR 
               (EXTRACT(MINUTE FROM start_time) % 30 = 0 AND EXTRACT(MINUTE FROM end_time) % 30 = 0)),
    CONSTRAINT uq_therapist_override_date 
        UNIQUE (therapist_profile_id, override_date)
);

CREATE INDEX IF NOT EXISTS idx_tso_therapist_id 
    ON therapist_schedule_override(therapist_profile_id);

CREATE INDEX IF NOT EXISTS idx_tso_therapist_date 
    ON therapist_schedule_override(therapist_profile_id, override_date);

COMMENT ON TABLE therapist_schedule_override IS 
    'One-off schedule exceptions for specific dates. Overrides take precedence over recurring weekly schedule.';

COMMENT ON COLUMN therapist_schedule_override.is_available IS 
    'false = entire day unavailable; true = custom hours for this date only';

COMMENT ON COLUMN therapist_schedule_override.reason IS 
    'Human-readable explanation (e.g., "Conference", "Extended hours for catch-up sessions")';
