-- Client-facing Telegram notifications: session reminders with a "cancel" button.

ALTER TABLE client_telegram_link ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';
ALTER TABLE telegram_link_token ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';

-- An outbox row is addressed either to a therapist user or to a client; client rows may carry the
-- appointment they are about (used for the cancel button and to drop stale reminders).
ALTER TABLE telegram_delivery_log ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE telegram_delivery_log ADD COLUMN client_id UUID REFERENCES clients(id) ON DELETE CASCADE;
ALTER TABLE telegram_delivery_log ADD COLUMN appointment_id UUID;
ALTER TABLE telegram_delivery_log ADD CONSTRAINT ck_tdl_recipient
    CHECK ((user_id IS NOT NULL AND client_id IS NULL) OR (user_id IS NULL AND client_id IS NOT NULL));
CREATE INDEX idx_tdl_client ON telegram_delivery_log(client_id) WHERE client_id IS NOT NULL;
CREATE INDEX idx_tdl_appointment ON telegram_delivery_log(appointment_id) WHERE appointment_id IS NOT NULL;
