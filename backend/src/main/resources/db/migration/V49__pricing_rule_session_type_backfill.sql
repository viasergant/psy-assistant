-- V49: PA-69 – Backfill session_type_id on therapist_pricing_rule (Phase 2a)
-- Uses the authoritative mapping: service_type.name → session_type.code

UPDATE therapist_pricing_rule tpr
SET session_type_id = (
    SELECT st.id
    FROM session_type st
    WHERE st.code = CASE srv.name
        WHEN 'Individual Session' THEN 'IN_PERSON'
        WHEN 'Group Session'      THEN 'GROUP'
        WHEN 'Intake Assessment'  THEN 'INTAKE'
        WHEN 'Follow-Up'          THEN 'FOLLOW_UP'
        ELSE NULL
    END
)
FROM service_type srv
WHERE tpr.service_type_id = srv.id
  AND tpr.session_type_id IS NULL;
