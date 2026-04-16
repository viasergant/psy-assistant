-- PA-41: Add at-risk flag to clients for no-show threshold tracking

ALTER TABLE clients
    ADD COLUMN is_at_risk BOOLEAN NOT NULL DEFAULT FALSE;
