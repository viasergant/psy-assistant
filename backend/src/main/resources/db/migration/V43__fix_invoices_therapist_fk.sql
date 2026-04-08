-- V43: Fix invoices.therapist_id foreign key reference
--
-- The invoices.therapist_id column was incorrectly constrained to reference users(id).
-- It should reference therapist_profile(id) because:
--   1. The frontend sends therapist_profile.id when creating invoices.
--   2. session_record.therapist_id also stores therapist_profile.id (copied from appointment).
--
-- This migration drops the incorrect FK and adds the correct one.

ALTER TABLE invoices
    DROP CONSTRAINT IF EXISTS invoices_therapist_id_fkey;

ALTER TABLE invoices
    ADD CONSTRAINT invoices_therapist_id_fkey
        FOREIGN KEY (therapist_id) REFERENCES therapist_profile(id);
