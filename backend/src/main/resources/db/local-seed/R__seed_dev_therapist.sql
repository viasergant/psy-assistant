-- R__seed_dev_therapist.sql
-- Local-development-only seed: creates a THERAPIST user and matching profile
-- so that session-note features can be tested without running the seed script.
--
-- Credentials:
--   Email:    therapist@psyassistant.local
--   Password: Admin1234!  (BCrypt cost-12, same hash as the admin seed user)
--
-- This file is a Flyway repeatable migration (R__) and is ONLY active when
-- the "db/local-seed" location is configured in application-local.yml.
-- It must NEVER be placed in db/migration or deployed to non-local environments.

INSERT INTO users (email, password_hash, full_name, role, active)
VALUES (
    'therapist@psyassistant.local',
    '$2a$12$bzum65qKv3b.9DQSwmhyaeCEOw4Djw6yGFRTQvL5Im9M9gIjVEha6',
    'Dev Therapist',
    'THERAPIST',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO therapist_profile (email, name, employment_status, active, created_by, updated_by)
VALUES (
    'therapist@psyassistant.local',
    'Dev Therapist',
    'ACTIVE',
    TRUE,
    'flyway-local-seed',
    'flyway-local-seed'
)
ON CONFLICT (email) DO NOTHING;
