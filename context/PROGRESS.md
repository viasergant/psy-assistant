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
| 3 | `AppMetaController` — `GET /api/v1/meta` | complete | |
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

---

## 2026-05-26 — Increment 3: `AppMetaController` — `GET /api/v1/meta`

- **What was completed:** Created `AppMetaResponse` record, `AppMetaController` (unauthenticated `GET /api/v1/meta`), and `AppMetaControllerTest` with 4 test cases; updated `SecurityConfig` to `permitAll()` for `/api/v1/meta` and `/api/v1/setup/admin`.
- **Interfaces/methods created:**
  - `AppMetaResponse(String mode, String version)` — response record in `com.psyassistant.common.mode`
  - `AppMetaController.getMeta(): AppMetaResponse` — `GET /api/v1/meta`, no auth required, reads active profiles from `Environment`; `solo` wins over `clinic` when both are active
- **Files created/modified:**
  - Created: `backend/src/main/java/com/psyassistant/common/mode/AppMetaResponse.java`
  - Created: `backend/src/main/java/com/psyassistant/common/mode/AppMetaController.java`
  - Created: `backend/src/test/java/com/psyassistant/common/mode/AppMetaControllerTest.java`
  - Modified: `backend/src/main/java/com/psyassistant/common/config/SecurityConfig.java` — added two `permitAll()` matchers for `/api/v1/meta` and `/api/v1/setup/admin` immediately after the existing `/api/v1/auth/**` matcher
- **Decisions made:**
  - Test uses static nested `@WebMvcTest` classes (`SoloProfileTest`, `ClinicProfileTest`) with distinct `@ActiveProfiles` to exercise mode detection, since `Environment` is a core Spring bean that cannot be mocked via `@MockitoBean`. Surefire discovers these as separate test classes.
  - The outer `AppMetaControllerTest` class (active profile: `test` only) verifies the default-to-SOLO fallback and the `permitAll()` rule (no Authorization header required).
  - Version property read order: `spring.application.version` → `build.version` → `"unknown"`.
- **Tests:** 522 passing / 0 failures (4 new tests; zero regressions)

---

## 2026-05-26 — Test Run (Increment 3, attempt 1)
- Passed: 522 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured)
- Coverage gate: N/A — no JaCoCo plugin in pom.xml
- Duration: ~12.8s
- Failures: none
- Security regression check: SecurityConfigTest (2), RbacIntegrationTest (13), AdminUserControllerTest (21), ActuatorSecurityTest (3), AppMetaControllerTest (2) — all 41 pass; the two new permitAll() matchers caused zero regressions in existing auth/security tests
- Action: all clear — Increment 3 fully green

---

## 2026-05-26 — Code Review (Increment 3)
- Quality: FAIL (Critical: 0, High: 1, Medium: 1)
- Coverage: FULLY COVERED
- Recommendation: fix and re-review

## 2026-05-26 — Code Review Fix (Increment 3): Nested test classes extracted

- **Finding fixed (HIGH):** Maven Surefire does not discover nested static `@WebMvcTest` classes by default; `SoloProfileTest` and `ClinicProfileTest` were silently skipped.
- **Fix:** Extracted both nested classes to standalone top-level test files:
  - Created: `backend/src/test/java/com/psyassistant/common/mode/AppMetaControllerSoloTest.java` (`@ActiveProfiles({"solo","test"})`, 1 test)
  - Created: `backend/src/test/java/com/psyassistant/common/mode/AppMetaControllerClinicTest.java` (`@ActiveProfiles({"clinic","test"})`, 1 test)
  - Modified: `backend/src/test/java/com/psyassistant/common/mode/AppMetaControllerTest.java` — removed nested static classes; outer class retains its 2 tests unchanged
- **Tests:** 524 passing / 0 failures (count increased from 522 to 524 — 2 previously-skipped tests now discovered)

## 2026-05-26 — Code Review Re-check (Increment 3)
- Quality: PASS (Critical: 0, High: 0)
- Coverage: FULLY COVERED
- Recommendation: approve
- Notes: HIGH finding resolved — nested static test classes extracted to top-level `AppMetaControllerSoloTest` and `AppMetaControllerClinicTest`; all 4 tests now Surefire-discoverable; profile assertions and outer-class integrity confirmed; Medium finding (two-pass profile scan) accepted as-is for v1
