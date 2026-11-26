-- R__seed_personal_therapist.sql
-- Local-development-only seed: a THERAPIST + RECEPTION_ADMIN_STAFF + FINANCE user with a linked profile, so the Telegram
-- digest/reminders (which resolve appointments through therapist_profile.user_id) work out of the box.
--
-- Credentials:
--   Email:    alphanet.vin@gmail.com
--   Password: 111  (BCrypt cost-12; intentionally weak, local development only)
--
-- Flyway repeatable migration, active ONLY when "db/local-seed" is in the flyway locations
-- (application-local.yml). Never copy it to db/migration. Fully idempotent.

INSERT INTO users (email, password_hash, full_name, active, must_change_password, language, org_id)
VALUES (
    'alphanet.vin@gmail.com',
    '$2a$12$4wzIL6BmySqZdvnol8opjeSduRhx0JRf3xIFcqkoA1at1zZEB61Qu',
    'Alphanet Vin',
    TRUE,
    FALSE,
    'en',
    '00000000-0000-0000-0000-000000000001'
)
ON CONFLICT (org_id, email) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, r.role
FROM users u
CROSS JOIN (VALUES ('THERAPIST'), ('RECEPTION_ADMIN_STAFF'), ('FINANCE')) AS r(role)
WHERE u.email = 'alphanet.vin@gmail.com'
  AND u.org_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO therapist_profile (email, name, employment_status, active, profile_completion_status,
                               org_id, created_by, updated_by)
VALUES (
    'alphanet.vin@gmail.com',
    'Alphanet Vin',
    'ACTIVE',
    TRUE,
    'COMPLETE',   -- skip the onboarding wizard for the local dev account
    '00000000-0000-0000-0000-000000000001',
    'flyway-local-seed',
    'flyway-local-seed'
)
ON CONFLICT (org_id, email) DO NOTHING;

-- Link profile to user (needed for Telegram schedule lookups); safe to re-run.
UPDATE therapist_profile tp
SET    user_id = u.id
FROM   users u
WHERE  tp.email = 'alphanet.vin@gmail.com'
  AND  tp.org_id = '00000000-0000-0000-0000-000000000001'
  AND  u.email = tp.email
  AND  u.org_id = tp.org_id
  AND  tp.user_id IS NULL;
