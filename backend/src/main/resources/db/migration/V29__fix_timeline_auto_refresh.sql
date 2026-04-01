-- V29: Fix timeline materialized view to auto-refresh and include direct client creation events.
-- This addresses two issues:
-- 1. Materialized view wasn't being refreshed automatically
-- 2. Clients created directly (not from lead conversion) had no timeline entry

-- Drop and recreate materialized view to include direct client creation
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
    gen_random_uuid() AS event_id,  -- Generate unique ID for conversion event
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

-- 4. NEW: Direct client creation event (clients created without lead conversion)
SELECT
    c.id AS client_id,
    gen_random_uuid() AS event_id,  -- Generate unique ID for creation event
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
WHERE c.source_lead_id IS NULL;  -- Only clients created directly (not from conversion)

-- Recreate indexes on materialized view
CREATE UNIQUE INDEX idx_timeline_unique ON client_activity_timeline (event_id);
CREATE INDEX idx_timeline_client_timestamp ON client_activity_timeline (client_id, event_timestamp DESC);
CREATE INDEX idx_timeline_event_type ON client_activity_timeline (client_id, event_type);
CREATE INDEX idx_timeline_created_at ON client_activity_timeline (created_at DESC);

-- Create trigger function to refresh materialized view
CREATE OR REPLACE FUNCTION trigger_refresh_timeline()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    -- Refresh the materialized view concurrently (non-blocking)
    -- This requires the unique index we created above
    REFRESH MATERIALIZED VIEW CONCURRENTLY client_activity_timeline;
    RETURN NULL;
END;
$$;

-- Create triggers on source tables to auto-refresh the materialized view
-- Trigger on appointment table
DROP TRIGGER IF EXISTS trg_appointment_timeline_refresh ON appointment;
CREATE TRIGGER trg_appointment_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON appointment
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

-- Trigger on client_profile_audit_entry table
DROP TRIGGER IF EXISTS trg_profile_audit_timeline_refresh ON client_profile_audit_entry;
CREATE TRIGGER trg_profile_audit_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON client_profile_audit_entry
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

-- Trigger on clients table (for conversions and direct creations)
DROP TRIGGER IF EXISTS trg_client_timeline_refresh ON clients;
CREATE TRIGGER trg_client_timeline_refresh
AFTER INSERT OR UPDATE OR DELETE ON clients
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_timeline();

COMMENT ON MATERIALIZED VIEW client_activity_timeline IS 'Aggregated client activity timeline from appointments, profile changes, conversion, and direct client creation events. Auto-refreshed via triggers.';
COMMENT ON FUNCTION trigger_refresh_timeline() IS 'Automatically refreshes client_activity_timeline materialized view when source data changes';
