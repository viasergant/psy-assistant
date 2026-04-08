-- ============================================================
-- V42 – Invoice generation: invoices, line items, number seq
-- ============================================================

-- Invoice number sequence lock (one row per calendar year)
CREATE TABLE invoice_number_seq (
    invoice_year SMALLINT PRIMARY KEY,
    last_seq     INT NOT NULL DEFAULT 0
);

-- Main invoice table
CREATE TABLE invoices (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number      VARCHAR(20)  NOT NULL UNIQUE,
    invoice_year        SMALLINT     NOT NULL,
    invoice_seq         INT          NOT NULL,
    UNIQUE (invoice_year, invoice_seq),
    client_id           UUID         NOT NULL REFERENCES clients(id),
    therapist_id        UUID         REFERENCES users(id),
    source              VARCHAR(20)  NOT NULL CHECK (source IN ('SESSION', 'PACKAGE', 'MANUAL')),
    session_id          UUID         REFERENCES session_record(id),
    prepaid_package_id  UUID,        -- FK to prepaid_package when PA-49 lands
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT', 'ISSUED', 'OVERDUE', 'PAID', 'CANCELLED')),
    issued_date         DATE,
    due_date            DATE,
    cancellation_reason TEXT,
    cancelled_at        TIMESTAMPTZ,
    subtotal            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount            NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total               NUMERIC(12, 2) NOT NULL DEFAULT 0,
    pdf_path            VARCHAR(500),
    notes               TEXT,
    created_by          VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by          VARCHAR(255),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Invoice line items
CREATE TABLE invoice_line_items (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID          NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500)  NOT NULL,
    quantity    NUMERIC(8, 2) NOT NULL,
    unit_price  NUMERIC(12, 2) NOT NULL,
    line_total  NUMERIC(12, 2) NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0,
    created_by  VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by  VARCHAR(255),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Performance indexes
CREATE INDEX idx_invoices_client_id    ON invoices(client_id);
CREATE INDEX idx_invoices_status       ON invoices(status);
CREATE INDEX idx_invoices_issued_date  ON invoices(issued_date);
CREATE INDEX idx_invoices_therapist_id ON invoices(therapist_id);
