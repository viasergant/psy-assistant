-- Owners of SOLO organizations (self-registered or solo bootstrap) were created with the THERAPIST
-- role but without a therapist_profile, so the JWT had no therapistProfileId and booking was impossible.
-- Create and link the missing profile. Idempotent.
INSERT INTO therapist_profile (email, name, employment_status, active, profile_completion_status,
                               org_id, user_id, created_by, updated_by)
SELECT u.email,
       COALESCE(NULLIF(u.full_name, ''), u.email),
       'ACTIVE', TRUE, 'COMPLETE',
       u.org_id, u.id, 'flyway-v90', 'flyway-v90'
FROM users u
JOIN organizations o ON o.id = u.org_id
JOIN user_roles r ON r.user_id = u.id AND r.role = 'THERAPIST'
WHERE o.type = 'SOLO'
  AND NOT EXISTS (SELECT 1 FROM therapist_profile tp WHERE LOWER(tp.email) = LOWER(u.email));
