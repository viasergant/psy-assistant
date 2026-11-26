-- Telegram notifications for therapists: per-user settings/link, one-time link tokens,
-- delivery outbox and long-polling offset. All tenant tables carry org_id.

CREATE TABLE therapist_notification_settings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    org_id                  UUID NOT NULL REFERENCES organizations(id),
    user_id                 UUID NOT NULL,
    chat_id_encrypted       TEXT,
    chat_id_hash            CHAR(64),
    linked_at               TIMESTAMPTZ,
    timezone                VARCHAR(64) NOT NULL DEFAULT 'UTC',
    language                VARCHAR(5) NOT NULL DEFAULT 'en',
    digest_enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    digest_time             TIME NOT NULL DEFAULT '07:30',
    digest_days             INT NOT NULL DEFAULT 127,
    skip_empty_digest       BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_minutes_before INT NOT NULL DEFAULT 60,
    include_client_names    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_tns_user UNIQUE (user_id),
    CONSTRAINT ck_tns_reminder CHECK (reminder_minutes_before BETWEEN 5 AND 1440),
    CONSTRAINT ck_tns_days CHECK (digest_days BETWEEN 0 AND 127)
);
-- One Telegram chat may be linked to exactly one therapist.
CREATE UNIQUE INDEX uq_tns_chat_hash ON therapist_notification_settings(chat_id_hash)
    WHERE chat_id_hash IS NOT NULL;
CREATE INDEX idx_tns_org ON therapist_notification_settings(org_id);

CREATE TABLE telegram_link_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    org_id      UUID NOT NULL REFERENCES organizations(id),
    user_id     UUID NOT NULL,
    token_hash  CHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ
);
CREATE INDEX idx_tlt_user ON telegram_link_token(user_id);

CREATE TABLE telegram_delivery_log (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(255),
    org_id         UUID NOT NULL REFERENCES organizations(id),
    user_id        UUID NOT NULL,
    kind           VARCHAR(20) NOT NULL,
    dedup_key      VARCHAR(255) NOT NULL UNIQUE,
    body           TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count    INT NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMPTZ,
    last_error     VARCHAR(255),
    sent_at        TIMESTAMPTZ
);
CREATE INDEX idx_tdl_status_retry ON telegram_delivery_log(status, next_retry_at);

CREATE TABLE telegram_poll_state (
    id           INT PRIMARY KEY CHECK (id = 1),
    last_update  BIGINT NOT NULL DEFAULT 0
);
INSERT INTO telegram_poll_state (id, last_update) VALUES (1, 0);
