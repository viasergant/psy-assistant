-- PA-41: Create attendance audit log (append-only, no FK on session_record_id)

CREATE TABLE attendance_audit_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_record_id   UUID NOT NULL,
    changed_by_user_id  UUID REFERENCES users (id),
    changed_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    previous_outcome    attendance_outcome_type,
    new_outcome         attendance_outcome_type NOT NULL,
    note                TEXT
);

CREATE INDEX idx_attendance_audit_session ON attendance_audit_log (session_record_id);
CREATE INDEX idx_attendance_audit_changed_at ON attendance_audit_log (changed_at);
