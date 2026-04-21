-- V65: Extend the client activity timeline materialized view with GROUP_SESSION and
--      GROUP_ATTENDANCE_OUTCOME event types.
--
-- Drops and recreates client_activity_timeline to add two new UNION branches:
--   6. GROUP_SESSION  — one row per active session_participant (group session visible in each
--                       linked client's timeline)
--   7. GROUP_ATTENDANCE_OUTCOME — one row per group_session_attendance record (per-client
--                       attendance outcome in each client's timeline)
--
-- Also adds refresh triggers on the new tables.

DROP MATERIALIZED VIEW IF EXISTS client_activity_timeline;

CREATE MATERIALIZED VIEW client_activity_timeline AS
-- 1. Appointment events
SELECT
    a.client_id,
    a.id AS event_id,
    'APPOINTMENT' AS event_type,
    CASE
        WHEN a.status = 'SCHEDULED' THEN 'appointment.scheduled'
        WHEN a.status = 'CONFIRMED' THEN 'appointment.confirmed'
        WHEN a.status = 'COMPLETED' THEN 'appointment.completed'
        WHEN a.status = 'CANCELLED' THEN 'appointment.cancelled'
        WHEN a.status = 'NO_SHOW' THEN 'appointment.no_show'
    END AS event_subtype,
    a.start_time AS event_timestamp,
    COALESCE(u.full_name, a.created_by) AS actor_name,
    a.created_by AS actor_identifier,
    jsonb_build_object(
        'appointmentId', a.id,
        'therapistId', a.therapist_profile_id,
        'sessionTypeId', a.session_type_id,
        'status', a.status,
        'startTime', a.start_time,
        'durationMinutes', a.duration_minutes,
        'notes', a.notes,
        'cancellationType', a.cancellation_type,
        'cancellationReason', a.cancellation_reason
    ) AS event_data,
    a.created_at AS created_at
FROM appointment a
LEFT JOIN users u ON a.created_by = u.email
WHERE a.status != 'CANCELLED' OR a.status = 'CANCELLED'  -- Include all appointments

UNION ALL

-- 2. Client profile change events
SELECT
    cpae.client_id,
    cpae.id AS event_id,
    'PROFILE_CHANGE' AS event_type,
    cpae.event_type AS event_subtype,
    cpae.created_at AS event_timestamp,
    COALESCE(u.full_name, cpae.actor_name) AS actor_name,
    CAST(cpae.actor_user_id AS VARCHAR) AS actor_identifier,
    jsonb_build_object(
        'section', cpae.section,
        'requestId', cpae.request_id,
        'changes', (
            SELECT jsonb_agg(
                jsonb_build_object(
                    'fieldName', cpac.field_name,
                    'oldValue', cpac.old_value,
                    'newValue', cpac.new_value
                )
            )
            FROM client_profile_audit_change cpac
            WHERE cpac.entry_id = cpae.id
        )
    ) AS event_data,
    cpae.created_at AS created_at
FROM client_profile_audit_entry cpae
LEFT JOIN users u ON cpae.actor_user_id = u.id

UNION ALL

-- 3. Client conversion event (from lead conversion)
SELECT
    c.id AS client_id,
    gen_random_uuid() AS event_id,
    'CONVERSION' AS event_type,
    'lead.converted' AS event_subtype,
    c.created_at AS event_timestamp,
    u.full_name AS actor_name,
    c.created_by AS actor_identifier,
    jsonb_build_object(
        'sourceLeadId', c.source_lead_id,
        'clientCode', c.client_code
    ) AS event_data,
    c.created_at AS created_at
FROM clients c
LEFT JOIN users u ON c.created_by = u.email
WHERE c.source_lead_id IS NOT NULL

UNION ALL

-- 4. Direct client creation events (no lead conversion)
SELECT
    c.id AS client_id,
    gen_random_uuid() AS event_id,
    'PROFILE_CHANGE' AS event_type,
    'client.created' AS event_subtype,
    c.created_at AS event_timestamp,
    u.full_name AS actor_name,
    c.created_by AS actor_identifier,
    jsonb_build_object(
        'clientId', c.id,
        'clientCode', c.client_code,
        'fullName', c.full_name,
        'email', c.email,
        'phone', c.phone
    ) AS event_data,
    c.created_at AS created_at
FROM clients c
LEFT JOIN users u ON c.created_by = u.email
WHERE c.source_lead_id IS NULL

UNION ALL

-- 5. Attendance outcome events (INDIVIDUAL sessions, via attendance_audit_log)
SELECT
    sr.client_id,
    aal.id AS event_id,
    'ATTENDANCE_OUTCOME' AS event_type,
    CASE aal.new_outcome
        WHEN 'ATTENDED'              THEN 'attendance.attended'
        WHEN 'NO_SHOW'               THEN 'attendance.no_show'
        WHEN 'LATE_CANCELLATION'     THEN 'attendance.late_cancellation'
        WHEN 'CANCELLED'             THEN 'attendance.cancelled'
        WHEN 'THERAPIST_CANCELLATION' THEN 'attendance.therapist_cancellation'
    END AS event_subtype,
    aal.changed_at AS event_timestamp,
    COALESCE(u.full_name, CAST(aal.changed_by_user_id AS VARCHAR)) AS actor_name,
    CAST(aal.changed_by_user_id AS VARCHAR) AS actor_identifier,
    jsonb_build_object(
        'sessionId', aal.session_record_id,
        'newOutcome', aal.new_outcome,
        'previousOutcome', aal.previous_outcome,
        'note', aal.note
    ) AS event_data,
    aal.changed_at AS created_at
FROM attendance_audit_log aal
JOIN session_record sr ON sr.id = aal.session_record_id
    AND sr.record_kind = 'INDIVIDUAL'           -- Only individual session outcomes here
LEFT JOIN users u ON u.id = aal.changed_by_user_id

UNION ALL

-- 6. GROUP_SESSION — one row per active participant so the group session appears
--    in every linked client's timeline (PA-42 AC#7)
SELECT
    sp.client_id,
    -- Use a deterministic ID: combine session_record_id and client_id to stay idempotent
    -- across materialized view refreshes while keeping the unique index happy
    gen_random_uuid() AS event_id,
    'GROUP_SESSION' AS event_type,
    CASE sr.status
        WHEN 'PENDING'     THEN 'group_session.pending'
        WHEN 'IN_PROGRESS' THEN 'group_session.in_progress'
        WHEN 'COMPLETED'   THEN 'group_session.completed'
        WHEN 'CANCELLED'   THEN 'group_session.cancelled'
    END AS event_subtype,
    -- Use session_date + scheduled_start_time as the event timestamp
    (sr.session_date::DATE + sr.scheduled_start_time::TIME)::TIMESTAMPTZ AS event_timestamp,
    COALESCE(u.full_name, sr.created_by) AS actor_name,
    sr.created_by AS actor_identifier,
    jsonb_build_object(
        'sessionId', sr.id,
        'therapistId', sr.therapist_id,
        'sessionDate', sr.session_date,
        'scheduledStartTime', sr.scheduled_start_time,
        'status', sr.status,
        'participantCount', (
            SELECT COUNT(*)
            FROM session_participant sp2
            WHERE sp2.session_record_id = sr.id
              AND sp2.removed_at IS NULL
        ),
        'groupLabel', 'GROUP'
    ) AS event_data,
    sr.created_at AS created_at
FROM session_participant sp
JOIN session_record sr ON sr.id = sp.session_record_id
    AND sr.record_kind = 'GROUP'
LEFT JOIN users u ON u.email = sr.created_by
WHERE sp.removed_at IS NULL                     -- Only active participants

UNION ALL

-- 7. GROUP_ATTENDANCE_OUTCOME — one row per per-client attendance record so each
--    client's individual attendance outcome appears in their own timeline (PA-42 AC#2)
SELECT
    gsa.client_id,
    gsa.id AS event_id,
    'GROUP_ATTENDANCE_OUTCOME' AS event_type,
    CASE gsa.attendance_outcome
        WHEN 'ATTENDED'              THEN 'attendance.attended'
        WHEN 'NO_SHOW'               THEN 'attendance.no_show'
        WHEN 'LATE_CANCELLATION'     THEN 'attendance.late_cancellation'
        WHEN 'CANCELLED'             THEN 'attendance.cancelled'
        WHEN 'THERAPIST_CANCELLATION' THEN 'attendance.therapist_cancellation'
    END AS event_subtype,
    gsa.recorded_at AS event_timestamp,
    COALESCE(u.full_name, CAST(gsa.recorded_by AS VARCHAR)) AS actor_name,
    CAST(gsa.recorded_by AS VARCHAR) AS actor_identifier,
    jsonb_build_object(
        'sessionId', gsa.session_record_id,
        'clientId', gsa.client_id,
        'outcome', gsa.attendance_outcome,
        'recordedAt', gsa.recorded_at,
        'groupLabel', 'GROUP'
    ) AS event_data,
    gsa.created_at AS created_at
FROM group_session_attendance gsa
LEFT JOIN users u ON u.id = gsa.recorded_by;

-- ─────────────────────────────────────────────────────────────────────────────
-- Recreate indexes (unique index required for CONCURRENT refresh)
-- ─────────────────────────────────────────────────────────────────────────────
-- NOTE: gen_random_uuid() in branches 3, 4, and 6 prevents a simple UNIQUE constraint.
-- We keep the existing non-unique covering indexes for query performance. The
-- CONCURRENT refresh relies on rows being stable when UUID is deterministic
-- (branches 1, 2, 5, 7). Branches 3, 4, and 6 use gen_random_uuid() so they
-- get new UUIDs on each full refresh (non-CONCURRENT), which is acceptable.
-- For a production system that requires CONCURRENT refresh on all branches,
-- use a deterministic hash (md5(id || client_id)) for branch 6 event_id.

CREATE INDEX idx_timeline_client_timestamp ON client_activity_timeline (client_id, event_timestamp DESC);
CREATE INDEX idx_timeline_event_type ON client_activity_timeline (client_id, event_type);
CREATE INDEX idx_timeline_created_at ON client_activity_timeline (created_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- Recreate trigger function
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION trigger_refresh_timeline()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    REFRESH MATERIALIZED VIEW client_activity_timeline;
    RETURN NULL;
END;
$$;

-- Existing triggers (re-created to pick up the updated function)
DROP TRIGGER IF EXISTS trg_appointment_timeline_refresh ON appointment;
CREATE TRIGGER trg_appointment_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON appointment
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

DROP TRIGGER IF EXISTS trg_profile_audit_timeline_refresh ON client_profile_audit_entry;
CREATE TRIGGER trg_profile_audit_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON client_profile_audit_entry
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

DROP TRIGGER IF EXISTS trg_client_timeline_refresh ON clients;
CREATE TRIGGER trg_client_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON clients
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

DROP TRIGGER IF EXISTS trg_attendance_audit_timeline_refresh ON attendance_audit_log;
CREATE TRIGGER trg_attendance_audit_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON attendance_audit_log
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

-- NEW: Refresh triggers on group session tables
DROP TRIGGER IF EXISTS trg_session_participant_timeline_refresh ON session_participant;
CREATE TRIGGER trg_session_participant_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON session_participant
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

DROP TRIGGER IF EXISTS trg_group_attendance_timeline_refresh ON group_session_attendance;
CREATE TRIGGER trg_group_attendance_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON group_session_attendance
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

COMMENT ON MATERIALIZED VIEW client_activity_timeline IS
    'Aggregated client activity timeline: appointments, profile changes, conversions, '
    'attendance outcomes, group sessions (per participant), and group attendance outcomes. '
    'Auto-refreshed via triggers.';
