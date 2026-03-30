-- V13: Therapist pricing rules table.
-- Stores service-specific pricing with effective date tracking.

CREATE TABLE IF NOT EXISTS therapist_pricing_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    service_type_id UUID NOT NULL REFERENCES service_type(id) ON DELETE RESTRICT,
    rate NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    effective_from DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255)
);

-- Indexes for filtering and ordering
CREATE INDEX IF NOT EXISTS idx_therapist_pricing_rule_profile_id ON therapist_pricing_rule(therapist_profile_id);
CREATE INDEX IF NOT EXISTS idx_therapist_pricing_rule_service_type_id ON therapist_pricing_rule(service_type_id);
CREATE INDEX IF NOT EXISTS idx_therapist_pricing_rule_effective_from ON therapist_pricing_rule(effective_from DESC);

-- Valid immutable uniqueness rule: only one pricing rule per therapist, service type,
-- and effective date. Current-date based uniqueness cannot be expressed in a
-- PostgreSQL index predicate because CURRENT_DATE is not immutable.
CREATE UNIQUE INDEX IF NOT EXISTS uq_therapist_pricing_rule_effective_date
    ON therapist_pricing_rule(therapist_profile_id, service_type_id, effective_from);
