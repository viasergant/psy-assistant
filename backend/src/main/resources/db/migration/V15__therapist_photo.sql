-- V15: Therapist photo storage table.
-- Stores profile photos with metadata (MIME type, size, upload timestamp).

CREATE TABLE IF NOT EXISTS therapist_photo (
    therapist_profile_id UUID PRIMARY KEY REFERENCES therapist_profile(id) ON DELETE CASCADE,
    mime_type VARCHAR(32) NOT NULL,
    data BYTEA NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    uploaded_by VARCHAR(255)
);

-- Index for recent uploads
CREATE INDEX IF NOT EXISTS idx_therapist_photo_uploaded_at ON therapist_photo(uploaded_at DESC);
