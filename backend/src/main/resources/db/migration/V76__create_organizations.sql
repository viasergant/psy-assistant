-- V76: Introduce Organization entity for multi-tenant deployments.
-- Each organization represents an independent practice (solo therapist or future clinic).
-- Existing deployments receive a single default organization so all subsequent backfills
-- can reference a known, stable UUID.

CREATE TABLE organizations (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL CHECK (type IN ('SOLO', 'CLINIC')),
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TRIAL')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_status ON organizations(status);

-- Seed a default organization for pre-existing deployments.
-- The well-known UUID is intentional: V77 and V78 reference it directly for the backfill.
INSERT INTO organizations (id, name, slug, type, status)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Default Organization', 'default', 'CLINIC', 'ACTIVE');
