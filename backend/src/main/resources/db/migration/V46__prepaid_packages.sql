-- V46__prepaid_packages.sql
-- Prepaid package definitions, instances, and balance audit log for PA-49

-- Package definition (catalogue of sellable packages)
CREATE TABLE prepaid_package_definition (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(200) NOT NULL,
    service_type  VARCHAR(50)  NOT NULL,
    session_qty   INT          NOT NULL CHECK (session_qty > 0),
    price         NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_by    VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by    VARCHAR(255),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_pkg_def_name UNIQUE (name)
);

-- Sold package instances (one per client purchase)
CREATE TABLE prepaid_package_instance (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    definition_id       UUID         NOT NULL REFERENCES prepaid_package_definition(id),
    client_id           UUID         NOT NULL REFERENCES clients(id),
    purchased_at        TIMESTAMPTZ  NOT NULL,
    invoice_id          UUID,        -- FK set after invoice is created; FK added below
    sessions_remaining  INT          NOT NULL CHECK (sessions_remaining >= 0),
    sessions_total      INT          NOT NULL CHECK (sessions_total > 0),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'EXHAUSTED', 'EXPIRED')),
    expires_at          DATE,
    created_by          VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by          VARCHAR(255),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Balance audit log (immutable – no updated_by/updated_at)
CREATE TABLE package_balance_audit (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    package_instance_id UUID         NOT NULL REFERENCES prepaid_package_instance(id),
    session_id          UUID         REFERENCES session_record(id),
    therapist_id        UUID         REFERENCES users(id),
    balance_before      INT          NOT NULL,
    balance_after       INT          NOT NULL,
    action              VARCHAR(30)  NOT NULL CHECK (action IN ('DEDUCT', 'EXPIRE', 'MANUAL_ADJUST')),
    actor               VARCHAR(255) NOT NULL,
    occurred_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Indexes for common queries
CREATE INDEX idx_pkg_instance_client ON prepaid_package_instance(client_id);
CREATE INDEX idx_pkg_instance_status ON prepaid_package_instance(status);
CREATE INDEX idx_pkg_instance_expires ON prepaid_package_instance(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_pkg_balance_audit_instance ON package_balance_audit(package_instance_id);

-- Add FK from invoices to prepaid_package_instance (replaces the unresolved placeholder column)
ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_package
    FOREIGN KEY (prepaid_package_id) REFERENCES prepaid_package_instance(id);

-- Add FK from prepaid_package_instance to invoices (after invoices FK is added)
ALTER TABLE prepaid_package_instance
    ADD CONSTRAINT fk_pkg_instance_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id);
