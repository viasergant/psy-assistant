-- V81: Remove service_catalog.category (free-text label that nothing read).
-- Name uniqueness was (org_id, name, category); it becomes (org_id, name).
-- Rows that share a name within an org (differing only by category) would violate the
-- new constraint, so they are first renamed to "<name> (<category>)".

UPDATE service_catalog sc
SET name = LEFT(sc.name || ' (' || sc.category || ')', 200)
WHERE EXISTS (
    SELECT 1 FROM service_catalog o
    WHERE o.org_id = sc.org_id AND o.name = sc.name AND o.id <> sc.id
);

ALTER TABLE service_catalog DROP CONSTRAINT IF EXISTS uq_catalog_org_name_cat;
ALTER TABLE service_catalog DROP COLUMN category;
ALTER TABLE service_catalog ADD CONSTRAINT uq_catalog_org_name UNIQUE (org_id, name);
