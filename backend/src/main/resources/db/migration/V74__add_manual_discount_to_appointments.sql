-- V74: Add manual_discount column to appointments and session_record.
-- Allows a one-off prepaid amount entered at booking time to be propagated
-- through to the auto-generated invoice as a discount deduction.

ALTER TABLE appointment
    ADD COLUMN IF NOT EXISTS manual_discount NUMERIC(12, 2);

ALTER TABLE session_record
    ADD COLUMN IF NOT EXISTS manual_discount NUMERIC(12, 2);
