-- PA-41: Create follow-up tasks table for no-show and late cancellation workflows

CREATE TYPE follow_up_task_status AS ENUM ('PENDING', 'COMPLETED', 'DISMISSED');
CREATE TYPE follow_up_task_type   AS ENUM ('NO_SHOW_FOLLOW_UP');

CREATE TABLE session_follow_up_tasks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_record_id   UUID NOT NULL UNIQUE REFERENCES session_record (id),
    assigned_to_user_id UUID REFERENCES users (id),
    task_type           follow_up_task_type NOT NULL,
    status              follow_up_task_status NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_follow_up_tasks_session_record ON session_follow_up_tasks (session_record_id);
CREATE INDEX idx_follow_up_tasks_assigned_to    ON session_follow_up_tasks (assigned_to_user_id);
CREATE INDEX idx_follow_up_tasks_status         ON session_follow_up_tasks (status);
