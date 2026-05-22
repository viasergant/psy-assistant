-- V69: Link appointments and session records to a specific service catalog item
-- so that auto-generated invoices can use the exact product and price chosen at booking time.

ALTER TABLE appointment
    ADD COLUMN service_catalog_item_id UUID
        REFERENCES service_catalog(id) ON DELETE SET NULL;

ALTER TABLE session_record
    ADD COLUMN service_catalog_item_id UUID
        REFERENCES service_catalog(id) ON DELETE SET NULL;

COMMENT ON COLUMN appointment.service_catalog_item_id IS
    'Optional FK to service_catalog; when set the invoice generated on session completion uses this item''s price.';

COMMENT ON COLUMN session_record.service_catalog_item_id IS
    'Snapshot FK copied from the parent appointment at session record creation time.';
