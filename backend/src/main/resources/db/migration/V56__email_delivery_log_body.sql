-- V56: Add body column to email_delivery_log to persist rendered email content.
ALTER TABLE email_delivery_log
    ADD COLUMN IF NOT EXISTS body TEXT;
