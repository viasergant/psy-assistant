-- V6: CRM — leads and lead contact methods.
CREATE TABLE IF NOT EXISTS leads (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name         VARCHAR(255) NOT NULL,
    source            VARCHAR(100),
    status            VARCHAR(50)  NOT NULL DEFAULT 'NEW',
    owner_id          UUID         REFERENCES users(id) ON DELETE SET NULL,
    notes             TEXT,
    last_contact_date TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255)
);

ALTER TABLE leads ADD CONSTRAINT chk_lead_status
    CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CONVERTED', 'INACTIVE'));

CREATE TABLE IF NOT EXISTS lead_contact_methods (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id    UUID        NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL,
    value      VARCHAR(255) NOT NULL,
    is_primary BOOLEAN     NOT NULL DEFAULT FALSE
);

ALTER TABLE lead_contact_methods ADD CONSTRAINT chk_contact_type
    CHECK (type IN ('EMAIL', 'PHONE'));

-- Trigger: at least one contact method per lead (DEFERRABLE for replace-on-edit)
CREATE OR REPLACE FUNCTION check_lead_has_contact()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (SELECT COUNT(*) FROM lead_contact_methods WHERE lead_id = OLD.lead_id) = 0 THEN
        RAISE EXCEPTION 'Lead must have at least one contact method';
    END IF;
    RETURN OLD;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_lead_contact_min
    AFTER DELETE ON lead_contact_methods
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_lead_has_contact();

CREATE INDEX IF NOT EXISTS idx_leads_status     ON leads(status);
CREATE INDEX IF NOT EXISTS idx_leads_owner_id   ON leads(owner_id);
CREATE INDEX IF NOT EXISTS idx_leads_created_at ON leads(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lcm_lead_id      ON lead_contact_methods(lead_id);
