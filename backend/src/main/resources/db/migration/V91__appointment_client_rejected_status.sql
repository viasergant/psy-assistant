-- V91: Add CLIENT_REJECTED status (client declined the session via Telegram; the session stays on the calendar)
ALTER TABLE appointment DROP CONSTRAINT chk_appointment_status;

ALTER TABLE appointment ADD CONSTRAINT chk_appointment_status
    CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW', 'CLIENT_REJECTED'));
