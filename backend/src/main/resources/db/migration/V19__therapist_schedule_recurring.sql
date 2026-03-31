-- V19: Therapist recurring weekly schedule table.

CREATE TABLE IF NOT EXISTS therapist_recurring_schedule (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    day_of_week         INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1=Monday, 7=Sunday
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    timezone            VARCHAR(64) NOT NULL DEFAULT 'UTC', -- IANA timezone identifier
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    
    CONSTRAINT chk_therapist_schedule_time_order CHECK (start_time < end_time),
    CONSTRAINT chk_therapist_schedule_time_increment 
        CHECK (
            EXTRACT(MINUTE FROM start_time) % 30 = 0 
            AND EXTRACT(MINUTE FROM end_time) % 30 = 0
        )
);

CREATE INDEX IF NOT EXISTS idx_trs_therapist_id 
    ON therapist_recurring_schedule(therapist_profile_id);

CREATE INDEX IF NOT EXISTS idx_trs_therapist_day 
    ON therapist_recurring_schedule(therapist_profile_id, day_of_week);

COMMENT ON TABLE therapist_recurring_schedule IS 
    'Defines therapist working hours as recurring weekly patterns. Each row represents availability for one day of the week.';

COMMENT ON COLUMN therapist_recurring_schedule.day_of_week IS 
    '1=Monday, 2=Tuesday, ..., 7=Sunday (ISO-8601 convention)';

COMMENT ON COLUMN therapist_recurring_schedule.timezone IS 
    'IANA timezone identifier (e.g., America/New_York, Europe/London). Used to interpret start_time and end_time.';
