-- V10: Align client_tags with BaseEntity audit columns.

ALTER TABLE client_tags
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

ALTER TABLE client_tags
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE client_tags
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE client_tags
    ALTER COLUMN updated_at SET NOT NULL;
