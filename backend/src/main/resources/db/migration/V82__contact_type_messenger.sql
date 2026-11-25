-- V82: Allow MESSENGER as a contact method type for leads and clients.
-- The existing CHECK constraints (V6 named, V7 unnamed/inline) are located by definition
-- so the migration does not depend on auto-generated constraint names.

DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT conrelid::regclass AS tbl, conname
        FROM pg_constraint
        WHERE contype = 'c'
          AND conrelid IN ('lead_contact_methods'::regclass, 'client_contact_methods'::regclass)
          AND pg_get_constraintdef(oid) LIKE '%EMAIL%'
          AND pg_get_constraintdef(oid) LIKE '%PHONE%'
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', c.tbl, c.conname);
    END LOOP;
END $$;

ALTER TABLE lead_contact_methods ADD CONSTRAINT chk_contact_type
    CHECK (type IN ('EMAIL', 'PHONE', 'MESSENGER'));
ALTER TABLE client_contact_methods ADD CONSTRAINT chk_client_contact_type
    CHECK (type IN ('EMAIL', 'PHONE', 'MESSENGER'));
