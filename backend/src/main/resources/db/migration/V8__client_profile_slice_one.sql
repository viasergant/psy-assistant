-- V8: Client profile slice one.
-- Adds structured client profile fields, optimistic locking, and
-- dedicated immutable audit tables for profile updates.

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS client_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS preferred_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS sex_or_gender VARCHAR(64),
    ADD COLUMN IF NOT EXISTS pronouns VARCHAR(64),
    ADD COLUMN IF NOT EXISTS assigned_therapist_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS secondary_phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS address_line_1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_line_2 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city VARCHAR(128),
    ADD COLUMN IF NOT EXISTS region VARCHAR(128),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS country VARCHAR(128),
    ADD COLUMN IF NOT EXISTS referral_source VARCHAR(255),
    ADD COLUMN IF NOT EXISTS referral_contact_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS referral_notes TEXT,
    ADD COLUMN IF NOT EXISTS preferred_communication_method VARCHAR(20),
    ADD COLUMN IF NOT EXISTS allow_phone BOOLEAN,
    ADD COLUMN IF NOT EXISTS allow_sms BOOLEAN,
    ADD COLUMN IF NOT EXISTS allow_email BOOLEAN,
    ADD COLUMN IF NOT EXISTS allow_voicemail BOOLEAN,
    ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(255),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS emergency_contact_email VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uq_clients_client_code ON clients(client_code)
    WHERE client_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_clients_assigned_therapist_id ON clients(assigned_therapist_id);

CREATE TABLE IF NOT EXISTS client_profile_audit_entry (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id     UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_name    VARCHAR(255),
    event_type    VARCHAR(64) NOT NULL,
    section       VARCHAR(64),
    request_id    VARCHAR(64),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS client_profile_audit_change (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id    UUID NOT NULL REFERENCES client_profile_audit_entry(id) ON DELETE CASCADE,
    field_name  VARCHAR(128) NOT NULL,
    old_value   TEXT,
    new_value   TEXT
);

CREATE INDEX IF NOT EXISTS idx_cpae_client_id_created_at
    ON client_profile_audit_entry(client_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cpac_entry_id
    ON client_profile_audit_change(entry_id);
