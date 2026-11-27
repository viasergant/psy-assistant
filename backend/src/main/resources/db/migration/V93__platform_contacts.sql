-- V93: Platform-wide contact details shown on the public Contacts page.
-- Global table (no org_id): one row, edited only by the global administrator.
CREATE TABLE platform_contacts (
    id                SMALLINT     PRIMARY KEY CHECK (id = 1),
    phone             VARCHAR(32),
    email             VARCHAR(254),
    telegram_channels TEXT,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
