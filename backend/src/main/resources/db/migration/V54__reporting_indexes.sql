-- ============================================================
-- V54 – Performance indexes for the PA-55 reporting queries
-- ============================================================

-- Lead conversion report: queries by created_at and source
CREATE INDEX IF NOT EXISTS idx_leads_created_at_source
    ON leads (created_at, source);

-- Therapist utilization: queries by therapist, start_time, and status
CREATE INDEX IF NOT EXISTS idx_appointment_therapist_start_status
    ON appointment (therapist_profile_id, start_time, status);

-- No-show/cancellation report: queries by start_time and status
CREATE INDEX IF NOT EXISTS idx_appointment_start_status
    ON appointment (start_time, status);

-- Revenue report: queries by therapist, issued_date, and status
CREATE INDEX IF NOT EXISTS idx_invoices_therapist_issued_date
    ON invoices (therapist_id, issued_date, status);

-- Client retention: queries by client and session_date
CREATE INDEX IF NOT EXISTS idx_session_record_client_date
    ON session_record (client_id, session_date);

-- Therapist utilization in client-retention context
CREATE INDEX IF NOT EXISTS idx_session_record_therapist_date
    ON session_record (therapist_id, session_date);
