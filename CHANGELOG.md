# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — 2026-05-20

### Added

- **Multi-role user support** — a single CRM user can now hold multiple roles simultaneously (e.g., THERAPIST + SUPERVISOR); the union of permissions for all assigned roles is reflected in the JWT and enforced at every endpoint
- **`user_roles` junction table** (V68 migration) — replaces the single `users.role` column; existing role data migrated automatically with legacy alias normalisation; pre-flight check aborts migration if unrecognised role values are present
- **Multi-role JWT authority building** — `TokenService` emits all `ROLE_X` values and the full permission union for every assigned role; admin TTL applies if any assigned role is `SYSTEM_ADMINISTRATOR`
- **Admin UI multi-role assignment** — create/edit user dialogs replaced single-role `<select>` with a checkbox group; at least one role must remain selected; the user list table displays all assigned roles
- **`RoleValidationException`** — typed domain exception (HTTP 400, code `ROLE_VALIDATION_ERROR`) for role validation failures on the therapist creation endpoint, replacing a generic `IllegalArgumentException`
- **RBAC integration tests** — dual-role (THERAPIST + SUPERVISOR) JWT verified to grant therapist access, grant supervisor-only `READ_TEAM_WORKLOAD` access, and be denied admin endpoint access (AC8/AC9/AC10)

### Changed

- `POST /api/v1/admin/users` and `PATCH /api/v1/admin/users/{id}` now accept `roles: [...]` (array) instead of `role` (single value); the deprecated `role` field is still included in responses for backward compatibility
- `GET /api/v1/admin/users?role=THERAPIST` now matches users that have THERAPIST among their roles (not just as their sole role)
- `POST /api/v1/admin/users/therapists` accepts requests where roles include THERAPIST plus additional roles (e.g., `[THERAPIST, SUPERVISOR]`)
- `PermissionService` (Angular) now collects all `ROLE_X` entries from the JWT rather than only the first, enabling correct permission resolution for multi-role users
- Schedule guard and client-detail component updated to use array role checks (`.includes()`) so a multi-role user is not misidentified

### Fixed

- Pagination count queries on `GET /api/v1/admin/users?role=X` now return the correct `totalPages` — the previous JOIN-based filter produced duplicate rows under some JPA providers; replaced with a correlated EXISTS subquery

---

## [Unreleased] — 2026-05-13

### Added

- **Risk flag subsystem** — full backend and frontend implementation of structured, role-protected risk flag indicators on client profiles (PA-27)
- **DB migration V67** — three new tables: `risk_flag_types` (configurable flag types with soft-delete), `client_risk_flags` (per-client flags with status, clinical note, review date, resolve workflow), and `risk_flag_audit_log` (append-only audit trail with no FK on `flag_id`, matching the `care_plan_audit` pattern)
- **Five seed risk flag types**: Self-Harm Risk, Crisis History, Safeguarding Concern, Domestic Abuse Concern, Suicidal Ideation
- **Four new permissions**: `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`, `MANAGE_RISK_FLAG_TYPES` — propagate automatically to JWTs via existing `TokenService`
- **`RiskFlagService`** — create, resolve, list (active and all) with THERAPIST assignment enforcement, clinical note stripping for callers lacking `READ_RISK_FLAG_NOTES`, and N+1-safe batch flag type name resolution
- **`RiskFlagTypeService`** — list active types (all authenticated staff), full admin CRUD with deterministic `displayOrder`-sorted listing
- **REST endpoints** under `/api/v1/clients/{clientId}/risk-flags` (4 endpoints) and `/api/v1/risk-flag-types` + `/api/v1/admin/risk-flag-types` (4 endpoints)
- **`AppointmentResponse`** extended with `activeRiskFlagTypes: List<String>` — populated only on single-appointment GET (detail view), not on list endpoints
- **`AppointmentMapper`** split into `toDetailResponse()` (enriched, batch-safe) and `toResponse()` (slim, `List.of()`) to avoid N+1 on calendar/list views
- **Frontend risk flag feature module** at `features/clients/components/risk-flags/` — `RiskFlagsPanelComponent`, `RiskFlagFormDialogComponent`, `RiskFlagResolveDialogComponent` with reactive forms, role-gated clinical note display, and red chip indicators
- **Risk flag panel** integrated into `ClientDetailComponent` with `MANAGE_RISK_FLAGS` and `READ_RISK_FLAG_NOTES` permission guards using JWT decode (consistent with `CarePlanListComponent`)
- **Risk flag chips on appointment detail panel** — red chip row (`#FEE2E2` / `#991B1B`) displayed when `activeRiskFlagTypes` is non-empty
- **Admin risk flag type management** at `features/admin/risk-flag-types/` — list table with deactivate action and create dialog; routed under `admin/risk-flag-types`
- **i18n keys** for risk flags panel, appointment chips, and admin panel in `en.json` and `uk.json`

### Changed

- `RolePermissions`: THERAPIST and SUPERVISOR now hold `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`; RECEPTION_ADMIN_STAFF holds `READ_RISK_FLAGS`; SYSTEM_ADMINISTRATOR holds all four new permissions
- `AppointmentController.getAppointment()` (GET-by-ID) now calls `appointmentMapper.toDetailResponse()` to include active risk flag types in the single-appointment response; list endpoints remain on `toResponse()` (no change in behaviour)
- Admin layout nav updated with a "Risk Flag Types" tab

### Fixed

- Resolved N+1 query in risk flag service (flag type name resolution now uses a single `findAllById` batch call per request)
- Resolved N+1 query in appointment mapper (risk flag enrichment is skipped entirely on list/calendar endpoints)
- `actorName` in audit log now stores human-readable display name (fullName → email → UUID fallback) instead of raw JWT subject UUID
