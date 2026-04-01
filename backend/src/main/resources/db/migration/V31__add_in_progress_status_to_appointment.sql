-- V31: Add IN_PROGRESS status to appointment check constraint

-- Drop the existing check constraint
ALTER TABLE appointment DROP CONSTRAINT chk_appointment_status;

-- Recreate the constraint with IN_PROGRESS status included
ALTER TABLE appointment ADD CONSTRAINT chk_appointment_status 
    CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));

-- Add comment explaining the status lifecycle
COMMENT ON COLUMN appointment.status IS 'Appointment lifecycle status: SCHEDULED (created) → CONFIRMED (client confirmed) → IN_PROGRESS (session started) → COMPLETED/NO_SHOW/CANCELLED (terminal states)';
