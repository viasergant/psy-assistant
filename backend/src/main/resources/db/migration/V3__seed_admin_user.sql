-- V3: Seed a default admin user for local development.
--
-- Password: Admin1234!   (BCrypt cost 12)
-- Change this immediately in any non-local environment.
-- The INSERT is idempotent: it does nothing if the email already exists.

INSERT INTO users (email, password_hash, role, active)
VALUES (
    'admin@psyassistant.local',
    '$2a$12$bzum65qKv3b.9DQSwmhyaeCEOw4Djw6yGFRTQvL5Im9M9gIjVEha6',
    'ADMIN',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

