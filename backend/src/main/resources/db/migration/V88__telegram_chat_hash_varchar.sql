-- Hibernate maps chat_id_hash to String (varchar) and ddl-auto=validate rejects the bpchar
-- columns created by V83/V85, which stops the application from starting.
-- The hash is a fixed 64-char hex digest, so converting to VARCHAR(64) loses no data.
ALTER TABLE therapist_notification_settings ALTER COLUMN chat_id_hash TYPE VARCHAR(64);
ALTER TABLE client_telegram_link            ALTER COLUMN chat_id_hash TYPE VARCHAR(64);
