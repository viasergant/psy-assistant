-- V9: Client tags and profile photo metadata.

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS photo_storage_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS photo_mime_type VARCHAR(64),
    ADD COLUMN IF NOT EXISTS photo_updated_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS client_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tag VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (client_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_client_tags_client_id ON client_tags(client_id);
