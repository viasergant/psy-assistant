-- V52: PA-69 – Drop legacy service_type table (Phase 3)
-- By this point all FKs pointing to service_type have been removed (V50).

DROP TABLE IF EXISTS service_type;
