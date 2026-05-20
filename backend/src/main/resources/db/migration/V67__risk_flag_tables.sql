-- V67: Risk Flag tables (PA-27)
-- Creates risk_flag_types, client_risk_flags, risk_flag_audit_log and seeds initial flag types

-- -----------------------------------------------------------------------
-- risk_flag_types
-- -----------------------------------------------------------------------
CREATE TABLE risk_flag_types (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL UNIQUE,
    display_order SMALLINT     NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------
-- client_risk_flags
-- -----------------------------------------------------------------------
CREATE TABLE client_risk_flags (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id            UUID        NOT NULL REFERENCES clients(id),
    flag_type_id         UUID        NOT NULL REFERENCES risk_flag_types(id),
    status               VARCHAR(20) NOT NULL
                             CHECK (status IN ('ACTIVE','RESOLVED')),
    clinical_note        TEXT,
    review_date          DATE        NOT NULL,
    created_by_user_id   UUID        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_by_user_id  UUID        REFERENCES users(id),
    resolved_at          TIMESTAMPTZ,
    resolution_note      TEXT
);

CREATE INDEX idx_risk_flags_client_status
    ON client_risk_flags(client_id, status);

-- -----------------------------------------------------------------------
-- risk_flag_audit_log  (append-only; intentionally NO FK on flag_id)
-- -----------------------------------------------------------------------
CREATE TABLE risk_flag_audit_log (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id          UUID         NOT NULL,
    client_id        UUID         NOT NULL,
    actor_user_id    UUID         NOT NULL REFERENCES users(id),
    actor_name       VARCHAR(255) NOT NULL,
    action_type      VARCHAR(30)  NOT NULL
                         CHECK (action_type IN ('FLAG_CREATED','FLAG_RESOLVED','FLAG_UPDATED')),
    flag_type_name   VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    action_timestamp TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_flag_audit_client
    ON risk_flag_audit_log(client_id, action_timestamp DESC);

CREATE INDEX idx_risk_flag_audit_flag
    ON risk_flag_audit_log(flag_id);

-- -----------------------------------------------------------------------
-- Seed initial flag types
-- -----------------------------------------------------------------------
INSERT INTO risk_flag_types (name, display_order) VALUES
    ('Self-Harm Risk',          1),
    ('Crisis History',          2),
    ('Safeguarding Concern',    3),
    ('Domestic Abuse Concern',  4),
    ('Suicidal Ideation',       5);
