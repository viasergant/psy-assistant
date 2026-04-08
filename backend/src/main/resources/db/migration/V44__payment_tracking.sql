-- ============================================================
-- V44 – Payment tracking: payments, refunds, wallets
-- ============================================================

-- 1. Add paid_amount to invoices (default 0 for existing rows)
ALTER TABLE invoices
    ADD COLUMN paid_amount NUMERIC(12,2) NOT NULL DEFAULT 0;

-- 2. Extend status constraint to include PARTIALLY_PAID
ALTER TABLE invoices
    DROP CONSTRAINT IF EXISTS invoices_status_check;
ALTER TABLE invoices
    ADD CONSTRAINT invoices_status_check
    CHECK (status IN ('DRAFT','ISSUED','OVERDUE','PAID','PARTIALLY_PAID','CANCELLED'));

-- 3. Immutable payments ledger
CREATE TABLE payments (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id     UUID           NOT NULL REFERENCES invoices(id),
    amount         NUMERIC(12,2)  NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(20)    NOT NULL CHECK (payment_method IN ('CASH','BANK_TRANSFER','CARD')),
    payment_date   DATE           NOT NULL,
    reference      VARCHAR(255),
    notes          TEXT,
    created_by     VARCHAR(255),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
    -- intentionally no updated_at: payments are immutable
);

-- 4. Immutable refunds ledger
CREATE TABLE refunds (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id   UUID           NOT NULL REFERENCES invoices(id),
    payment_id   UUID           REFERENCES payments(id),
    amount       NUMERIC(12,2)  NOT NULL CHECK (amount > 0),
    reason       TEXT           NOT NULL,
    refund_date  DATE           NOT NULL,
    reference    VARCHAR(255),
    created_by   VARCHAR(255),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- 5. Client credit wallet (one row per client, for future overpayment credit)
CREATE TABLE client_wallets (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id  UUID           NOT NULL UNIQUE REFERENCES clients(id),
    balance    NUMERIC(12,2)  NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_by VARCHAR(255),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by VARCHAR(255),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- 6. Wallet transaction ledger (immutable audit trail)
CREATE TABLE wallet_transactions (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id        UUID           NOT NULL REFERENCES client_wallets(id),
    amount           NUMERIC(12,2)  NOT NULL,
    transaction_type VARCHAR(30)    NOT NULL
        CHECK (transaction_type IN ('OVERPAYMENT_CREDIT','INVOICE_DEBIT','REFUND_CREDIT','MANUAL_ADJUSTMENT')),
    reference_id     UUID,
    notes            TEXT,
    created_by       VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_payments_invoice_id      ON payments(invoice_id);
CREATE INDEX idx_payments_payment_date    ON payments(payment_date);
CREATE INDEX idx_refunds_invoice_id       ON refunds(invoice_id);
CREATE INDEX idx_wallet_txn_wallet_id     ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallets_client_id        ON client_wallets(client_id);
CREATE INDEX idx_invoices_status_due_date ON invoices(status, due_date);
