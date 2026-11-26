-- The client bot is a separate Telegram bot, so it needs its own long-polling offset row.
ALTER TABLE telegram_poll_state DROP CONSTRAINT IF EXISTS telegram_poll_state_id_check;
INSERT INTO telegram_poll_state (id, last_update) VALUES (2, 0) ON CONFLICT (id) DO NOTHING;
