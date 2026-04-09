-- V45__service_catalog.sql
-- Service catalog and pricing history tables for PA-46

-- Service catalog master table
CREATE TABLE service_catalog (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name          VARCHAR(200) NOT NULL,
  category      VARCHAR(100) NOT NULL,
  service_type  VARCHAR(50)  NOT NULL,
  duration_min  INTEGER      NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at    TIMESTAMPTZ  NOT NULL,
  updated_at    TIMESTAMPTZ  NOT NULL,
  created_by    VARCHAR(255),
  CONSTRAINT uq_catalog_name_category UNIQUE (name, category)
);

-- Append-only price history for default prices
CREATE TABLE service_catalog_price_history (
  id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  service_id     UUID          NOT NULL REFERENCES service_catalog(id),
  price          DECIMAL(10,2) NOT NULL,
  effective_from DATE          NOT NULL,
  effective_to   DATE,
  changed_by     VARCHAR(255)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL,
  CONSTRAINT chk_price_positive CHECK (price >= 0)
);

CREATE INDEX idx_ph_service_open ON service_catalog_price_history(service_id)
  WHERE effective_to IS NULL;

-- Per-therapist price overrides (current only, no history)
CREATE TABLE service_catalog_therapist_override (
  id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  service_id   UUID          NOT NULL REFERENCES service_catalog(id),
  therapist_id UUID          NOT NULL REFERENCES users(id),
  price        DECIMAL(10,2) NOT NULL,
  created_at   TIMESTAMPTZ   NOT NULL,
  updated_at   TIMESTAMPTZ   NOT NULL,
  created_by   VARCHAR(255),
  CONSTRAINT uq_override_service_therapist UNIQUE (service_id, therapist_id),
  CONSTRAINT chk_override_price_positive CHECK (price >= 0)
);
