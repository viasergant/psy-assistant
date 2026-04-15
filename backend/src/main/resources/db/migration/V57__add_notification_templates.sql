-- V57: Notification template management (PA-52)
-- Stores configurable message templates per event type / channel / language.

CREATE TABLE notification_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(64)  NOT NULL,
    channel     VARCHAR(16)  NOT NULL,
    language    VARCHAR(8)   NOT NULL,
    subject     TEXT,
    body        TEXT         NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'INACTIVE',
    has_unknown_variables BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(255)
);

-- Partial unique index: at most one ACTIVE template per event_type/channel/language.
CREATE UNIQUE INDEX uq_notification_template_active
    ON notification_templates (event_type, channel, language)
    WHERE status = 'ACTIVE';
