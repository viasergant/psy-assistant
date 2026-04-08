-- V38: Outcome measure definition and entry tables (PA-44)

-- -----------------------------------------------------------------------
-- outcome_measure_definition
-- -----------------------------------------------------------------------
CREATE TABLE outcome_measure_definition (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50)  NOT NULL,
    display_name     VARCHAR(255) NOT NULL,
    description      TEXT,
    min_score        INT          NOT NULL DEFAULT 0,
    max_score        INT          NOT NULL,
    alert_threshold  INT,
    alert_label      VARCHAR(255),
    alert_severity   VARCHAR(10)  CHECK (alert_severity IN ('WARNING','ALERT')),
    sort_order       SMALLINT     NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    CONSTRAINT uq_outcome_measure_code UNIQUE (code)
);

CREATE INDEX idx_outcome_measure_def_active
    ON outcome_measure_definition(active, sort_order);

-- -----------------------------------------------------------------------
-- outcome_measure_entry (append-only, no updated_at)
-- -----------------------------------------------------------------------
CREATE TABLE outcome_measure_entry (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    care_plan_id            UUID        NOT NULL REFERENCES care_plan(id) ON DELETE CASCADE,
    measure_definition_id   UUID        NOT NULL REFERENCES outcome_measure_definition(id),
    score                   INT         NOT NULL,
    assessment_date         DATE        NOT NULL,
    notes                   TEXT,
    recorded_by_user_id     UUID        NOT NULL REFERENCES users(id),
    recorded_by_name        VARCHAR(255),
    threshold_breached      BOOLEAN     NOT NULL DEFAULT FALSE,
    alert_label             VARCHAR(255),
    alert_severity          VARCHAR(10) CHECK (alert_severity IN ('WARNING','ALERT')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255)
);

CREATE INDEX idx_outcome_measure_entry_plan_def_date
    ON outcome_measure_entry(care_plan_id, measure_definition_id, assessment_date DESC);

CREATE INDEX idx_outcome_measure_entry_plan_date
    ON outcome_measure_entry(care_plan_id, assessment_date DESC);
