-- V33: Add session completion fields (session_notes, actual_end_time)
--
-- This migration adds support for completing sessions with:
-- 1. session_notes: Required clinical notes entered by therapist when completing a session
-- 2. actual_end_time: Optional actual end time if session ran longer/shorter than planned
--
-- Both fields are populated when a session transitions from IN_PROGRESS to COMPLETED status.

ALTER TABLE session_record
    ADD COLUMN session_notes TEXT,
    ADD COLUMN actual_end_time TIME;

-- Add column comments for documentation
COMMENT ON COLUMN session_record.session_notes IS 'Clinical notes entered by therapist when completing the session (required for COMPLETED status)';
COMMENT ON COLUMN session_record.actual_end_time IS 'Actual end time if different from scheduled (optional, populated when session is completed)';
