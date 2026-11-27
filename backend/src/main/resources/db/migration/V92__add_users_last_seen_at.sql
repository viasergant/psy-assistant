-- V92: Track the last authenticated activity of a user (presence for the global admin Users list)
ALTER TABLE users ADD COLUMN last_seen_at TIMESTAMPTZ;
