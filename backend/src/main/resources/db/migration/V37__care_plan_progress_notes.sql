-- V37: Goal progress notes and extended audit types (PA-44)

-- -----------------------------------------------------------------------
-- Extend care_plan_goal.status to include PAUSED
-- -----------------------------------------------------------------------
ALTER TABLE care_plan_goal
    DROP CONSTRAINT IF EXISTS care_plan_goal_status_check;

ALTER TABLE care_plan_goal
    ADD CONSTRAINT care_plan_goal_status_check
        CHECK (status IN ('PENDING','IN_PROGRESS','ACHIEVED','ABANDONED','PAUSED'));

-- -----------------------------------------------------------------------
-- Extend care_plan_audit.action_type to include new types
-- -----------------------------------------------------------------------
ALTER TABLE care_plan_audit
    DROP CONSTRAINT IF EXISTS care_plan_audit_action_type_check;

ALTER TABLE care_plan_audit
    ADD CONSTRAINT care_plan_audit_action_type_check
        CHECK (action_type IN (
            'PLAN_CREATED','PLAN_UPDATED','PLAN_CLOSED','PLAN_ARCHIVED',
            'GOAL_ADDED','GOAL_UPDATED','GOAL_STATUS_CHANGED','GOAL_REMOVED',
            'INTERVENTION_ADDED','INTERVENTION_UPDATED','INTERVENTION_REMOVED',
            'MILESTONE_ADDED','MILESTONE_ACHIEVED','MILESTONE_REMOVED',
            'GOAL_PROGRESS_NOTE_ADDED','OUTCOME_MEASURE_RECORDED'
        ));

-- -----------------------------------------------------------------------
-- goal_progress_note (append-only, no updated_at)
-- -----------------------------------------------------------------------
CREATE TABLE goal_progress_note (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id          UUID         NOT NULL REFERENCES care_plan_goal(id) ON DELETE CASCADE,
    care_plan_id     UUID         NOT NULL,
    note_text        TEXT         NOT NULL,
    author_user_id   UUID         NOT NULL REFERENCES users(id),
    author_name      VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255)
);

CREATE INDEX idx_goal_progress_note_goal_ts
    ON goal_progress_note(goal_id, created_at DESC);

CREATE INDEX idx_goal_progress_note_plan_ts
    ON goal_progress_note(care_plan_id, created_at DESC);

CREATE INDEX idx_goal_progress_note_author
    ON goal_progress_note(author_user_id);
