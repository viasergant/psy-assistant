-- V21: Therapist leave periods table.

CREATE TABLE IF NOT EXISTS therapist_leave (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    start_date           DATE NOT NULL,
    end_date             DATE NOT NULL,
    leave_type           VARCHAR(32) NOT NULL CHECK (leave_type IN ('ANNUAL', 'SICK', 'PUBLIC_HOLIDAY', 'OTHER')),
    status               VARCHAR(32) NOT NULL DEFAULT 'PENDING' 
                         CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    request_notes        TEXT, -- Therapist-submitted notes
    admin_notes          TEXT, -- Admin response notes
    requested_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at          TIMESTAMPTZ, -- Timestamp when admin approved/rejected
    reviewed_by          UUID REFERENCES users(id) ON DELETE SET NULL, -- Admin who reviewed
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(255),
    
    CONSTRAINT chk_leave_date_order CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_tl_therapist_id 
    ON therapist_leave(therapist_profile_id);

CREATE INDEX IF NOT EXISTS idx_tl_therapist_status 
    ON therapist_leave(therapist_profile_id, status);

CREATE INDEX IF NOT EXISTS idx_tl_therapist_dates 
    ON therapist_leave(therapist_profile_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_tl_status_requested 
    ON therapist_leave(status, requested_at DESC) 
    WHERE status = 'PENDING';

COMMENT ON TABLE therapist_leave IS 
    'Leave period requests and approvals. Approved leave blocks all dates in the range from scheduling.';

COMMENT ON COLUMN therapist_leave.leave_type IS 
    'ANNUAL = annual leave, SICK = sick leave, PUBLIC_HOLIDAY = public holiday, OTHER = other';

COMMENT ON COLUMN therapist_leave.status IS 
    'PENDING = awaiting admin review, APPROVED = accepted and blocking schedule, REJECTED = denied, CANCELLED = withdrawn by therapist';

COMMENT ON COLUMN therapist_leave.reviewed_by IS 
    'User ID of the administrator who approved or rejected this leave request';
