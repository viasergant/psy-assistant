-- V48: PA-69 – Add session_type_id FK to therapist_pricing_rule (Phase 1)
-- Adds a new nullable link from therapist pricing rules to the canonical
-- session_type lookup table. Existing rows keep their service_type_id until
-- the Phase 2 backfill migration (V49).

ALTER TABLE therapist_pricing_rule
    ADD COLUMN session_type_id UUID REFERENCES session_type(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_tpr_session_type_id
    ON therapist_pricing_rule(session_type_id);
