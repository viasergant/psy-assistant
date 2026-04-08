-- V40: Caseload Overview infrastructure (PA-30)

-- 1. Link therapist_profile to users (resolves owner_id join ambiguity)
ALTER TABLE therapist_profile
    ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_therapist_profile_user_id ON therapist_profile(user_id);

-- Backfill user_id from matching email
UPDATE therapist_profile tp
SET user_id = (SELECT u.id FROM users u WHERE u.email = tp.email)
WHERE user_id IS NULL;

-- 2. Contracted hours per week
ALTER TABLE therapist_profile
    ADD COLUMN IF NOT EXISTS contracted_hours_per_week NUMERIC(5, 2)
        CHECK (contracted_hours_per_week IS NULL OR contracted_hours_per_week > 0);

-- 3. Supervisor team membership
CREATE TABLE IF NOT EXISTS supervisor_team_member (
    supervisor_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    assigned_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by          VARCHAR(255),
    PRIMARY KEY (supervisor_id, therapist_profile_id)
);

CREATE INDEX IF NOT EXISTS idx_stm_supervisor ON supervisor_team_member(supervisor_id);

-- 4. Daily caseload snapshot
CREATE TABLE IF NOT EXISTS caseload_snapshot (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id      UUID         NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    snapshot_date             DATE         NOT NULL,
    active_client_count       INTEGER      NOT NULL DEFAULT 0,
    sessions_this_week        INTEGER      NOT NULL DEFAULT 0,
    sessions_this_month       INTEGER      NOT NULL DEFAULT 0,
    scheduled_hours_this_week NUMERIC(8,2) NOT NULL DEFAULT 0,
    contracted_hours_per_week NUMERIC(5,2),
    utilization_rate          NUMERIC(6,4),
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_caseload_snapshot UNIQUE (therapist_profile_id, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_caseload_snapshot_date_therapist
    ON caseload_snapshot(snapshot_date DESC, therapist_profile_id);

CREATE INDEX IF NOT EXISTS idx_caseload_snapshot_therapist
    ON caseload_snapshot(therapist_profile_id, snapshot_date DESC);

-- 5. Treatment status on clients (needed for caseload drill-down view)
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS treatment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (treatment_status IN ('ACTIVE', 'ON_HOLD', 'DISCHARGED'));
