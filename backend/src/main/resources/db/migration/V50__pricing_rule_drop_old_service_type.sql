-- V50: PA-69 – Drop service_type_id from therapist_pricing_rule (Phase 2b)
-- Makes session_type_id NOT NULL (all rows backfilled), drops the legacy FK,
-- and replaces the uniqueness constraint to use session_type_id.

-- Make the new FK mandatory
ALTER TABLE therapist_pricing_rule
    ALTER COLUMN session_type_id SET NOT NULL;

-- Remove the old FK column
ALTER TABLE therapist_pricing_rule
    DROP COLUMN service_type_id;

-- Replace old unique index with one based on the canonical lookup
DROP INDEX IF EXISTS uq_therapist_pricing_rule_effective_date;

ALTER TABLE therapist_pricing_rule
    ADD CONSTRAINT uq_pricing_rule_session_type_effective
        UNIQUE (therapist_profile_id, session_type_id, effective_from);
