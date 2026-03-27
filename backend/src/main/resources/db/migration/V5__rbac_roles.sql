-- V5: RBAC — migrate legacy roles to scoped roles and enforce valid values.
--
-- ADMIN  → SYSTEM_ADMINISTRATOR  (full system management)
-- USER   → THERAPIST             (therapist-scoped access)
--
-- A CHECK constraint is then added so that only the five scoped role names
-- are accepted for new and updated rows going forward.

UPDATE users SET role = 'SYSTEM_ADMINISTRATOR' WHERE role = 'ADMIN';
UPDATE users SET role = 'THERAPIST'            WHERE role = 'USER';

ALTER TABLE users ADD CONSTRAINT chk_user_role
    CHECK (role IN (
        'RECEPTION_ADMIN_STAFF',
        'THERAPIST',
        'SUPERVISOR',
        'FINANCE',
        'SYSTEM_ADMINISTRATOR'
    ));
