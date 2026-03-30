-- V16: Therapist specializations and languages (many-to-many junctions).
-- Creates junction tables linking therapists to their specializations and languages.

CREATE TABLE IF NOT EXISTS therapist_specialization (
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    specialization_id UUID NOT NULL REFERENCES specialization(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (therapist_profile_id, specialization_id)
);

CREATE TABLE IF NOT EXISTS therapist_language (
    therapist_profile_id UUID NOT NULL REFERENCES therapist_profile(id) ON DELETE CASCADE,
    language_id UUID NOT NULL REFERENCES language(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (therapist_profile_id, language_id)
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_therapist_specialization_specialization_id ON therapist_specialization(specialization_id);
CREATE INDEX IF NOT EXISTS idx_therapist_language_language_id ON therapist_language(language_id);
