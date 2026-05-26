# Solo/Clinic Deployment Mode — Implementation Plan

## Overview

Add a solo/clinic deployment mode layer to the existing Spring Boot + Angular CRM. The application
currently has all domain features implemented. This plan adds a thin cross-cutting layer that:

1. Fails fast at startup if no mode profile is configured (`solo` or `clinic`)
2. Exposes `GET /api/v1/meta` so Angular can discover the mode at runtime
3. Creates the initial user automatically in solo mode (via `ApplicationRunner`) and via a
   one-time setup endpoint in clinic mode
4. Gates clinic-only controllers with `@Profile("clinic")` so they return HTTP 404 in solo mode
5. Provides Angular structural directive `*appIfMode` and `ModeGuard` so routes and UI elements
   can be conditionally rendered without per-component mode checks

**Architecture reference:** `_bmad-output/planning-artifacts/architecture.md` (decisions D1–D5,
patterns P1–P7). Any design question not re-stated below should be resolved by consulting that
document.

---

## Architecture Notes

### Key Pre-existing Conditions Found in Codebase

- `must_change_password` column already exists on `users` table (V18 migration). The `User` entity
  already has `mustChangePassword` / `isMustChangePassword()` / `setMustChangePassword()`. The
  architecture document calls this column `force_password_change` in V70 — that concept is already
  fulfilled by V18. **V70 is therefore a no-op column addition (safe `ADD COLUMN IF NOT EXISTS`)**
  added only to satisfy the architecture's sequence numbering and to document the confirmation.
- `mustChangePasswordGuard` already exists in `frontend/src/app/core/auth/guards/` and redirects
  to `/auth/first-login-password-change` — the Angular first-boot redirect is already wired.
- `AdminUserController` in `com.psyassistant.admin` is the primary clinic-only controller.
- `ReportController` (therapist utilization, caseload) and `CaseloadController` are clinic-only
  per the architecture.
- `LocalFlywayConfig` uses `@Profile("local")` — confirms the profile pattern.
- `TokenService.buildAuthorities()` computes JWT authorities from `User.getRoles()` — giving solo
  user all 5 roles automatically gives them all authorities without code changes.
- Spring Boot 3.x `ApplicationContextInitializer` registration is via
  `META-INF/spring/org.springframework.context.ApplicationContextInitializer.imports` (not
  `spring.factories`).

### Non-Negotiable Constraints

- `ErrorResponse record(Instant timestamp, int status, String error, String code, String path)` —
  never `ProblemDetail`. All new exception handlers in `GlobalExceptionHandler` must return
  `ResponseEntity<ErrorResponse>`.
- `@Profile("clinic")` on `@RestController` class only — never on `@Service` or `@Repository`.
- `AuditLogService.record()` called from service layer inside a `try-catch` that swallows failures.
- Flyway: `V{n}__snake_case.sql` with two underscores; V70+ for mode work; `R__*.sql` for seeds.
- Zero changes to any existing `@PreAuthorize` annotation.
- New Angular mode infrastructure lives in `src/app/core/mode/` only.

---

## Increment Summary

| # | Title | Status |
|---|-------|--------|
| 1 | V70 Migration — Confirm `must_change_password` Column | completed |
| 2 | Spring Profile Stubs and `DeploymentModeValidator` | pending |
| 3 | `AppMetaController` — `GET /api/v1/meta` | pending |
| 4 | `SoloUserInitRunner` — Solo First-Boot User Creation | pending |
| 5 | `ClinicSetupController` — Clinic First-Boot Admin Endpoint | pending |
| 6 | Move V3 Admin Seed to `local-seed/` | pending |
| 7 | Angular `core/mode/` Package | pending |
| 8 | Apply `@Profile("clinic")` to Clinic-Only Controllers | pending |
| 9 | Wire Angular Routes with `ModeGuard` and `*appIfMode` | pending |

---

## Increment 1 — V70 Migration: Confirm `must_change_password` Column

### Goal
Add V70 migration that confirms `must_change_password` already exists (safe `IF NOT EXISTS`) and
documents it as the `force_password_change` concept described in D1.2. This satisfies the
architecture's stated V70 requirement while being safe against the pre-existing V18 column.

### Files
**Create:**
- `backend/src/main/resources/db/migration/V70__add_force_password_change.sql`

### Tasks
1. Write a migration using `ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE`.
   Because the column already exists from V18, `IF NOT EXISTS` ensures the migration is a no-op.
   Add a `COMMENT ON COLUMN` to document the architecture's intent.

### Acceptance Criteria
- Migration file named exactly `V70__add_force_password_change.sql` (two underscores).
- Running the migration against a fresh DB (all V1–V69 applied) succeeds without error.
- Running the migration against a DB that already has V18 applied also succeeds without error.
- No changes to `User.java` entity (field already exists as `mustChangePassword`).

### Expected Output
- One new SQL migration file at V70.

---

## Increment 2 — Spring Profile Stubs and `DeploymentModeValidator`

### Goal
Create `application-solo.yml` and `application-clinic.yml` stub files, create the
`DeploymentModeValidator` that fails fast if neither `solo` nor `clinic` profile is active, and
register it via `META-INF/spring/org.springframework.context.ApplicationContextInitializer.imports`.

### Files
**Create:**
- `backend/src/main/resources/application-solo.yml`
- `backend/src/main/resources/application-clinic.yml`
- `backend/src/main/java/com/psyassistant/common/mode/DeploymentModeValidator.java`
- `backend/src/main/resources/META-INF/spring/org.springframework.context.ApplicationContextInitializer.imports`
- `backend/src/test/java/com/psyassistant/common/mode/DeploymentModeValidatorTest.java`

**Modify:**
- `backend/src/main/resources/application-local.yml` — ensure `flyway.locations` includes
  `classpath:db/local-seed` when the `local` profile is active (verify; add if missing).

### Tasks
1. Create `application-solo.yml` as an empty stub with a comment explaining it overrides
   `application.yml` defaults for solo mode. No values needed at v1.
2. Create `application-clinic.yml` as an empty stub with the same comment for clinic mode.
3. Implement `DeploymentModeValidator implements ApplicationContextInitializer<ConfigurableApplicationContext>`:
   - Read `environment.getActiveProfiles()`.
   - If neither `"solo"` nor `"clinic"` is present, throw `IllegalStateException` with the exact
     message: `"Deployment mode not configured. Start with --spring.profiles.active=solo or --spring.profiles.active=clinic"`.
   - If exactly one of `"solo"` or `"clinic"` is present, do nothing (valid).
   - Class lives in `com.psyassistant.common.mode`.
4. Register the class in
   `META-INF/spring/org.springframework.context.ApplicationContextInitializer.imports` with its
   fully-qualified class name `com.psyassistant.common.mode.DeploymentModeValidator`.
5. Write unit tests for `DeploymentModeValidator`:
   - `noModeProfile_throwsIllegalStateException`
   - `soloProfileActive_passes`
   - `clinicProfileActive_passes`
   - `bothSoloAndClinicActive_passes` (edge case — not rejected; first match wins)
6. Check `application-local.yml`; if `flyway.locations` is not already including
   `classpath:db/local-seed`, add it.

### Acceptance Criteria
- `DeploymentModeValidator` unit tests pass.
- Application started with `--spring.profiles.active=local` (no mode) fails with
  `IllegalStateException` at context initialisation — before any bean creation.
- Application started with `--spring.profiles.active=solo,local` starts successfully.
- Application started with `--spring.profiles.active=clinic,local` starts successfully.
- `DeploymentModeValidator` contains no `@Service`, `@Component`, or Spring bean annotations —
  it is a pure `ApplicationContextInitializer`.

### Dependencies
- Increment 1 (migration must be at V70 before the context starts successfully)

### Expected Output
- `DeploymentModeValidator` class ready for injection by `SoloUserInitRunner` (Increment 4).
- Profile stubs ready for future config overrides.

---

## Increment 3 — `AppMetaController`: `GET /api/v1/meta`

### Goal
Implement the unauthenticated `GET /api/v1/meta` endpoint that returns `{ "mode": "SOLO"|"CLINIC", "version": "<app-version>" }`.
Add `permitAll()` for `/api/v1/meta` and `/api/v1/setup/admin` in `SecurityConfig`.

### Files
**Create:**
- `backend/src/main/java/com/psyassistant/common/mode/AppMetaController.java`
- `backend/src/main/java/com/psyassistant/common/mode/AppMetaResponse.java`
- `backend/src/test/java/com/psyassistant/common/mode/AppMetaControllerTest.java`

**Modify:**
- `backend/src/main/java/com/psyassistant/common/config/SecurityConfig.java` — add
  `.requestMatchers("/api/v1/meta").permitAll()` and
  `.requestMatchers("/api/v1/setup/admin").permitAll()` before `anyRequest().authenticated()`.

### Tasks
1. Create `AppMetaResponse` record:
   ```java
   package com.psyassistant.common.mode;
   public record AppMetaResponse(String mode, String version) {}
   ```
2. Implement `AppMetaController`:
   - `@RestController`, no `@Profile` annotation (present in both modes).
   - `GET /api/v1/meta`, no `@PreAuthorize` (unauthenticated).
   - Reads active profiles from `Environment` to determine mode (`"SOLO"` or `"CLINIC"`).
   - Reads `spring.application.version` (or `build.version`) from `Environment`; falls back to
     `"unknown"` if not set.
   - Returns `AppMetaResponse`.
3. Update `SecurityConfig.filterChain()`: add the two new `permitAll()` matchers immediately after
   the existing `/api/v1/auth/**` matcher, before `anyRequest().authenticated()`.
4. Write `AppMetaControllerTest`:
   - Use `@WebMvcTest(AppMetaController.class)`.
   - `soloProfile_returnsSoloMode` — assert `mode == "SOLO"`.
   - `clinicProfile_returnsClinicMode` — assert `mode == "CLINIC"`.
   - `noAuth_returnsOk` — assert HTTP 200 without Authorization header.

### Acceptance Criteria
- `GET /api/v1/meta` returns HTTP 200 without authentication.
- Response body is `{ "mode": "SOLO", "version": "..." }` or `{ "mode": "CLINIC", "version": "..." }`.
- `GET /api/v1/meta` does not appear in `SecurityConfig` role restrictions.
- `POST /api/v1/setup/admin` path is added to `permitAll()` (for Increment 5).
- All `AppMetaControllerTest` tests pass.

### Dependencies
- Increment 2 (`DeploymentModeValidator` must be registered; tests require a valid profile)

### Expected Output
- Live unauthenticated endpoint for Angular `AppModeService` to call (Increment 7).

---

## Increment 4 — `SoloUserInitRunner`: Solo First-Boot User Creation

### Goal
Implement the `@Profile("solo")` `ApplicationRunner` that creates a single user from environment
variables `SOLO_USERNAME` (email) and `SOLO_PASSWORD` on first boot if no users exist. The user
gets all 5 roles and `mustChangePassword = true`.

### Files
**Create:**
- `backend/src/main/java/com/psyassistant/common/mode/SoloUserInitRunner.java`
- `backend/src/test/java/com/psyassistant/common/mode/SoloUserInitRunnerTest.java`

### Tasks
1. Implement `SoloUserInitRunner implements ApplicationRunner`:
   - Annotate with `@Component` and `@Profile("solo")`.
   - Inject `UserRepository`, `PasswordEncoder`, and (optionally) `AuditLogService`.
   - Read `SOLO_USERNAME` and `SOLO_PASSWORD` env vars (via `@Value("${SOLO_USERNAME:}")`).
   - In `run()`: if `userRepository.count() > 0`, log `event=SOLO_INIT_SKIPPED reason=users_exist`
     and return early (idempotent).
   - Validate that `SOLO_USERNAME` and `SOLO_PASSWORD` are non-blank; throw `IllegalStateException`
     if either is missing.
   - Create `User` with all 5 roles: `RECEPTION_ADMIN_STAFF`, `THERAPIST`, `SUPERVISOR`,
     `FINANCE`, `SYSTEM_ADMINISTRATOR`.
   - Set `mustChangePassword = true` on the created user.
   - Save via `userRepository.save(user)`.
   - Log `event=SOLO_USER_CREATED userId=<uuid>` (never log email, password, or username).
   - Wrap audit log in `try-catch` per P3 pattern:
     ```java
     try { auditLogService.record(entry); }
     catch (Exception ex) { log.warn("Audit log failed: {}", ex.getMessage()); }
     ```
2. Write unit tests:
   - `noUsersExist_createsUser` — assert user saved with all 5 roles and `mustChangePassword=true`.
   - `usersAlreadyExist_skips` — assert no save called when `userRepository.count() > 0`.
   - `missingEnvVars_throwsIllegalStateException` — assert failure when `SOLO_USERNAME` is blank.

### Acceptance Criteria
- Starting the app with `solo,local` profile and empty DB creates exactly one user with all 5 roles.
- Restarting with users already present logs `SOLO_INIT_SKIPPED` and creates no additional users.
- `SOLO_USERNAME` or `SOLO_PASSWORD` unset → `IllegalStateException` at startup with a clear message.
- User entity never directly sets a plain-text password in any log output.
- `SoloUserInitRunner` is NOT annotated with any `@Profile` other than `"solo"`.
- `UserManagementService` is NOT annotated with `@Profile` (service layer must remain mode-agnostic).

### Dependencies
- Increment 1 (V70 migration — `must_change_password` column confirmed)
- Increment 2 (`DeploymentModeValidator` must be registered for the solo profile to boot)

### Expected Output
- Solo first-boot flow complete.

---

## Increment 5 — `ClinicSetupController`: Clinic First-Boot Admin Endpoint

### Goal
Implement the `@Profile("clinic")` `POST /api/v1/setup/admin` endpoint that creates the initial
admin user when zero users exist. After first use, the endpoint is permanently disabled (returns
HTTP 409 if users already exist). This is the clinic equivalent of `SoloUserInitRunner`.

### Files
**Create:**
- `backend/src/main/java/com/psyassistant/common/mode/ClinicSetupController.java`
- `backend/src/main/java/com/psyassistant/common/mode/ClinicSetupRequest.java`
- `backend/src/test/java/com/psyassistant/common/mode/ClinicSetupControllerTest.java`

### Tasks
1. Create `ClinicSetupRequest` record:
   ```java
   public record ClinicSetupRequest(
       @NotBlank @Email String email,
       @NotBlank @Size(min=8) String password,
       String fullName
   ) {}
   ```
2. Implement `ClinicSetupController`:
   - `@RestController`, `@Profile("clinic")`, `@RequestMapping("/api/v1/setup")`.
   - No `@PreAuthorize` — publicly accessible (protected only by zero-user gate).
   - Inject `UserRepository`, `PasswordEncoder`, `AuditLogService`.
   - `POST /api/v1/setup/admin`:
     - If `userRepository.count() > 0`, return HTTP 409 with `ErrorResponse`:
       `code = "SETUP_ALREADY_COMPLETE"`, message `"Initial setup already completed"`.
     - Validate request body via `@Valid`.
     - Create user with role `SYSTEM_ADMINISTRATOR`, `mustChangePassword = false`,
       `active = true`.
     - Save via `userRepository.save(user)`.
     - Wrap audit log in `try-catch` per P3.
     - Return HTTP 201 with `{ "id": "<uuid>" }`.
3. Ensure `GlobalExceptionHandler` handles `MethodArgumentNotValidException` for request
   validation — confirm it already does (it should from existing code); add if missing.
4. Write `ClinicSetupControllerTest`:
   - `noUsersExist_createsAdmin_returns201`.
   - `usersAlreadyExist_returns409WithErrorResponse`.
   - `invalidEmail_returns400WithErrorResponse`.
   - `noAuth_requestAllowed` — confirm endpoint is not blocked by `SecurityConfig`.

### Acceptance Criteria
- `POST /api/v1/setup/admin` with valid body and zero users → HTTP 201, user created with
  `SYSTEM_ADMINISTRATOR` role.
- Second call with users present → HTTP 409 with `ErrorResponse` (not `ProblemDetail`).
- Endpoint returns HTTP 404 when started with `solo` profile (`@Profile("clinic")` not active).
- Request validation errors return HTTP 400 with `ErrorResponse`.
- `ClinicSetupController` is NOT annotated with any service-layer `@Profile`.

### Dependencies
- Increment 1 (V70 migration)
- Increment 2 (`DeploymentModeValidator`, profile stubs)
- Increment 3 (`SecurityConfig` already has `permitAll()` for `/api/v1/setup/admin`)

### Expected Output
- Clinic first-boot flow complete.

---

## Increment 6 — Move V3 Admin Seed to `local-seed/`

### Goal
Move the static admin user seed data from `V3__seed_admin_user.sql` (a versioned Flyway migration
that runs in all environments) to a new repeatable local-only seed file
`R__seed_admin_user.sql`. The original V3 file cannot be modified (Flyway checksum protection);
instead, the seed data is replicated to the new location so local dev continues to have a default
admin account, while production boots cleanly without hardcoded credentials.

### Files
**Create:**
- `backend/src/main/resources/db/local-seed/R__seed_admin_user.sql`

**Modify:**
- `backend/src/main/resources/application-local.yml` — confirm/add `flyway.locations` includes
  `classpath:db/local-seed` (prerequisite from Increment 2, verify here).

### Tasks
1. Create `R__seed_admin_user.sql` in `db/local-seed/` with the same `INSERT ... ON CONFLICT DO NOTHING`
   as V3, but targeting the current `user_roles` junction table structure (V68 dropped the `role`
   column from `users`). The seed must:
   - Insert the admin user into `users` (email, password_hash, active, must_change_password=false,
     language, created_at, updated_at, failed_login_attempts=0).
   - Insert the corresponding row into `user_roles` (user_id, role='SYSTEM_ADMINISTRATOR').
   - Use `ON CONFLICT DO NOTHING` on both inserts (idempotent).
2. Confirm (or add) in `application-local.yml`:
   ```yaml
   spring:
     flyway:
       locations: classpath:db/migration,classpath:db/local-seed
   ```
3. Note in a SQL comment that V3 still runs in all environments (cannot be removed/modified) and
   that this file supersedes it for the local dev experience.

### Acceptance Criteria
- Local startup (with `local` profile) creates the admin user from `R__seed_admin_user.sql`.
- The seed is idempotent — running it twice does not create duplicate rows.
- Production startup (without `local` profile) does NOT run `R__seed_admin_user.sql`.
- The V3 migration file is NOT modified.
- The inserted admin user can log in at `admin@psyassistant.local` / `Admin1234!`.

### Dependencies
- Increment 2 (local Flyway config must include `local-seed` location)

### Expected Output
- Local dev workflow restored: admin user available on every clean local boot.

---

## Increment 7 — Angular `core/mode/` Package

### Goal
Create the Angular mode infrastructure: `AppModeService` (singleton, `APP_INITIALIZER`),
`*appIfMode` structural directive, and `ModeGuard`. These are the client-side equivalents of
`DeploymentModeValidator` and `@Profile("clinic")`.

### Files
**Create:**
- `frontend/src/app/core/mode/app-mode.service.ts`
- `frontend/src/app/core/mode/app-mode.service.spec.ts`
- `frontend/src/app/core/mode/app-if-mode.directive.ts`
- `frontend/src/app/core/mode/app-if-mode.directive.spec.ts`
- `frontend/src/app/core/mode/mode.guard.ts`
- `frontend/src/app/core/mode/mode.guard.spec.ts`

**Modify:**
- `frontend/src/app/app.config.ts` — register `APP_INITIALIZER` for `AppModeService.init()`.

### Tasks
1. Create `AppModeService`:
   - `@Injectable({ providedIn: 'root' })`.
   - Private `BehaviorSubject<'SOLO' | 'CLINIC' | null>` named `modeSubject`.
   - Public `mode$: Observable<'SOLO' | 'CLINIC' | null>` = `modeSubject.asObservable()`.
   - Public getter `mode: 'SOLO' | 'CLINIC' | null` returning `modeSubject.getValue()`.
   - `init(): Promise<void>` — calls `GET /api/v1/meta` via `HttpClient`, sets `modeSubject`,
     swallows errors (returns resolved promise on failure so boot is not blocked).
   - `AppModeService` is the ONLY Angular class that calls `GET /api/v1/meta`.
2. Create `*appIfMode` structural directive (`AppIfModeDirective`):
   - Selector: `[appIfMode]`.
   - Input: `appIfMode: 'SOLO' | 'CLINIC'`.
   - Injects `AppModeService` and `TemplateRef`, `ViewContainerRef`.
   - Shows template content when `AppModeService.mode === appIfMode`; clears otherwise.
   - Re-evaluates on `mode$` emission (subscribe in constructor or `ngOnInit`).
3. Create `ModeGuard`:
   - Implements `CanActivateFn`.
   - Factory function: `modeGuard(requiredMode: 'SOLO' | 'CLINIC'): CanActivateFn`.
   - Returns `true` if `AppModeService.mode === requiredMode`; redirects to `/dashboard`
     (or `/` if dashboard doesn't exist) otherwise.
4. Register `AppModeService.init()` in `app.config.ts` as an additional `APP_INITIALIZER`:
   ```typescript
   {
     provide: APP_INITIALIZER,
     useFactory: (svc: AppModeService) => () => svc.init(),
     deps: [AppModeService],
     multi: true
   }
   ```
5. Write unit tests (follow existing guard spec patterns in `core/auth/guards/`):
   - `AppModeService`: `init_setsMode_fromApiResponse`, `init_onError_modeRemainsNull`.
   - `AppIfModeDirective`: `matchingMode_rendersTemplate`, `nonMatchingMode_clearsTemplate`.
   - `ModeGuard`: `correctMode_returnsTrue`, `wrongMode_redirectsToDashboard`.

### Acceptance Criteria
- `AppModeService.mode` is `'SOLO'` or `'CLINIC'` after `APP_INITIALIZER` resolves.
- `*appIfMode="'clinic'"` hides content in solo mode and shows it in clinic mode.
- `ModeGuard` blocks navigation to clinic-only routes in solo mode.
- `AppModeService` is never imported inside any feature `*Service` class (mode conditioning
  belongs in templates and guards only).
- All spec tests pass.

### Dependencies
- Increment 3 (`GET /api/v1/meta` endpoint must be live)

### Expected Output
- Angular mode infrastructure ready for route and template wiring (Increment 9).

---

## Increment 8 — Apply `@Profile("clinic")` to Clinic-Only Controllers

### Goal
Annotate the backend controllers that must be absent in solo mode with `@Profile("clinic")`.
Verify that all service/repository classes they call are NOT annotated with `@Profile`.

### Files
**Modify:**
- `backend/src/main/java/com/psyassistant/admin/AdminUserController.java` — add `@Profile("clinic")`
- `backend/src/main/java/com/psyassistant/reporting/caseload/CaseloadController.java` — add `@Profile("clinic")`
- `backend/src/main/java/com/psyassistant/reporting/reports/ReportController.java` — add `@Profile("clinic")` for therapist utilization endpoint (see note below)

### Notes on `ReportController`
The architecture specifies that `THERAPIST_UTILIZATION` and caseload overview reports are
clinic-only; revenue, lead conversion, client retention, and no-show reports are available in both
modes. `ReportController` currently handles all 5 report types in a single controller class.

Two options per D3.2:
- **Option A (preferred):** Split `ReportController` into `ReportController` (both modes, 4 report
  types) and a new `ClinicReportController` (`@Profile("clinic")`, therapist-utilization only).
  This keeps the "one class = one mode visibility" principle clean.
- **Option B (acceptable for v1):** Keep `ReportController` in both modes but have the Angular
  `ModeGuard` guard the therapist-utilization route. The backend endpoint returns data in solo mode
  but the UI never calls it.

This increment implements **Option B** for minimal blast radius: annotate only `AdminUserController`
and `CaseloadController` with `@Profile("clinic")` and rely on the Angular guard for the utilization
report. Leave a `TODO` comment in `ReportController` for a later increment.

### Tasks
1. Add `@Profile("clinic")` to `AdminUserController` class declaration.
2. Add `@Profile("clinic")` to `CaseloadController` class declaration.
3. Add `// TODO: split THERAPIST_UTILIZATION endpoint to ClinicReportController` comment to
   `ReportController`.
4. Confirm that `UserManagementService`, `CaseloadService`, `CaseloadQueryService` are NOT
   annotated with `@Profile` (they must not be — verify and fix if any exist).
5. Write or update integration tests:
   - `AdminUserControllerTest`: add `@ActiveProfiles("clinic,local")` assertion that endpoints
     return HTTP 404 when the test runs without the `clinic` profile.
   - Note: existing `AdminUserControllerTest` tests should still pass with `clinic,local` profiles.

### Acceptance Criteria
- `GET /api/v1/admin/users` returns HTTP 404 when started with `solo,local` profile.
- `GET /api/v1/caseload` returns HTTP 404 when started with `solo,local` profile.
- Both endpoints return their normal responses when started with `clinic,local` profile.
- No `@Service` or `@Repository` class has `@Profile("clinic")` annotation.
- `AdminUserControllerTest` passes with `clinic,local` profiles.

### Dependencies
- Increment 2 (`DeploymentModeValidator` must be registered so profile-based tests can start the context)

### Expected Output
- Clinic-only backend endpoints properly absent in solo mode.

---

## Increment 9 — Wire Angular Routes and UI with `ModeGuard` and `*appIfMode`

### Goal
Apply `ModeGuard` to clinic-only Angular routes and use `*appIfMode` directive in the shell
navigation so menu items and section-level UI elements are correctly hidden/shown by mode.

### Files
**Modify:**
- `frontend/src/app/app.routes.ts` — add `ModeGuard('clinic')` to `admin` and `reports`
  (therapist-utilization sub-route).
- `frontend/src/app/layout/shell/shell.component.*` — apply `*appIfMode="'clinic'"` to
  clinic-only nav items (admin, multi-therapist views).
- `frontend/src/app/features/reports/reports.routes.ts` (if exists) — add `ModeGuard('clinic')`
  to `therapist-utilization` and `caseload-overview` child routes.

### Tasks
1. In `app.routes.ts`, add `modeGuard('clinic')` to the `admin` route's `canActivate` array.
2. In `reports.routes.ts`, wrap `therapist-utilization` and `caseload-overview` routes with
   `canActivate: [modeGuard('clinic')]`.
3. In `shell.component.html`, wrap clinic-only menu items (Admin, multi-therapist schedule view,
   utilization reports) with `*appIfMode="'clinic'"`.
4. Confirm that the default redirect logic in `app.routes.ts` still works in solo mode — the
   first visible route should be accessible to the solo user.
5. Verify `ModeGuard` is never imported inside feature service classes.

### Acceptance Criteria
- Navigating to `/admin` in solo mode redirects to `/` (or equivalent dashboard).
- Navigating to `/reports/therapist-utilization` in solo mode redirects.
- Admin nav item is absent from the shell in solo mode.
- All existing route guard tests pass.
- No feature `*Service` class imports `AppModeService`.

### Dependencies
- Increment 7 (`ModeGuard` and `AppModeService` must exist)
- Increment 8 (backend endpoints already absent — UI guards follow the API boundary)

### Expected Output
- Full end-to-end solo/clinic mode gating implemented on frontend.

---

## Risks

| Risk | Mitigation |
|------|------------|
| `DeploymentModeValidator` breaks existing tests that start a full Spring context without mode profile | Use `@ActiveProfiles("solo,local")` or `@ActiveProfiles("clinic,local")` on integration tests; unit tests using `@WebMvcTest` are not affected |
| V3 still runs in production and inserts a hardcoded admin | V3 is idempotent (`ON CONFLICT DO NOTHING`); solo mode's `SoloUserInitRunner` checks `count() > 0` and skips if V3 has already seeded a user; clinic mode's setup endpoint similarly checks `count() > 0` |
| `LocalFlywayConfig` drops schema on `local` boot — new `local-seed/` files must be Flyway-compatible | Confirm `flyway.locations` includes both paths; the DROP+recreate strategy in `LocalFlywayConfig` runs all migrations including seeds on every boot |
| `AppMetaController` returns incorrect mode if both `solo` and `clinic` profiles are accidentally active | `DeploymentModeValidator` only rejects the absence of both profiles; having both is technically allowed. Implement mode detection as "first wins" in priority order (`solo` before `clinic`) and log a warning |
| Angular `APP_INITIALIZER` for `AppModeService` fails network call | Errors are swallowed in `init()` so the app still boots; UI defaults to showing all content (safer than blocking boot) |

---

## Success Criteria

1. `./mvnw spring-boot:run -Dspring-boot.run.profiles=solo,local` — starts, creates solo user,
   JWT for that user contains all 5 roles, password change is required.
2. `./mvnw spring-boot:run -Dspring-boot.run.profiles=clinic,local` — starts, `POST /api/v1/setup/admin`
   creates first admin, subsequent calls return 409.
3. `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (no mode) — fails with
   `IllegalStateException` before any bean is created.
4. `GET /api/v1/meta` returns `{ "mode": "SOLO" }` in solo mode and `{ "mode": "CLINIC" }` in
   clinic mode, without authentication.
5. `GET /api/v1/admin/users` returns HTTP 404 in solo mode.
6. Angular app: admin nav item absent in solo mode; present in clinic mode.
7. All existing tests pass with `@ActiveProfiles("solo,local")` or `@ActiveProfiles("clinic,local")`.
