-- V66: Fix recipient_address_hash column type from CHAR(64) to VARCHAR(64)
-- CHAR(64) is stored as bpchar in PostgreSQL which fails Hibernate schema validation
ALTER TABLE email_delivery_log
    ALTER COLUMN recipient_address_hash TYPE VARCHAR(64);
