CREATE TABLE email_delivery_log (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  VARCHAR(255),
    event_type                  VARCHAR(100) NOT NULL,
    recipient_address_encrypted TEXT NOT NULL,
    recipient_address_hash      CHAR(64) NOT NULL,
    subject_template_key        VARCHAR(255) NOT NULL,
    status                      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    provider_message_id         VARCHAR(255),
    retry_count                 INT NOT NULL DEFAULT 0,
    next_retry_at               TIMESTAMPTZ,
    last_error                  TEXT,
    sent_at                     TIMESTAMPTZ
);

CREATE INDEX idx_edl_status_retry ON email_delivery_log(status, next_retry_at);
CREATE INDEX idx_edl_recipient_hash ON email_delivery_log(recipient_address_hash);
