-- R__seed_admin_user.sql
-- Local-development-only seed: creates the default SYSTEM_ADMINISTRATOR user so
-- that local dev has a working admin account on every clean boot.
--
-- Credentials:
--   Email:    admin@psyassistant.local
--   Password: Admin1234!  (BCrypt cost-12)
--
-- NOTE: V3__seed_admin_user.sql still runs in ALL environments (including production)
-- and cannot be modified (Flyway checksum protection).  V3 inserts the same email
-- against the now-removed users.role column; it is idempotent and harmless because
-- V68 already migrated all rows before dropping that column.
-- This file supersedes V3 for the LOCAL dev experience: it uses the current schema
-- (users.role column gone; roles now in user_roles junction table) and is only active
-- when the application-local.yml flyway.locations includes "classpath:db/local-seed".
--
-- This file is a Flyway repeatable migration (R__) so Flyway re-runs it on every boot.
-- Both INSERTs use ON CONFLICT DO NOTHING, making the file fully idempotent.

INSERT INTO users (email, password_hash, active, must_change_password, language,
                   failed_login_attempts, org_id, created_at, updated_at)
VALUES (
    'admin@psyassistant.local',
    '$2a$12$bzum65qKv3b.9DQSwmhyaeCEOw4Djw6yGFRTQvL5Im9M9gIjVEha6',
    TRUE,
    FALSE,
    'en',
    0,
    '00000000-0000-0000-0000-000000000001',
    NOW(),
    NOW()
)
ON CONFLICT (org_id, email) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'SYSTEM_ADMINISTRATOR'
FROM   users
WHERE  email = 'admin@psyassistant.local'
ON CONFLICT (user_id, role) DO NOTHING;
