# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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
