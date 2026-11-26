-- Telegram link for clients: a therapist invites a client with a one-time deep link; the bot binds the
-- client's private chat to exactly one client row. Tenant table: carries org_id.

CREATE TABLE client_telegram_link (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255),
    org_id            UUID NOT NULL REFERENCES organizations(id),
    client_id         UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    chat_id_encrypted TEXT NOT NULL,
    chat_id_hash      CHAR(64) NOT NULL,
    language          VARCHAR(5) NOT NULL DEFAULT 'en',
    linked_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ctl_client UNIQUE (client_id)
);
-- One Telegram chat may be linked to exactly one client.
CREATE UNIQUE INDEX uq_ctl_chat_hash ON client_telegram_link(chat_id_hash);
CREATE INDEX idx_ctl_org ON client_telegram_link(org_id);

-- A link token now belongs either to a therapist user or to a client.
ALTER TABLE telegram_link_token ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE telegram_link_token ADD COLUMN client_id UUID REFERENCES clients(id) ON DELETE CASCADE;
ALTER TABLE telegram_link_token ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'en';
ALTER TABLE telegram_link_token ADD CONSTRAINT ck_tlt_owner
    CHECK ((user_id IS NOT NULL AND client_id IS NULL) OR (user_id IS NULL AND client_id IS NOT NULL));
CREATE INDEX idx_tlt_client ON telegram_link_token(client_id);
