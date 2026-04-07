-- V34: Session notes with structured templates, version history, and visibility controls
--
-- Creates tables for:
-- 1. session_note: Primary note record (one or many per session, per author)
-- 2. session_note_version: Immutable revision history (never deleted)
-- 3. note_template: System-defined structured templates

-- Note type enum
CREATE TYPE note_type AS ENUM ('FREE_FORM', 'STRUCTURED');

-- Note visibility enum
CREATE TYPE note_visibility AS ENUM ('PRIVATE', 'SUPERVISOR_VISIBLE');

-- Main session note table
CREATE TABLE session_note (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_record_id UUID         NOT NULL REFERENCES session_record(id),
    author_id         VARCHAR(255) NOT NULL,
    note_type         note_type    NOT NULL,
    visibility        note_visibility NOT NULL DEFAULT 'PRIVATE',
    content           TEXT,                    -- encrypted rich-text HTML (FREE_FORM)
    structured_fields TEXT,                    -- encrypted JSON string (STRUCTURED)
    content_hash      VARCHAR(64),             -- SHA-256 of plaintext for idempotency
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(255)
);

COMMENT ON TABLE  session_note IS 'Clinical session notes written by therapists; content is AES-256-GCM encrypted at rest';
COMMENT ON COLUMN session_note.content IS 'Encrypted rich-text HTML for FREE_FORM notes';
COMMENT ON COLUMN session_note.structured_fields IS 'Encrypted JSON map of field-key to value for STRUCTURED notes';
COMMENT ON COLUMN session_note.content_hash IS 'SHA-256 of the plaintext content used to prevent duplicate version entries on identical re-saves';

-- Immutable version history table (records are never deleted)
CREATE TABLE session_note_version (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id           UUID         NOT NULL REFERENCES session_note(id),
    version_number    INT          NOT NULL,
    content           TEXT,                    -- encrypted snapshot
    structured_fields TEXT,                    -- encrypted JSON snapshot
    content_hash      VARCHAR(64),
    author_id         VARCHAR(255) NOT NULL,
    visibility        note_visibility NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE session_note_version IS 'Immutable snapshot of every previous version of a session note; rows are never deleted';

-- System-defined note templates (seeded by V35)
CREATE TABLE note_template (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    template_fields JSONB        NOT NULL,     -- array of {key, label, required} objects
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE note_template IS 'System-defined structured note templates; not user-manageable';

-- Indexes
CREATE INDEX idx_session_note_session_record ON session_note(session_record_id);
CREATE INDEX idx_session_note_author        ON session_note(author_id);
CREATE INDEX idx_session_note_version_note  ON session_note_version(note_id);
