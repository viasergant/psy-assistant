# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — 2026-11-26

### Added (Client Telegram link)

- A therapist can now invite a client to the Telegram bot: `POST /api/v1/clients/{id}/telegram/link?language=en|uk` returns a one-time `https://t.me/<bot>?start=<token>` link, `GET` reports whether the client is linked, and `DELETE` unlinks. When the client presses Start, the bot binds that private chat to the client stored on the token (never to an id typed by the client). The client can send `/stop` to disconnect. Migration `V85` adds `client_telegram_link` (encrypted chat id, unique chat hash, `org_id`) and lets `telegram_link_token` belong to a client. A chat can belong to only one therapist or client, and access reuses the client-profile checks (org filter and assigned-therapist rule). 

### Added (Client session reminders and self-service cancellation)

- Linked clients now get a Telegram reminder before each upcoming session (`notifications.telegram.client-reminder-minutes`, default 1440 = 24 h), shown in the time zone given when the invite was created (`POST /api/v1/clients/{id}/telegram/link?timezone=Europe/Kyiv`). The reminder has a **Cancel session** button; pressing it asks for confirmation (with a late-cancellation warning), and confirming cancels the session through `AppointmentService`. The therapist gets a Telegram notice when they are linked, and the client's other pending reminders for that session are dropped.
- Cancellations closer than `notifications.telegram.late-cancellation-hours` (default 24) to the start are recorded as `LATE_CANCELLATION`, otherwise `CLIENT_INITIATED`. Because `appointment.cancelled_by` and the audit trail reference a user, the action is recorded under the session's therapist with the actor name "Client (Telegram)".
- Only the client, organization and appointment stored in the link and the database are trusted, never the button payload; sessions of another client or organization, and sessions that are cancelled or already started, cannot be cancelled. Reminders for sessions that stop being upcoming are dropped at send time.
- Frontend: the client detail page has a **Telegram** section to create an invite link (client language and the therapist's browser time zone are sent along), copy it, see whether the client is connected, and disconnect.
- Migration `V86` adds `timezone` to links and tokens, and lets `telegram_delivery_log` rows address a client (`client_id`, `appointment_id`). The bot now also subscribes to `callback_query` updates; in webhook mode the webhook is re-registered on startup.

### Fixed (Client timeline)

- The client timeline no longer shows appointment notes. They were returned as raw `enc:k1:…` ciphertext because the timeline view bypassed field decryption. Migration `V84` recreates `client_activity_timeline` without the `notes` key, and the timeline UI no longer renders it.

### Fixed (Therapist availability)

- `GET /api/v1/therapists/{id}/availability` now removes 30-minute slots that overlap a non-cancelled appointment, instead of listing every scheduled slot as free. This is the groundwork for letting clients pick a new slot from a Telegram bot.

### Changed (Telegram settings)

- The Telegram timezone now defaults to the browser's timezone whenever the stored value is still the UTC default, and it is saved automatically once the chat is linked, so digests fire in local time without a manual save.

### Fixed (Reports navigation)

- The Reports menu opened the Clients page in solo mode, because `/reports` redirected to the clinic-only caseload report. It now redirects to the first report available for the current mode and role, and the menu item is hidden for solo-mode therapists (no reports are available to them).

### Fixed (Telegram settings)

- Saving Telegram notification settings with a reminder lead time below 5 minutes failed with a generic error (the API enforces 5–1440). The form now validates the range client-side and shows a specific message

### Changed (local seed)

- `R__seed_personal_therapist.sql` now also grants `RECEPTION_ADMIN_STAFF` and `FINANCE` to the dev account, so Leads and Billing appear in the menu (local profile only)

### Added (change password)

- **Self-service password change** — `POST /api/v1/users/me/password` (`currentPassword`, `newPassword`) for any signed-in user. Verifies the current password, enforces the configured password policy and rejects reuse of the current password. All refresh tokens are revoked and a fresh token pair is returned, so the calling session stays signed in; other devices are signed out when their access token expires (up to 60 minutes — access tokens are stateless)
- **Brute-force protection** — wrong current passwords count towards the normal login lockout (5 attempts, 30 minutes); a locked account gets `423 ACCOUNT_LOCKED`. Rejections use 400/423 with codes `INVALID_CURRENT_PASSWORD`, `SAME_PASSWORD`, `ACCOUNT_LOCKED` instead of 401, so the frontend does not trigger a token refresh
- **Audit and notification** — `PASSWORD_CHANGED` audit event (`self-service`) and a best-effort confirmation email (`email.subject.password.changed`, en/uk)
- **Settings → Change password** section (en/uk)


### Added (Telegram notifications)

- **Telegram digest and reminders for therapists** — one platform bot sends each therapist a daily schedule (at their local time, on chosen weekdays) and a reminder N minutes before every session. Messages contain only time, session type and the opaque client code; client first names are an opt-in per therapist
- **Safe account linking** — Settings → "Connect Telegram" creates a single-use, 10-minute deep link (`t.me/<bot>?start=<token>`, only the SHA-256 hash is stored). The bot binds the chat to exactly the user that issued the token; group chats are refused and one chat can belong to only one user. The chat id is encrypted at rest and never exposed by the API
- **Webhook or long polling** — `notifications.telegram.mode` (`webhook` verifies `X-Telegram-Bot-Api-Secret-Token`; `polling` uses `getUpdates` with a DB-locked offset so only one instance polls). Configured with `TELEGRAM_ENABLED`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `TELEGRAM_MODE`, `TELEGRAM_WEBHOOK_SECRET`, `TELEGRAM_WEBHOOK_URL`
- **Outbox delivery** — `telegram_delivery_log` with unique dedup keys (no double sends across restarts/instances), SKIP LOCKED claiming, backoff, Telegram `retry_after` support, auto-unlink when the user blocks the bot (Flyway `V83`)
- **API** `GET/PUT /api/v1/telegram/settings`, `POST/DELETE /api/v1/telegram/link`, `POST /api/v1/telegram/test` (therapist / system administrator, always scoped to the caller); Telegram settings section on the Settings page (en/uk)

### Security (tenant isolation)

- **Hibernate `orgFilter` was never active** — `TenantHibernateJpaDialect` was registered as a plain `JpaDialect` bean, which Spring Boot ignores, so every JPA query (clients, leads, appointments, …) returned all organisations' rows. The dialect is now installed on the entity manager factory (`TenantConfig`); `TenantFilterIntegrationTest` proves two organisations no longer see each other's rows
- **Raw SQL is now scoped to the caller's organisation** — native/JDBC queries bypass the Hibernate `orgFilter`, so they leaked data across organisations. Added `org_id` conditions to client search, revenue / no-show / client-retention / lead-conversion / therapist-utilization reports, caseload list and snapshots, appointment conflict checks, bulk series cancel and the finance dashboard summary
- **`TenantContext.require()`** — raw-SQL callers fail fast if no organisation is bound instead of returning unscoped data

### Added (global admin)

- **Users tab in Global Admin** — read-only, paginated list of all registered users across all organisations (email, name, roles, organisation, status, created) with role/status/search filters and sorting; frontend route `/global-admin/users`
- **`GET /api/v1/global-admin/users`** — `GLOBAL_ADMIN` authority only, available in every deployment mode; `UserSpecification.withFilters` gained an optional email/name `search`

### Security (database encryption)

- **Field-level encryption of clinical free text** (AES-256-GCM): client/lead `notes`, referral notes, risk-flag notes, care plan descriptions and goal progress notes, outcome measure notes, session-record notes, attendance/appointment notes, and the old/new values copied into client-profile, care-plan and appointment audit tables. Name, email, phone and DOB stay plaintext so client search is unchanged.
- **Versioned keys:** ciphertext is now `enc:<keyId>:...`; `k1` is the existing `SESSION_NOTES_ENCRYPTION_KEY`, more keys via `app.encryption.keys.<id>` + `app.encryption.active-key-id`. Legacy session-note ciphertext remains readable.
- **`EncryptionBackfillRunner`** (`app.encryption.backfill.enabled=true`, run once): encrypts existing plaintext rows, upgrades legacy ciphertext and re-encrypts rows to the active key (key rotation). Idempotent.
- **Postgres TLS** in the `prod` profile via `DB_SSLMODE` (default `require`) / `DB_SSLROOTCERT`.
- ADR `docs/adr/002-encryption-at-rest.md`.

### Added (contact methods)

- **`MESSENGER` contact method type** for leads and clients — new option in the create/edit/convert lead dialogs (en/uk), accepted by the API (`EMAIL|PHONE|MESSENGER`); Flyway V82 widens the `lead_contact_methods` and `client_contact_methods` type CHECK constraints.

### Removed

- **Service catalog `category`** — dropped the free-text category field (nothing read it besides the detail view and the duplicate check) from the entity, API (`category` removed from create/update/list/detail), UI and translations. Flyway V81 drops the column and replaces the `(org_id, name, category)` unique constraint with `(org_id, name)`; services that shared a name across categories are renamed to `<name> (<category>)` first so the migration cannot fail.

### Changed

- **BREAKING (saas): `POST /api/v1/register` now requires email confirmation** — returns `202 Accepted` with no body and no tokens. The owner `User` is created inactive with `email_verified=false`; a single-use link (24 h) is emailed via the existing outbox. Registering an already-used address returns the identical `202` and emails the existing owner instead (no account enumeration).

### Added

- **Email confirmation for saas registration** — `POST /api/v1/register/confirm` (activates the account) and `POST /api/v1/register/resend` (rate-limited, 3/h per address and per IP). Login returns `EMAIL_NOT_VERIFIED` for correct credentials on an unconfirmed account. Flyway V79 adds `users.email_verified` (existing users stay verified) and `one_time_tokens`.
- **Generic `OneTimeTokenService`** (`auth.token`) — 256-bit random tokens stored as SHA-256 hashes, single-use, purpose-bound; reusable for future flows such as password reset.
- **CAPTCHA on public registration** — `CaptchaVerifier` port with Cloudflare Turnstile implementation, enabled via `CAPTCHA_ENABLED`, `CAPTCHA_SITE_KEY`, `CAPTCHA_SECRET_KEY`; site key exposed through `GET /api/v1/meta`.
- **`UnverifiedRegistrationCleanupJob`** — daily purge of registrations unconfirmed after 7 days (user, tokens and empty organization).
- **Frontend**: `/auth/register` (with "check your email" + resend state) and `/auth/confirm-email` pages, saas-only; register link on the login page; en/uk translations.

### Fixed

- **`UserManagementService` missing `orgId` on new users** — `createUser` and `createUserWithTemporaryPassword` now call `user.setOrgId(TenantContext.get())` before the INSERT, fixing a NOT NULL constraint violation that caused every therapist-creation call to return HTTP 500 in multi-tenant (saas/clinic) deployments.

### Added

- **`Organization` entity + multi-tenancy foundation** — introduces an `Organization` table as the root tenant boundary. All 25 core domain tables (`users`, `clients`, `leads`, `therapist_profile`, `appointments`, `session_record`, `session_note`, `care_plan`, `invoices`, `prepaid_package_*`, `service_catalog*`, `discount_rules`, `caseload_snapshot`, `client_risk_flags`, `follow_up_tasks`, `therapist_*schedule/leave`, `notification_templates`, `outcome_measure_definition`, `note_template`) receive an `org_id FK NOT NULL` column via Flyway V76–V78.
- **Row-level isolation via Hibernate `@Filter`** — a single `orgFilter` declared in `package-info.java` and applied to each org-scoped entity class; activated at transaction-begin time by `TenantHibernateJpaDialect` (extends `HibernateJpaDialect`) reading from a request-scoped `TenantContext` ThreadLocal populated by `TenantInterceptor`.
- **`orgId` JWT claim** — `TokenService.buildAccessToken()` now includes the authenticated user's organization UUID in the JWT, enabling the interceptor to scope all subsequent queries automatically.
- **`saas` deployment profile** — third deployment mode accepted by `DeploymentModeValidator`; enables `GET /api/v1/meta` mode=SAAS and activates the registration controller.
- **`POST /api/v1/register`** (`@Profile("saas")`) — public self-service registration endpoint. Creates one `Organization` (type SOLO) + one `User` (all five roles) in a single transaction and immediately returns a JWT + refresh token so the caller is logged in without a separate login step.
- **`AuthService.issueTokensForUser(User)`** — new package-level method used by registration to build a token pair for an already-persisted user without re-validating credentials.
- **`UserRepository.existsByEmailAndOrgId`** — org-scoped email uniqueness check for use in clinic user management (global uniqueness enforced at registration time, org-scoped uniqueness available for admin-created users).
- **Org-scoped unique constraints** — V78 replaces four global unique constraints (`users.email`, `therapist_profile.email`, `service_catalog(name,category)`, `prepaid_package_definition.name`) with org-scoped composite variants and updates the matching `@Table` annotations.
- **Bootstrap creates Organization** — `SoloUserInitRunner` and `ClinicSetupController` now create an `Organization` row before the first user, keeping solo/clinic single-org deployments fully functional with the new schema.

---

## [Unreleased] — 2026-05-27

### Added

- **Solo/Clinic deployment mode** — the application now requires an explicit Spring Boot profile at startup (`--spring.profiles.active=solo` or `clinic`). Starting without a declared mode fails fast with a descriptive error.
- **`GET /api/v1/meta`** — unauthenticated endpoint returning the active deployment mode (`SOLO`/`CLINIC`) and application version; consumed by the Angular frontend on boot.
- **Solo first-boot user creation (`SoloUserInitRunner`)** — on first boot in solo mode the system auto-creates one user account from `SOLO_USERNAME`/`SOLO_PASSWORD` environment variables with all five roles and `mustChangePassword=true`. Subsequent boots are idempotent.
- **Clinic first-boot admin endpoint (`POST /api/v1/setup/admin`)** — available in clinic mode only; allows creation of the initial administrator account before any users exist. Returns HTTP 201 with a `Location` header; returns HTTP 409 once setup is complete. The count-and-save is transactional to prevent race conditions.
- **Local dev admin seed (`R__seed_admin_user.sql`)** — Flyway repeatable script in `db/local-seed/` that inserts `admin@psyassistant.local` / `Admin1234!` using the current `user_roles` junction-table schema; idempotent via `ON CONFLICT DO NOTHING`.
- **Angular `AppModeService`** — singleton service that fetches `GET /api/v1/meta` during app initialisation and exposes the deployment mode as a `BehaviorSubject`; errors are swallowed so a backend outage does not block the app from loading.
- **`*appIfMode` structural directive** — shows or hides any element based on the current deployment mode; reactive to mode changes via Angular signals.
- **`modeGuard(requiredMode)`** — functional route guard factory that redirects to `/` when the active deployment mode does not match the required mode.

### Changed

- `AdminUserController` and `CaseloadController` are now registered only in clinic mode (`@Profile("clinic")`); requesting those endpoints in solo mode returns HTTP 404.
- The `admin` route in the Angular frontend requires clinic mode; solo-mode users are redirected to `/` by the mode guard.
- The `caseload` and `therapist-utilization` report routes require clinic mode; solo-mode users are redirected to `/`.
- The shell navigation sidebar hides the Admin entry in solo mode using a reactive `clinicOnly` computed filter on `visibleNav`.
- `application-solo.yml` and `application-clinic.yml` profile stubs added; `application-local.yml` `flyway.locations` already included `db/local-seed` (no change required).

### Fixed

- **Proactive `org_id` sweep — 9 remaining entities** — Added `setOrgId()` setters and `TenantContext.get()` calls for: `TherapistProfile` (create + clinic-register paths in `TherapistProfileService`), `TherapistRecurringSchedule` + `TherapistScheduleOverride` (3 sites in `TherapistScheduleService`), `TherapistLeave` (`TherapistLeaveService`), `FollowUpTask` (`NoShowFollowUpService`), `ServiceCatalogTherapistOverride` (`ServiceCatalogService`). Added `setOrgId()` setters (no current creation site) to `OutcomeMeasureDefinition` and `NoteTemplate`. Fixed `CaseloadSnapshotJob` raw-JDBC upsert to select and pass `org_id` from `therapist_profile` into the `caseload_snapshot` INSERT.
- **`client_risk_flags.org_id` null on flag creation** — `RiskFlagService.createFlag` now sets `orgId` from `TenantContext` before persisting. Added `setOrgId()` to `ClientRiskFlag`.
- **`prepaid_package_instance.org_id` null on package sale** — `PackageService.sellPackage` now sets `orgId` from `TenantContext` before persisting. Added `setOrgId()` to `PrepaidPackageInstance`.
- **`care_plan.org_id` null on creation** — `CarePlanService.create` now sets `orgId` from `TenantContext` before persisting. Added `setOrgId()` to `CarePlan`.
- **`service_catalog_price_history.org_id` null on creation** — `ServiceCatalogService` now sets `orgId` from `TenantContext` on both `ServiceCatalogPriceHistory` construction sites (`createService` and `updateDefaultPrice`). Added `setOrgId()` to `ServiceCatalogPriceHistory`.
- **`notification_templates.org_id` null on template creation** — `NotificationTemplateService.createTemplate` now sets `orgId` from `TenantContext` before persisting. Added `setOrgId()` to `NotificationTemplate`.
- **`GET/POST /api/v1/billing/discounts` returned 404** — the discount rules REST layer was missing entirely. Added `DiscountRuleService` and `DiscountRuleController` implementing `GET` (list all / filter by `clientId` or `serviceCatalogId`), `POST` (create), and `PATCH /{id}/status` (toggle active). Added `MANAGE_DISCOUNTS` permission to `Permission` enum, assigned to `FINANCE` and `SYSTEM_ADMINISTRATOR` roles. Added `setOrgId()` to `DiscountRule`; service sets it from `TenantContext` on creation.
- **`service_catalog.org_id` null on creation** — `ServiceCatalogService.createService` now sets `orgId` from `TenantContext` before persisting. Added `setOrgId()` setter to `ServiceCatalog`.
- **`prepaid_package_definition.org_id` null on creation** — `PackageService.createDefinition` now sets `orgId` from `TenantContext` before persisting. Added `setOrgId()` setter to `PrepaidPackageDefinition`.
- **`invoices.org_id` null on all invoice creation paths** — `InvoiceService` now calls `invoice.setOrgId(TenantContext.get())` on all four creation sites: auto-generated from session completion, generated from session, manual, and package. Added `setOrgId()` setter to `Invoice`.
- **`session_note.org_id` null on note creation** — `SessionNoteService.createNote` now sets `orgId` from `TenantContext` before persisting, fixing a `NOT NULL` constraint violation on `POST /api/sessions/{id}/notes`.
- **`session_record.org_id` null on appointment status change** — `SessionRecordService.createSessionIfNotExists` and `startSessionFromAppointment` now set `orgId` from the loaded `Appointment` entity before persisting the new `SessionRecord`. Previously the field was left null, causing a `NOT NULL` constraint violation when marking an appointment `IN_PROGRESS` or `COMPLETED`.
- `RbacIntegrationTest` and `PsyAssistantApplicationTest` updated to activate the `clinic` profile, preventing false 404s on clinic-only endpoints that previously masked 403 assertions.

---

## [Unreleased] — 2026-05-22

### Added

- **Service catalog item selector in appointment booking dialog** — an optional "Service" dropdown appears after a session type is chosen and shows only active catalog items that match that session type. Selecting a service pre-fills the duration field from the catalog item's configured `durationMin`.
- **DB migration V69** — adds nullable `service_catalog_item_id` FK column to both `appointment` and `session_record` tables, referencing `service_catalog`.
- **Deterministic invoice pricing** — when a booking includes a specific service catalog item, the auto-generated invoice on session completion uses that item's price (including therapist-specific override if present) rather than picking the first active match by session type. Backward-compatible: appointments without a service selection fall through to the existing pricing chain.
- **Attended checkbox for individual session records** — therapists can mark a session as attended directly from the session list view; translations updated for both English and Ukrainian.
- **Settings feature** — new `/settings` route with language selection, enabling users to switch between English and Ukrainian UI.

### Changed

- All form `<select>` elements replaced with PrimeNG `p-select` for consistent look, keyboard navigation, and accessible dropdown behaviour.
- Tables across the Leads, Clients, and other list views migrated to PrimeNG `p-table` with built-in pagination, sortable columns, and uniform row styling.
- PrimeNG select and multiselect components now share consistent background, border colour, and focus ring variables from the design-system tokens.
- Button component extended with an `outlined` variant for secondary actions.
- Session completion dialogs enriched with a dedicated clinical notes section; notes load and display inline without a separate dialog step.
- User management dialogs now trim whitespace from all text inputs before validation and submission.
- Comprehensive English and Ukrainian translations added for all previously untranslated screen labels, placeholders, validation messages, and ARIA strings.

### Fixed

- User seed script updated to insert roles via the `user_roles` junction table instead of the deprecated `users.role` column (V68 multi-role migration compatibility).

---

## [2026-05-13]

### Added

- **Risk flag subsystem** — full backend and frontend implementation of structured, role-protected risk flag indicators on client profiles (PA-27).
- **DB migration V67** — three new tables: `risk_flag_types` (configurable flag types with soft-delete), `client_risk_flags` (per-client flags with status, clinical note, review date, resolve workflow), and `risk_flag_audit_log` (append-only audit trail with no FK on `flag_id`, matching the `care_plan_audit` pattern).
- **Five seed risk flag types**: Self-Harm Risk, Crisis History, Safeguarding Concern, Domestic Abuse Concern, Suicidal Ideation.
- **Four new permissions**: `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`, `MANAGE_RISK_FLAG_TYPES` — propagate automatically to JWTs via existing `TokenService`.
- **`RiskFlagService`** — create, resolve, list (active and all) with THERAPIST assignment enforcement, clinical note stripping for callers lacking `READ_RISK_FLAG_NOTES`, and N+1-safe batch flag type name resolution.
- **`RiskFlagTypeService`** — list active types (all authenticated staff), full admin CRUD with deterministic `displayOrder`-sorted listing.
- **REST endpoints** under `/api/v1/clients/{clientId}/risk-flags` (4 endpoints) and `/api/v1/risk-flag-types` + `/api/v1/admin/risk-flag-types` (4 endpoints).
- **`AppointmentResponse`** extended with `activeRiskFlagTypes: List<String>` — populated only on single-appointment GET (detail view), not on list endpoints.
- **`AppointmentMapper`** split into `toDetailResponse()` (enriched, batch-safe) and `toResponse()` (slim, `List.of()`) to avoid N+1 on calendar/list views.
- **Frontend risk flag feature module** at `features/clients/components/risk-flags/` — `RiskFlagsPanelComponent`, `RiskFlagFormDialogComponent`, `RiskFlagResolveDialogComponent` with reactive forms, role-gated clinical note display, and red chip indicators.
- **Risk flag panel** integrated into `ClientDetailComponent` with `MANAGE_RISK_FLAGS` and `READ_RISK_FLAG_NOTES` permission guards using JWT decode (consistent with `CarePlanListComponent`).
- **Risk flag chips on appointment detail panel** — red chip row (`#FEE2E2` / `#991B1B`) displayed when `activeRiskFlagTypes` is non-empty.
- **Admin risk flag type management** at `features/admin/risk-flag-types/` — list table with deactivate action and create dialog; routed under `admin/risk-flag-types`.
- **i18n keys** for risk flags panel, appointment chips, and admin panel in `en.json` and `uk.json`.

### Changed

- `RolePermissions`: THERAPIST and SUPERVISOR now hold `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`; RECEPTION_ADMIN_STAFF holds `READ_RISK_FLAGS`; SYSTEM_ADMINISTRATOR holds all four new permissions.
- `AppointmentController.getAppointment()` (GET-by-ID) now calls `appointmentMapper.toDetailResponse()` to include active risk flag types in the single-appointment response; list endpoints remain on `toResponse()` (no change in behaviour).
- Admin layout nav updated with a "Risk Flag Types" tab.

### Fixed

- Resolved N+1 query in risk flag service (flag type name resolution now uses a single `findAllById` batch call per request).
- Resolved N+1 query in appointment mapper (risk flag enrichment is skipped entirely on list/calendar endpoints).
- `actorName` in audit log now stores human-readable display name (fullName → email → UUID fallback) instead of raw JWT subject UUID.
