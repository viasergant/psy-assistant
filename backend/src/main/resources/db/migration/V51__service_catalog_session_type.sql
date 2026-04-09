-- V51: PA-69 – Migrate service_catalog to use session_type_id FK (Phase 2c)
-- Replaces the legacy VARCHAR service_type enum column with a FK to session_type.

-- Step 1: add nullable FK
ALTER TABLE service_catalog
    ADD COLUMN session_type_id UUID REFERENCES session_type(id) ON DELETE RESTRICT;

-- Step 2: backfill using enum string → session_type.code mapping
UPDATE service_catalog sc
SET session_type_id = (
    SELECT st.id
    FROM session_type st
    WHERE st.code = CASE sc.service_type
        WHEN 'INDIVIDUAL_SESSION' THEN 'IN_PERSON'
        WHEN 'GROUP_SESSION'      THEN 'GROUP'
        WHEN 'INTAKE_ASSESSMENT'  THEN 'INTAKE'
        WHEN 'FOLLOW_UP'          THEN 'FOLLOW_UP'
        ELSE NULL
    END
);

-- Step 3: enforce NOT NULL (all existing rows have a mapping)
ALTER TABLE service_catalog
    ALTER COLUMN session_type_id SET NOT NULL;

-- Step 4: drop legacy column
ALTER TABLE service_catalog
    DROP COLUMN service_type;

CREATE INDEX IF NOT EXISTS idx_service_catalog_session_type_id
    ON service_catalog(session_type_id);
