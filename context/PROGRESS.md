# Solo/Clinic Deployment Mode — Progress Tracker

## 2026-05-26 — Planning Complete

**Architecture source:** `_bmad-output/planning-artifacts/architecture.md`
**Plan:** `context/PLAN.md`
**Total increments:** 9

---

## Increment Status

| # | Title | Status | Notes |
|---|-------|--------|-------|
| 1 | V70 Migration — Confirm `must_change_password` Column | complete | `must_change_password` already exists from V18; V70 is a safe no-op `ADD COLUMN IF NOT EXISTS` |
| 2 | Spring Profile Stubs and `DeploymentModeValidator` | complete | |
| 3 | `AppMetaController` — `GET /api/v1/meta` | pending | |
| 4 | `SoloUserInitRunner` — Solo First-Boot User Creation | pending | |
| 5 | `ClinicSetupController` — Clinic First-Boot Admin Endpoint | pending | |
| 6 | Move V3 Admin Seed to `local-seed/` | pending | |
| 7 | Angular `core/mode/` Package | pending | |
| 8 | Apply `@Profile("clinic")` to Clinic-Only Controllers | pending | |
| 9 | Wire Angular Routes and UI with `ModeGuard` and `*appIfMode` | pending | |

---

---

## 2026-05-26 — Increment 1: V70 Migration — Confirm `must_change_password` Column

- **What was completed:** Created `V70__add_force_password_change.sql` as a safe no-op migration that confirms the `must_change_password` column exists and updates its `COMMENT ON COLUMN` to document the architectural role (D1.2 force_password_change concept).
- **Interfaces/methods created:** None (SQL migration only)
- **Files created/modified:**
  - Created: `backend/src/main/resources/db/migration/V70__add_force_password_change.sql`
  - Modified: `context/PROGRESS.md` (this update)
- **Decisions made:**
  - V18 already uses `ADD COLUMN IF NOT EXISTS must_change_password`, so V70 runs the identical `ADD COLUMN IF NOT EXISTS` statement — confirmed it is safe to run against a DB that already has V18 applied (PostgreSQL silently skips the `IF NOT EXISTS` clause).
  - The `COMMENT ON COLUMN` in V70 supersedes the V18 comment with the architectural description from the plan spec.
- **Tests:** No tests required — pure SQL DDL migration with no business logic.

---

## Key Codebase Facts Discovered During Planning

- `must_change_password` column exists from **V18** (not missing; architecture's V70 is a safe no-op)
- `mustChangePasswordGuard` already in `frontend/src/app/core/auth/guards/` — redirects to `/auth/first-login-password-change`
- `User.isMustChangePassword()` / `setMustChangePassword()` already implemented
- `TokenService.buildAuthorities()` unions ROLE_X + Permission names from all roles — solo all-5-roles gives full authority set automatically
- `AdminUserController` at `com.psyassistant.admin` is the primary clinic-only controller
- `CaseloadController` at `com.psyassistant.reporting.caseload` is clinic-only
- `ReportController` handles all 5 report types in one class — therapist-utilization is clinic-only but splitting is deferred (Option B: Angular guard only)
- `META-INF/spring/` directory does not yet exist — must be created for `DeploymentModeValidator` registration
- `LocalFlywayConfig` (`@Profile("local")`) drops and recreates the schema on every local boot
- `application-local.yml` and `application-prod.yml` exist; `application-solo.yml` and `application-clinic.yml` do not yet exist
- V3 migration seeds admin user to `users` table with old `role` column (pre-V68); local seed must use new `user_roles` junction table structure

---

---

## 2026-05-26 — Increment 2: Spring Profile Stubs and `DeploymentModeValidator`

- **What was completed:** Created `application-solo.yml` and `application-clinic.yml` stub files; implemented `DeploymentModeValidator` as a pure `ApplicationContextInitializer` with no Spring annotations; registered it via the Boot 3.x imports file; wrote 7 unit tests covering all acceptance criteria cases.
- **Interfaces/methods created:**
  - `DeploymentModeValidator.initialize(ConfigurableApplicationContext)` — throws `IllegalStateException` with exact message if neither `solo` nor `clinic` profile is active; no-op otherwise.
- **Files created/modified:**
  - Created: `backend/src/main/resources/application-solo.yml`
  - Created: `backend/src/main/resources/application-clinic.yml`
  - Created: `backend/src/main/java/com/psyassistant/common/mode/DeploymentModeValidator.java`
  - Created: `backend/src/main/resources/META-INF/spring/org.springframework.context.ApplicationContextInitializer.imports`
  - Created: `backend/src/test/java/com/psyassistant/common/mode/DeploymentModeValidatorTest.java`
  - Not modified: `backend/src/main/resources/application-local.yml` — `flyway.locations` already included `classpath:db/local-seed` from a prior change.
- **Decisions made:**
  - Error message spans three concatenated string literals in source to satisfy the 120-char line limit enforced by checkstyle; the runtime value is identical to the spec's required string.
  - Test method names use pure camelCase to comply with the project checkstyle `MethodName` rule (`^[a-z][a-zA-Z0-9]*$`). The spec's `shouldExpectedBehavior_whenCondition` pattern with underscores is incompatible with the project's checkstyle config.
  - Import order follows the project convention: static imports (alphabetically sorted) with no blank line separator, then regular imports alphabetically — no blank line between groups.
- **Tests:** 7 passing / 0 failures (full suite: 520 passing / 0 failures)

---

## 2026-05-26 — Code Review (Increment 1)
- Quality: PASS (Critical: 0, High: 0)
- Coverage: FULLY COVERED
- Recommendation: approve

---

## 2026-05-26 — Test Run (Increment 1, attempt 1)
- Passed: 513 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured)
- Coverage gate: N/A — no JaCoCo plugin in pom.xml
- Duration: ~12s
- Failures: none
- Action: all clear — V70 migration file addition caused zero regressions; full suite green

---

## 2026-05-26 — Code Review (Increment 2)
- Quality: PASS (Critical: 0, High: 0)
- Coverage: FULLY COVERED
- Recommendation: approve

---

## 2026-05-26 — Test Run (Increment 2, attempt 1)
- Passed: 520 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured)
- Coverage gate: N/A — no JaCoCo plugin in pom.xml
- Duration: ~11s (two runs; 11.395s on second run)
- Failures: none
- Action: all clear — DeploymentModeValidator and 7 new unit tests pass; zero regressions across all @SpringBootTest and @WebMvcTest classes
