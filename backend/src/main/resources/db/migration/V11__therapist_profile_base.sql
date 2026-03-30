-- V11: Therapist profile base schema.
-- Creates therapist_profile table with employment status tracking,
-- and core reference tables for specializations, languages, and service types.

-- Reference table: specializations
CREATE TABLE IF NOT EXISTS specialization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Reference table: languages
CREATE TABLE IF NOT EXISTS language (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL UNIQUE,
    language_code VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Reference table: service types (for pricing rules)
CREATE TABLE IF NOT EXISTS service_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Core therapist profile table
CREATE TABLE IF NOT EXISTS therapist_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(64),
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    bio TEXT,
    contact_phone VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255)
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_therapist_profile_active ON therapist_profile(active);
CREATE INDEX IF NOT EXISTS idx_therapist_profile_email ON therapist_profile(email);
CREATE INDEX IF NOT EXISTS idx_therapist_profile_created_at ON therapist_profile(created_at DESC);
