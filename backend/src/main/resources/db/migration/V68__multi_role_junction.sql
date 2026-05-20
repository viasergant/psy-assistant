-- V68: Multi-role support — replace users.role column with user_roles junction table.
--
-- Steps:
--   1. Drop the chk_user_role CHECK constraint added by V5.
--   2. Create user_roles junction table with its own CHECK constraint.
--   3. Migrate existing single-role values into user_roles, converting any
--      legacy ADMIN / USER aliases that V5 may not have caught.
--   4. Drop the now-redundant users.role column.
--   5. Create a supporting index on user_roles.role.

-- -----------------------------------------------------------------------
-- 1. Drop the CHECK constraint that guarded users.role
-- -----------------------------------------------------------------------
ALTER TABLE users DROP CONSTRAINT chk_user_role;

-- -----------------------------------------------------------------------
-- 2. Create the junction table
-- -----------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL CHECK (role IN (
                'RECEPTION_ADMIN_STAFF',
                'THERAPIST',
                'SUPERVISOR',
                'FINANCE',
                'SYSTEM_ADMINISTRATOR'
            )),
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role)
);

-- -----------------------------------------------------------------------
-- 3. Pre-flight: abort migration if any user has an unrecognised role value.
--    All role values must be mappable before we drop the column.
-- -----------------------------------------------------------------------
DO $$
DECLARE
    bad_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO bad_count
    FROM   users
    WHERE  role NOT IN (
               'RECEPTION_ADMIN_STAFF', 'THERAPIST', 'SUPERVISOR',
               'FINANCE', 'SYSTEM_ADMINISTRATOR', 'ADMIN', 'USER'
           );
    IF bad_count > 0 THEN
        RAISE EXCEPTION 'V68 migration aborted: % user(s) have unrecognised role values. Fix these rows before re-running the migration.', bad_count;
    END IF;
END $$;

-- -----------------------------------------------------------------------
-- Migrate data from users.role → user_roles
--    Handle any stale legacy aliases (ADMIN, USER) that V5 may have missed.
-- -----------------------------------------------------------------------
INSERT INTO user_roles (user_id, role)
SELECT id,
       CASE role
           WHEN 'ADMIN' THEN 'SYSTEM_ADMINISTRATOR'
           WHEN 'USER'  THEN 'THERAPIST'
           ELSE role
       END
FROM   users
WHERE  role IN (
           'RECEPTION_ADMIN_STAFF',
           'THERAPIST',
           'SUPERVISOR',
           'FINANCE',
           'SYSTEM_ADMINISTRATOR',
           'ADMIN',
           'USER'
       );

-- -----------------------------------------------------------------------
-- 4. Drop the role column from users
-- -----------------------------------------------------------------------
ALTER TABLE users DROP COLUMN role;

-- -----------------------------------------------------------------------
-- 5. Supporting index for the role-filter query
--    (e.g. GET /api/v1/admin/users?role=THERAPIST)
-- -----------------------------------------------------------------------
CREATE INDEX idx_user_roles_role ON user_roles(role);
