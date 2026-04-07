-- V36: Care Plan tables (PA-43)
-- Creates care_plan, care_plan_goal, care_plan_intervention, care_plan_milestone, care_plan_audit

-- -----------------------------------------------------------------------
-- care_plan
-- -----------------------------------------------------------------------
CREATE TABLE care_plan (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id        UUID        NOT NULL REFERENCES clients(id),
    therapist_id     UUID        NOT NULL REFERENCES users(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE','CLOSED','ARCHIVED')),
    closed_at        TIMESTAMPTZ,
    closed_by_user_id UUID       REFERENCES users(id),
    archived_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255)
);

CREATE INDEX idx_care_plan_client_active
    ON care_plan(client_id) WHERE status = 'ACTIVE';

CREATE INDEX idx_care_plan_therapist
    ON care_plan(therapist_id, status);

-- -----------------------------------------------------------------------
-- care_plan_goal
-- -----------------------------------------------------------------------
CREATE TABLE care_plan_goal (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    care_plan_id UUID        NOT NULL REFERENCES care_plan(id) ON DELETE CASCADE,
    description  TEXT        NOT NULL,
    priority     SMALLINT    NOT NULL DEFAULT 0,
    target_date  DATE,
    status       VARCHAR(30)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','IN_PROGRESS','ACHIEVED','ABANDONED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(255)
);

CREATE INDEX idx_care_plan_goal_plan ON care_plan_goal(care_plan_id);

-- -----------------------------------------------------------------------
-- care_plan_intervention
-- -----------------------------------------------------------------------
CREATE TABLE care_plan_intervention (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id           UUID        NOT NULL REFERENCES care_plan_goal(id) ON DELETE CASCADE,
    intervention_type VARCHAR(100) NOT NULL,
    description       TEXT        NOT NULL,
    frequency         VARCHAR(100),
    status            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE'
                          CHECK (status IN ('ACTIVE','COMPLETED','DISCONTINUED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255)
);

CREATE INDEX idx_care_plan_intervention_goal ON care_plan_intervention(goal_id);

-- -----------------------------------------------------------------------
-- care_plan_milestone
-- -----------------------------------------------------------------------
CREATE TABLE care_plan_milestone (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id             UUID        NOT NULL REFERENCES care_plan_goal(id) ON DELETE CASCADE,
    description         TEXT        NOT NULL,
    target_date         DATE,
    achieved_at         TIMESTAMPTZ,
    achieved_by_user_id UUID        REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255)
);

CREATE INDEX idx_care_plan_milestone_goal ON care_plan_milestone(goal_id);

-- -----------------------------------------------------------------------
-- care_plan_audit  (append-only; intentionally NO FK on care_plan_id)
-- -----------------------------------------------------------------------
CREATE TABLE care_plan_audit (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    care_plan_id     UUID        NOT NULL,
    entity_type      VARCHAR(30),
    entity_id        UUID        NOT NULL,
    action_type      VARCHAR(50)  NOT NULL
                         CHECK (action_type IN (
                             'PLAN_CREATED','PLAN_UPDATED','PLAN_CLOSED','PLAN_ARCHIVED',
                             'GOAL_ADDED','GOAL_UPDATED','GOAL_STATUS_CHANGED','GOAL_REMOVED',
                             'INTERVENTION_ADDED','INTERVENTION_UPDATED','INTERVENTION_REMOVED',
                             'MILESTONE_ADDED','MILESTONE_ACHIEVED','MILESTONE_REMOVED'
                         )),
    actor_user_id    UUID        NOT NULL REFERENCES users(id),
    actor_name       VARCHAR(255) NOT NULL,
    action_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    field_name       VARCHAR(100),
    old_value        TEXT,
    new_value        TEXT,
    request_id       VARCHAR(100),
    ip_address       VARCHAR(45),
    metadata         JSONB
);

CREATE INDEX idx_care_plan_audit_plan_ts
    ON care_plan_audit(care_plan_id, action_timestamp DESC);

CREATE INDEX idx_care_plan_audit_actor
    ON care_plan_audit(actor_user_id);

CREATE INDEX idx_care_plan_audit_entity
    ON care_plan_audit(entity_type, entity_id);
