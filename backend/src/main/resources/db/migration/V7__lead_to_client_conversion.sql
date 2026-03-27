-- V7: Lead-to-client conversion — clients table, client contact methods,
--      and back-link column on leads.

CREATE TABLE IF NOT EXISTS clients (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name        VARCHAR(255) NOT NULL,
    owner_id         UUID         REFERENCES users(id) ON DELETE SET NULL,
    notes            TEXT,
    source_lead_id   UUID         UNIQUE REFERENCES leads(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS client_contact_methods (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id  UUID         NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    type       VARCHAR(20)  NOT NULL CHECK (type IN ('EMAIL', 'PHONE')),
    value      VARCHAR(255) NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE
);

ALTER TABLE leads ADD COLUMN IF NOT EXISTS converted_client_id UUID
    REFERENCES clients(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_clients_source_lead_id ON clients(source_lead_id);
CREATE INDEX IF NOT EXISTS idx_clients_owner_id       ON clients(owner_id);
CREATE INDEX IF NOT EXISTS idx_ccm_client_id          ON client_contact_methods(client_id);
