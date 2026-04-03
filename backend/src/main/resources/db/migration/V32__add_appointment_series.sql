-- PA-33: Recurring Appointments
-- Adds appointment_series table and extends appointment / appointment_audit tables.

-- New series table
CREATE TABLE appointment_series (
    id                     BIGSERIAL    PRIMARY KEY,
    recurrence_type        VARCHAR(20)  NOT NULL,
    start_date             DATE         NOT NULL,
    total_occurrences      INT          NOT NULL CHECK (total_occurrences BETWEEN 1 AND 20),
    generated_occurrences  INT          NOT NULL,
    therapist_profile_id   UUID         NOT NULL,
    client_id              UUID         NOT NULL,
    session_type_id        UUID         NOT NULL REFERENCES session_type(id),
    duration_minutes       INT          NOT NULL,
    timezone               VARCHAR(50)  NOT NULL,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_by             UUID         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_series_therapist ON appointment_series(therapist_profile_id);
CREATE INDEX idx_series_client    ON appointment_series(client_id);
CREATE INDEX idx_series_status    ON appointment_series(status) WHERE status != 'CANCELLED';

-- Extend appointment table with series linkage fields
ALTER TABLE appointment
    ADD COLUMN series_id         BIGINT  NULL REFERENCES appointment_series(id),
    ADD COLUMN recurrence_index  INT     NULL,
    ADD COLUMN is_modified       BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_appointment_series ON appointment(series_id) WHERE series_id IS NOT NULL;

-- Extend audit table with series context fields
ALTER TABLE appointment_audit
    ADD COLUMN edit_scope  VARCHAR(20) NULL,
    ADD COLUMN series_id   BIGINT      NULL;

CREATE INDEX idx_audit_series ON appointment_audit(series_id) WHERE series_id IS NOT NULL;
