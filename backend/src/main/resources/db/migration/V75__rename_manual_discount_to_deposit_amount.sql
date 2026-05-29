-- V75: Rename manual_discount -> deposit_amount columns.
-- Advance payments collected at booking time are now recorded as Payment
-- ledger entries against the invoice rather than as a discount deduction,
-- preserving full revenue totals in reporting.

ALTER TABLE appointment
    RENAME COLUMN manual_discount TO deposit_amount;

ALTER TABLE session_record
    RENAME COLUMN manual_discount TO deposit_amount;
