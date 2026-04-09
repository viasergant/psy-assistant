-- V47__discount_rules.sql
-- Discount rules and per-line-item discount snapshots for PA-49

-- Add service_catalog_id column to invoice_line_items for SERVICE-scoped discounts
ALTER TABLE invoice_line_items
    ADD COLUMN service_catalog_id UUID REFERENCES service_catalog(id);

-- Discount rule definitions
CREATE TABLE discount_rules (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(200) NOT NULL,
    type                VARCHAR(20)  NOT NULL CHECK (type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    value               NUMERIC(12,2) NOT NULL CHECK (value > 0),
    scope               VARCHAR(20)  NOT NULL CHECK (scope IN ('CLIENT', 'SERVICE')),
    client_id           UUID         REFERENCES clients(id),
    service_catalog_id  UUID         REFERENCES service_catalog(id),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by          VARCHAR(255),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- CLIENT scope: client_id required, service_catalog_id must be null
    CONSTRAINT chk_discount_client_scope
        CHECK (scope <> 'CLIENT' OR (client_id IS NOT NULL AND service_catalog_id IS NULL)),
    -- SERVICE scope: service_catalog_id required, client_id must be null
    CONSTRAINT chk_discount_service_scope
        CHECK (scope <> 'SERVICE' OR (service_catalog_id IS NOT NULL AND client_id IS NULL))
);

-- Immutable snapshot of discount applied to each invoice line item
CREATE TABLE invoice_line_item_discounts (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    line_item_id      UUID         NOT NULL REFERENCES invoice_line_items(id) ON DELETE CASCADE,
    discount_rule_id  UUID         REFERENCES discount_rules(id),  -- null = manual override
    discount_type     VARCHAR(20)  NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value    NUMERIC(12,2) NOT NULL,
    cap_applied       BOOLEAN      NOT NULL DEFAULT FALSE,
    discount_amount   NUMERIC(12,2) NOT NULL,
    created_by        VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_discount_rules_client    ON discount_rules(client_id) WHERE client_id IS NOT NULL;
CREATE INDEX idx_discount_rules_service   ON discount_rules(service_catalog_id) WHERE service_catalog_id IS NOT NULL;
CREATE INDEX idx_discount_rules_active    ON discount_rules(active) WHERE active = TRUE;
CREATE INDEX idx_line_discount_line_item  ON invoice_line_item_discounts(line_item_id);
CREATE INDEX idx_line_items_catalog       ON invoice_line_items(service_catalog_id) WHERE service_catalog_id IS NOT NULL;
