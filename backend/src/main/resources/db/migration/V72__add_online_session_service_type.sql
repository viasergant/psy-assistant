-- V72: Extend service_type lookup and package definitions with ONLINE_SESSION
-- service type, distinguishing online individual sessions from in-person ones.

-- No schema changes required — service_type is stored as VARCHAR.
-- Only seed data and comment updates are needed.

COMMENT ON COLUMN prepaid_package_definition.service_type IS
    'Billing service type: INDIVIDUAL_SESSION, ONLINE_SESSION, GROUP_SESSION, INTAKE_ASSESSMENT, FOLLOW_UP';
