-- V84: Stop exposing appointment notes through client_activity_timeline.
--
-- Root cause: appointment.notes is encrypted at rest (EncryptedTextConverter), but the view
-- copied the raw column into event_data, so the timeline API returned `enc:k1:...` ciphertext
-- and, once decrypted, would have surfaced session-prep notes to every timeline reader.
--
-- Fix: recreate the materialized view without the 'notes' key. Everything else is identical
-- to V71 (a materialized view cannot be altered in place).

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
    COALESCE(u.full_name, u.email) AS actor_name,
    a.created_by AS actor_identifier,
    jsonb_build_object(
        'appointmentId', a.id,
        'therapistId', a.therapist_profile_id,
        'sessionTypeId', a.session_type_id,
        'status', a.status,
        'startTime', a.start_time,
        'durationMinutes', a.duration_minutes,
        'cancellationType', a.cancellation_type,
        'cancellationReason', a.cancellation_reason
    ) AS event_data,
    a.created_at AS created_at
FROM appointment a
LEFT JOIN users u ON a.created_by = u.id::text
WHERE a.status != 'CANCELLED' OR a.status = 'CANCELLED'  -- Include all appointments

UNION ALL

-- 2. Client profile change events
SELECT
    cpae.client_id,
    cpae.id AS event_id,
    'PROFILE_CHANGE' AS event_type,
    cpae.event_type AS event_subtype,
    cpae.created_at AS event_timestamp,
    COALESCE(u.full_name, u.email) AS actor_name,
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
    COALESCE(u.full_name, u.email) AS actor_name,
    c.created_by AS actor_identifier,
    jsonb_build_object(
        'sourceLeadId', c.source_lead_id,
        'clientCode', c.client_code
    ) AS event_data,
    c.created_at AS created_at
FROM clients c
LEFT JOIN users u ON c.created_by = u.id::text
WHERE c.source_lead_id IS NOT NULL

UNION ALL

-- 4. Direct client creation events (no lead conversion)
SELECT
    c.id AS client_id,
    gen_random_uuid() AS event_id,
    'PROFILE_CHANGE' AS event_type,
    'client.created' AS event_subtype,
    c.created_at AS event_timestamp,
    COALESCE(u.full_name, u.email) AS actor_name,
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
LEFT JOIN users u ON c.created_by = u.id::text
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
    COALESCE(u.full_name, u.email) AS actor_name,
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
    AND sr.record_kind = 'INDIVIDUAL'
LEFT JOIN users u ON u.id = aal.changed_by_user_id

UNION ALL

-- 6. GROUP_SESSION — one row per active participant
SELECT
    sp.client_id,
    gen_random_uuid() AS event_id,
    'GROUP_SESSION' AS event_type,
    CASE sr.status
        WHEN 'PENDING'     THEN 'group_session.pending'
        WHEN 'IN_PROGRESS' THEN 'group_session.in_progress'
        WHEN 'COMPLETED'   THEN 'group_session.completed'
        WHEN 'CANCELLED'   THEN 'group_session.cancelled'
    END AS event_subtype,
    (sr.session_date::DATE + sr.scheduled_start_time::TIME)::TIMESTAMPTZ AS event_timestamp,
    COALESCE(u.full_name, u.email) AS actor_name,
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
LEFT JOIN users u ON sr.created_by = u.id::text
WHERE sp.removed_at IS NULL

UNION ALL

-- 7. GROUP_ATTENDANCE_OUTCOME — per-client group attendance outcome
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
    COALESCE(u.full_name, u.email) AS actor_name,
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

-- Recreate indexes
CREATE INDEX idx_timeline_client_timestamp ON client_activity_timeline (client_id, event_timestamp DESC);
CREATE INDEX idx_timeline_event_type ON client_activity_timeline (client_id, event_type);
CREATE INDEX idx_timeline_created_at ON client_activity_timeline (created_at DESC);

-- Recreate trigger function (no-op if already exists with same body)
CREATE OR REPLACE FUNCTION trigger_refresh_timeline()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    REFRESH MATERIALIZED VIEW client_activity_timeline;
    RETURN NULL;
END;
$$;

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

DROP TRIGGER IF EXISTS trg_group_session_participant_timeline_refresh ON session_participant;
CREATE TRIGGER trg_group_session_participant_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON session_participant
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

DROP TRIGGER IF EXISTS trg_group_session_attendance_timeline_refresh ON group_session_attendance;
CREATE TRIGGER trg_group_session_attendance_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON group_session_attendance
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();
