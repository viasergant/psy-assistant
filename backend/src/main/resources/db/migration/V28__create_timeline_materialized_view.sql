-- V28: Client activity timeline materialized view.
-- Aggregates events from appointments, profile changes, and conversion history
-- for fast, unified timeline queries.

-- Timeline materialized view
-- Sources: appointments, client_profile_audit_entry, client conversion event
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
LEFT JOIN users u ON a.created_by = u.username
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

-- 3. Client conversion event (derived from clients table)
SELECT
    c.id AS client_id,
    c.id AS event_id,  -- Use client ID as event ID for conversion
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
LEFT JOIN users u ON c.created_by = u.username
WHERE c.source_lead_id IS NOT NULL;  -- Only clients created from lead conversion

-- Create indexes on materialized view for fast queries
CREATE INDEX idx_timeline_client_timestamp ON client_activity_timeline (client_id, event_timestamp DESC);
CREATE INDEX idx_timeline_event_type ON client_activity_timeline (client_id, event_type);
CREATE INDEX idx_timeline_created_at ON client_activity_timeline (created_at DESC);

-- Function to refresh timeline view (can be called manually or scheduled)
CREATE OR REPLACE FUNCTION refresh_client_timeline()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY client_activity_timeline;
END;
$$;

COMMENT ON MATERIALIZED VIEW client_activity_timeline IS 'Aggregated client activity timeline from appointments, profile changes, and conversion events';
COMMENT ON COLUMN client_activity_timeline.event_type IS 'High-level event category: APPOINTMENT, PROFILE_CHANGE, CONVERSION';
COMMENT ON COLUMN client_activity_timeline.event_subtype IS 'Specific event subtype (e.g., appointment.scheduled, lead.converted)';
COMMENT ON COLUMN client_activity_timeline.event_data IS 'JSONB payload with event-specific details';
