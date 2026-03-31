-- Add language preference column to users table
-- Supports locale selection per user (en, uk)
-- Default is 'en' (English)

ALTER TABLE users
    ADD COLUMN language VARCHAR(5) DEFAULT 'en' NOT NULL;

COMMENT ON COLUMN users.language IS 'User preferred locale (en, uk)';
