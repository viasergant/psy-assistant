-- PA-41: Add attendance outcome tracking to session records
-- flyway:noTransaction is not needed as CREATE TYPE in a new migration is fine
-- Note: ALTER TYPE ... ADD VALUE cannot run in a transaction, but initial CREATE TYPE can.

CREATE TYPE attendance_outcome_type AS ENUM (
    'ATTENDED',
    'NO_SHOW',
    'LATE_CANCELLATION',
    'CANCELLED',
    'THERAPIST_CANCELLATION'
);

ALTER TABLE session_record
    ADD COLUMN attendance_outcome attendance_outcome_type,
    ADD COLUMN cancelled_at        TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancellation_initiator_id UUID REFERENCES users (id);
