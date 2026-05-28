-- V70: Fix package_balance_audit.therapist_id foreign key reference
--
-- The therapist_id column was incorrectly constrained to reference users(id).
-- It should reference therapist_profile(id) because session_record.therapist_id
-- stores therapist_profile.id (copied from the appointment), and the
-- PackageDeductionEventHandler passes that same value via SessionCompletedEvent.
--
-- This is the same class of bug that was fixed for invoices in V43.

ALTER TABLE package_balance_audit
    DROP CONSTRAINT IF EXISTS package_balance_audit_therapist_id_fkey;

ALTER TABLE package_balance_audit
    ADD CONSTRAINT package_balance_audit_therapist_id_fkey
        FOREIGN KEY (therapist_id) REFERENCES therapist_profile(id);
