# PA-27 Progress

## 2026-05-13 — Increment 1: V67 Flyway migration: schema + seed

- **What was completed:** Created V67 migration with all three tables, four indexes, and five seed rows.
- **Interfaces/methods created:** N/A (SQL migration only)
- **Files created/modified:** `backend/src/main/resources/db/migration/V67__risk_flag_tables.sql` (created)
- **Decisions made:**
  - Followed V36 style conventions: section header comments, column alignment, inline CHECK constraints.
  - `risk_flag_audit_log` has no FK on `flag_id` or `client_id`, matching the `care_plan_audit` append-only pattern documented in the plan.
  - Seed INSERT omits `active` (defaults to TRUE) and `created_at` (defaults to NOW()) — explicit values are not needed and would add noise.
- **Tests:** N/A — migration SQL has no unit tests; acceptance is verified by `mvn flyway:migrate` on a fresh schema.

---

## 2026-05-13 — Increment 2: New permissions in Permission.java / RolePermissions.java

- **What was completed:** Added four new risk-flag permission constants to `Permission.java` and updated the role-to-permission matrix in `RolePermissions.java` for all five active roles.
- **Interfaces/methods created:** Four new `Permission` enum constants: `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`, `MANAGE_RISK_FLAG_TYPES`.
- **Files created/modified:**
  - `backend/src/main/java/com/psyassistant/common/security/Permission.java` (modified — new `// ---- Risk flags` section)
  - `backend/src/main/java/com/psyassistant/common/security/RolePermissions.java` (modified — Javadoc matrix table + role sets updated)
  - `backend/src/test/java/com/psyassistant/common/rbac/RolePermissionsRiskFlagsTest.java` (created — 15 unit tests)
- **Role matrix changes:**
  - `RECEPTION_ADMIN_STAFF`: added `READ_RISK_FLAGS`
  - `THERAPIST`: added `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`
  - `SUPERVISOR`: added `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`
  - `FINANCE`: no changes
  - `SYSTEM_ADMINISTRATOR`: added all four new permissions
- **Decisions made:** None — implementation matches spec exactly.
- **Tests:** 15 passing (unit), 10 passing (`RbacIntegrationTest` — no regression).

---

## 2026-05-13 — Increment 3: Domain entities and repositories

- **What was completed:** Created three JPA entities, two status/action enums, and three Spring Data repositories for the riskflags package.
- **Interfaces/methods created:**
  - `RiskFlagType(String name, short displayOrder)` constructor; `deactivate()` method; getters for all fields
  - `ClientRiskFlag(UUID clientId, UUID flagTypeId, String clinicalNote, LocalDate reviewDate, UUID createdByUserId)` constructor; `resolve(UUID resolvedByUserId, String resolutionNote)` method; getters for all fields
  - `RiskFlagAuditLog(UUID flagId, UUID clientId, UUID actorUserId, String actorName, RiskFlagAuditActionType actionType, String flagTypeName, String status)` constructor; getters for all fields
  - `RiskFlagTypeRepository.findAllByActiveTrueOrderByDisplayOrderAsc()`
  - `ClientRiskFlagRepository.findAllByClientIdAndStatus(UUID, ClientRiskFlagStatus)`
  - `ClientRiskFlagRepository.findAllByClientIdOrderByCreatedAtDesc(UUID)`
  - `RiskFlagAuditLogRepository.findAllByClientIdOrderByActionTimestampDesc(UUID)`
- **Files created:**
  - `backend/src/main/java/com/psyassistant/riskflags/domain/ClientRiskFlagStatus.java`
  - `backend/src/main/java/com/psyassistant/riskflags/domain/RiskFlagAuditActionType.java`
  - `backend/src/main/java/com/psyassistant/riskflags/domain/RiskFlagType.java`
  - `backend/src/main/java/com/psyassistant/riskflags/domain/ClientRiskFlag.java`
  - `backend/src/main/java/com/psyassistant/riskflags/domain/RiskFlagAuditLog.java`
  - `backend/src/main/java/com/psyassistant/riskflags/repository/RiskFlagTypeRepository.java`
  - `backend/src/main/java/com/psyassistant/riskflags/repository/ClientRiskFlagRepository.java`
  - `backend/src/main/java/com/psyassistant/riskflags/repository/RiskFlagAuditLogRepository.java`
  - `backend/src/test/java/com/psyassistant/riskflags/domain/RiskFlagTypeTest.java`
  - `backend/src/test/java/com/psyassistant/riskflags/domain/ClientRiskFlagTest.java`
  - `backend/src/test/java/com/psyassistant/riskflags/domain/RiskFlagAuditLogTest.java`
- **Decisions made:**
  - `RiskFlagType` does NOT extend `SimpleBaseEntity`. The `risk_flag_types` table (V67) has only `created_at`, not `updated_at`. Extending `SimpleBaseEntity` would require an `updated_at` column that doesn't exist in the schema, causing a Hibernate mapping error. Explicit `@Column(name = "created_at", insertable = false, updatable = false)` is used instead — matching the `CarePlanAudit` pattern.
  - `ClientRiskFlag.createdAt` uses `insertable = false, updatable = false` so the DB DEFAULT NOW() populates the column; this matches the V67 schema where `created_at` defaults server-side.
  - `RiskFlagAuditLog.actionTimestamp` uses the same `insertable = false, updatable = false` approach — DB-managed, not application-managed.
  - `ClientRiskFlagRepository` exposes `findAllByClientIdOrderByCreatedAtDesc` (not `findAllByClientId`) to enforce consistent ordering for the supervisor full-history view.
- **Tests:** 18 new tests passing; full suite 431/431 passing, 0 failures

---

## 2026-05-13 — Increment 4: Service layer and unit tests

- **What was completed:** Created four DTOs, two services (`RiskFlagService` and `RiskFlagTypeService`), and 10 unit tests for `RiskFlagService`.
- **Interfaces/methods created:**
  - `CreateRiskFlagRequest` record: `flagTypeId UUID @NotNull`, `clinicalNote String`, `reviewDate LocalDate @NotNull @FutureOrPresent`
  - `ResolveRiskFlagRequest` record: `resolutionNote String @NotBlank`
  - `RiskFlagResponse` record: `id, clientId, flagTypeId, flagTypeName, status, clinicalNote, reviewDate, createdByUserId, createdAt, resolvedByUserId, resolvedAt, resolutionNote`
  - `RiskFlagTypeResponse` record: `id UUID, name String, displayOrder int, active boolean`
  - `RiskFlagService.createFlag(UUID clientId, CreateRiskFlagRequest request, UUID actorId, String actorName)`
  - `RiskFlagService.resolveFlag(UUID clientId, UUID flagId, ResolveRiskFlagRequest request, UUID actorId, String actorName)`
  - `RiskFlagService.listActiveFlags(UUID clientId)`
  - `RiskFlagService.listAllFlags(UUID clientId)`
  - `RiskFlagTypeService.listActive()`
  - `RiskFlagTypeService.listAll()`
  - `RiskFlagTypeService.create(String name, int displayOrder)`
  - `RiskFlagTypeService.deactivate(UUID id)`
- **Files created:**
  - `backend/src/main/java/com/psyassistant/riskflags/dto/CreateRiskFlagRequest.java`
  - `backend/src/main/java/com/psyassistant/riskflags/dto/ResolveRiskFlagRequest.java`
  - `backend/src/main/java/com/psyassistant/riskflags/dto/RiskFlagResponse.java`
  - `backend/src/main/java/com/psyassistant/riskflags/dto/RiskFlagTypeResponse.java`
  - `backend/src/main/java/com/psyassistant/riskflags/service/RiskFlagService.java`
  - `backend/src/main/java/com/psyassistant/riskflags/service/RiskFlagTypeService.java`
  - `backend/src/test/java/com/psyassistant/riskflags/service/RiskFlagServiceTest.java`
- **Decisions made:**
  - Permission checks are implemented both via `@PreAuthorize` (for Spring context enforcement) and via programmatic `SecurityContextHolder` checks (to support unit testing without a Spring context). This dual approach ensures AOP-level enforcement in production and testable logic at the method level.
  - `listAll()` and `create()` and `deactivate()` in `RiskFlagTypeService` use both `@PreAuthorize` and `requireAuthority()` — consistent with the programmatic approach used in `RiskFlagService`.
  - `loadActiveFlagType()` throws `EntityNotFoundException` for both missing and inactive flag types, as specified.
  - Method test names follow the project's camelCase convention (no underscores) to satisfy Checkstyle rule `^[a-z][a-zA-Z0-9]*$`.
  - `@DisplayName` is used to retain human-readable test descriptions.
- **Tests:** 10 new tests passing; full suite 441/441 passing, 0 failures

---

## 2026-05-13 — Increment 4 post-merge fixes

- **What was completed:** Two performance/correctness fixes applied to the Increment 4 service layer before Increment 5 proceeds.
- **Fix 1 (High) — N+1 query eliminated in `RiskFlagService.listActiveFlags` and `listAllFlags`:**
  - Both methods previously called `flagTypeRepository.findById(flag.getFlagTypeId())` inside a stream loop.
  - Refactored to collect distinct `flagTypeId` values into a `Set<UUID>`, call `flagTypeRepository.findAllById(ids)` once, build a `Map<UUID, String>` (id → name), and look up names from the map during mapping.
  - Two existing tests updated to stub `findAllById(Set.of(flagTypeId))` instead of the removed `findById(flagTypeId)` stubs (the old stubs triggered `UnnecessaryStubbing` errors under Mockito strict mode).
- **Fix 2 (Medium) — `RiskFlagTypeService.listAll()` now returns rows in deterministic order:**
  - Changed `flagTypeRepository.findAll()` to `flagTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"))`.
  - Added `import org.springframework.data.domain.Sort`.
- **Files modified:**
  - `backend/src/main/java/com/psyassistant/riskflags/service/RiskFlagService.java`
  - `backend/src/main/java/com/psyassistant/riskflags/service/RiskFlagTypeService.java`
  - `backend/src/test/java/com/psyassistant/riskflags/service/RiskFlagServiceTest.java`
- **Tests:** 441/441 passing, 0 failures

---

## 2026-05-13 — Increment 5: REST controllers

- **What was completed:** Created `RiskFlagController`, `RiskFlagTypeController`, and the `CreateRiskFlagTypeRequest` DTO. All REST endpoints are wired to the service layer with `@PreAuthorize` security and correct HTTP semantics.
- **Interfaces/methods created:**
  - `RiskFlagController.listActive(UUID clientId)` — `GET /api/v1/clients/{clientId}/risk-flags`
  - `RiskFlagController.listHistory(UUID clientId)` — `GET /api/v1/clients/{clientId}/risk-flags/history`
  - `RiskFlagController.create(UUID clientId, CreateRiskFlagRequest, Authentication)` — `POST /api/v1/clients/{clientId}/risk-flags` → 201 + Location
  - `RiskFlagController.resolve(UUID clientId, UUID flagId, ResolveRiskFlagRequest, Authentication)` — `PATCH /api/v1/clients/{clientId}/risk-flags/{flagId}/resolve` → 200
  - `RiskFlagTypeController.listActive()` — `GET /api/v1/risk-flag-types` (no `@PreAuthorize`, any authenticated user)
  - `RiskFlagTypeController.listAll()` — `GET /api/v1/admin/risk-flag-types`
  - `RiskFlagTypeController.create(CreateRiskFlagTypeRequest)` — `POST /api/v1/admin/risk-flag-types` → 201 + Location
  - `RiskFlagTypeController.deactivate(UUID id)` — `PATCH /api/v1/admin/risk-flag-types/{id}/deactivate` → 200
  - `CreateRiskFlagTypeRequest` record: `name @NotBlank @Size(max=100)`, `displayOrder int @Min(0)`
- **Files created:**
  - `backend/src/main/java/com/psyassistant/riskflags/rest/RiskFlagController.java`
  - `backend/src/main/java/com/psyassistant/riskflags/rest/RiskFlagTypeController.java`
  - `backend/src/main/java/com/psyassistant/riskflags/dto/CreateRiskFlagTypeRequest.java`
- **Decisions made:**
  - `actorId` extracted via `UserManagementService.currentPrincipalId()` (matches `ClientController` pattern).
  - `actorName` extracted via `auth.getName()` (matches spec; avoids the extra DB lookup that `CarePlanController.resolveActorName()` does).
  - `Location` header on 201 responses is constructed as a plain `URI.create(...)` string — the existing codebase does not use `ServletUriComponentsBuilder` and returns 201 without `Location` in other controllers; the explicit URI string approach is simpler and doesn't depend on servlet context being available in tests.
  - `RiskFlagTypeController` uses two base paths (`/api/v1/risk-flag-types` and `/api/v1/admin/risk-flag-types`) on the same class without a class-level `@RequestMapping` — each method carries its full path. This is consistent with the plan spec and avoids a shared base path.
  - No controller-slice tests added in this increment per the spec ("controller slice tests are deferred").
- **Tests:** 441/441 passing, 0 failures (no regressions)

---

## 2026-05-13 — Pre-Increment-5 hotfix: RiskFlagController actorName resolution

- **What was completed:** Fixed a High-severity bug where `RiskFlagController` passed `auth.getName()` (a raw UUID JWT subject) as `actorName` to the audit log instead of a human-readable display name.
- **Root cause:** `Authentication.getName()` returns the JWT subject claim, which is the user's UUID string, not their name.
- **Fix applied:** Injected `UserRepository` into `RiskFlagController` and added a private `resolveActorName(UUID actorId, Authentication auth)` helper that mirrors the identical pattern in `CarePlanController`: looks up the user by ID, prefers `fullName`, falls back to `email`, and finally falls back to `auth.getName()` (UUID) if the user is not found.
- **Files modified:**
  - `backend/src/main/java/com/psyassistant/riskflags/rest/RiskFlagController.java` — constructor updated, `UserRepository` field added, `resolveActorName` private method added, both `actorName` usages updated
- **Files created:**
  - `backend/src/test/java/com/psyassistant/riskflags/rest/RiskFlagControllerTest.java` — 13 controller-slice tests (actorName resolution with fullName, with null fullName → email, with missing user → UUID fallback; HTTP status and security checks for all endpoints)
- **Decisions made:**
  - Exact same `resolveActorName` implementation as `CarePlanController` — no deviation.
  - `@MockitoBean AuditLogService` added to the test (required by `GlobalExceptionHandler` constructor injection).
- **Tests:** 454/454 passing (441 existing + 13 new), 0 failures

---

## 2026-05-13 — Increment 6: Extend AppointmentResponse with activeRiskFlagTypes

- **What was completed:** Added `List<String> activeRiskFlagTypes` as the last component to `AppointmentResponse` record. Updated `AppointmentMapper` to inject `ClientRiskFlagRepository` and `RiskFlagTypeRepository`, and populate the field using a two-query approach (active flags by client, then batch type lookup) that avoids N+1. Wrote 7 unit tests covering all branches. `CalendarAppointmentBlock` was NOT changed.
- **Interfaces/methods created:**
  - `AppointmentResponse.activeRiskFlagTypes()` — new record component (accessor generated by the record)
  - `AppointmentMapper(ClientRiskFlagRepository, RiskFlagTypeRepository)` — new constructor (replaced no-arg)
  - `AppointmentMapper.resolveActiveRiskFlagTypeNames(UUID)` — private helper method
- **Files created/modified:**
  - `backend/src/main/java/com/psyassistant/scheduling/dto/AppointmentResponse.java` (modified — added `List<String> activeRiskFlagTypes` component + `import java.util.List`)
  - `backend/src/main/java/com/psyassistant/scheduling/dto/AppointmentMapper.java` (modified — constructor injection, `resolveActiveRiskFlagTypeNames` private method, `activeRiskFlagTypes` passed to record constructor)
  - `backend/src/test/java/com/psyassistant/scheduling/dto/AppointmentMapperTest.java` (created — 7 unit tests)
- **Decisions made:**
  - `AppointmentService.java` was listed as a file to modify in the plan, but it contains no `AppointmentResponse` or `AppointmentMapper` usage — no changes were needed.
  - Confirmed via grep that `new AppointmentResponse(...)` is constructed in exactly one place: `AppointmentMapper.toResponse()`. All call-sites in controllers use `appointmentMapper.toResponse()` and required no changes.
  - `resolveActiveRiskFlagTypeNames` early-returns `List.of()` when no active flags exist, avoiding the type repository call entirely for clients with no flags.
  - Orphaned flag type IDs (flag exists, type deleted from repo) are silently filtered out via `filter(name -> name != null)` to prevent NPE.
  - `CalendarAppointmentBlock` is confirmed unchanged — N+1 concern on calendar grid is avoided.
- **Tests:** 7 new tests passing; full suite 461/461 passing, 0 failures

---

## 2026-05-13 — Pre-Increment-6 hotfix: N+1 in AppointmentMapper

- **What was completed:** Fixed a High-severity N+1 issue where `AppointmentMapper.toResponse()` called `resolveActiveRiskFlagTypeNames()` (two SQL queries per appointment) on all call sites including list endpoints. Risk-flag enrichment now applies only to the single-appointment detail view.
- **Root cause:** `toResponse()` was enriching every appointment with risk flag data, regardless of whether the caller was a single-detail endpoint or a list endpoint.
- **Fix applied:**
  1. Renamed the enriched mapping to `toDetailResponse(Appointment)` — calls `resolveActiveRiskFlagTypeNames()` and is used only by the `GET /api/v1/appointments/{id}` detail endpoint.
  2. Created a new slim `toResponse(Appointment)` that passes `List.of()` for `activeRiskFlagTypes` without any repository queries — used by list and mutation endpoints.
  3. Extracted common record construction into a private `buildResponse(Appointment, List<String>)` helper to eliminate duplication between the two public methods.
  4. Updated `AppointmentController.getAppointment()` to call `appointmentMapper.toDetailResponse()` (was `toResponse()`).
  5. All other `AppointmentController` call-sites (`createAppointment`, `rescheduleAppointment`, `cancelAppointment`, `updateAppointmentStatus`, `getTherapistAppointments`) retain `toResponse()` — they now receive the slim version.
- **Files modified:**
  - `backend/src/main/java/com/psyassistant/scheduling/dto/AppointmentMapper.java` — `toResponse()` is now slim; `toDetailResponse()` is the enriched version; `buildResponse()` private helper added
  - `backend/src/main/java/com/psyassistant/scheduling/rest/AppointmentController.java` — `getAppointment()` now calls `toDetailResponse()`
  - `backend/src/test/java/com/psyassistant/scheduling/dto/AppointmentMapperTest.java` — previous `toResponse` enrichment tests moved to `toDetailResponse`; two new slim `toResponse` tests added (null-guard already existed, base-fields test renamed); total test count increased from 7 to 10
- **Decisions made:**
  - Mutation endpoints (create, reschedule, cancel, updateStatus) use the slim `toResponse()` — they are not detail-view reads and the spec only requires enrichment for the GET-by-ID endpoint.
  - The `toResponseReturnsNullWhenAppointmentIsNull` test is preserved on `toResponse()` (slim path) since null-safety is required on both methods; a separate null test added for `toDetailResponse()`.
- **Tests:** 464/464 passing (461 pre-existing + 3 new), 0 failures

---

## 2026-05-13 — Increment 7: Frontend: models and service

- **What was completed:** Created TypeScript model interfaces/types for risk flags and a root-level Angular HTTP service wiring all five backend endpoints.
- **Interfaces/methods created:**
  - `RiskFlagStatus` — `'ACTIVE' | 'RESOLVED'` union type
  - `RiskFlagType` interface: `id, name, displayOrder, active`
  - `RiskFlag` interface: `id, clientId, flagTypeId, flagTypeName, status, clinicalNote, reviewDate, createdByUserId, createdAt, resolvedByUserId, resolvedAt, resolutionNote`
  - `CreateRiskFlagPayload` interface: `flagTypeId, clinicalNote, reviewDate`
  - `ResolveRiskFlagPayload` interface: `resolutionNote`
  - `RiskFlagService.listActive(clientId: string): Observable<RiskFlag[]>` — GET `/api/v1/clients/{id}/risk-flags`
  - `RiskFlagService.listAll(clientId: string): Observable<RiskFlag[]>` — GET `/api/v1/clients/{id}/risk-flags/history`
  - `RiskFlagService.create(clientId: string, payload: CreateRiskFlagPayload): Observable<RiskFlag>` — POST `/api/v1/clients/{id}/risk-flags`
  - `RiskFlagService.resolve(clientId: string, flagId: string, payload: ResolveRiskFlagPayload): Observable<RiskFlag>` — PATCH `/api/v1/clients/{id}/risk-flags/{flagId}/resolve`
  - `RiskFlagService.listTypes(): Observable<RiskFlagType[]>` — GET `/api/v1/risk-flag-types`
- **Files created:**
  - `frontend/src/app/features/clients/components/risk-flags/models/risk-flag.model.ts`
  - `frontend/src/app/features/clients/components/risk-flags/services/risk-flag.service.ts`
  - `frontend/src/app/features/clients/components/risk-flags/services/risk-flag.service.spec.ts`
- **Decisions made:**
  - Service uses constructor injection (`constructor(private http: HttpClient)`) — matches the existing pattern in `ClientService` and `CarePlanService` (not `inject()`, which is not used in any existing service in this codebase).
  - Base URL is hardcoded as `/api/v1/clients` and `/api/v1/risk-flag-types` — no `environment.apiUrl` reference, matching all existing services which use relative path strings directly. The `environment.ts` file does not define `apiUrl`.
  - Two separate base path constants (`clientsBase`, `typesBase`) keep the `listTypes()` endpoint clearly distinct from the client-scoped endpoints.
  - Spec uses `HttpClientTestingModule` + `HttpTestingController` — matches the Angular testing pattern used elsewhere; 13 tests covering all five methods, including edge cases (null clinicalNote, empty arrays, correct HTTP methods and URL shapes).
- **Tests:** 13 new spec tests; `ng build --configuration production` passes with zero TypeScript errors (pre-existing budget warnings are unrelated).

---

## 2026-05-13 — Increment 8: Frontend: RiskFlagsPanel on client detail

- **What was completed:** Created three standalone Angular components — `RiskFlagsPanelComponent`, `RiskFlagFormDialogComponent`, `RiskFlagResolveDialogComponent` — and integrated the panel into `client-detail.component.ts`.
- **Interfaces/methods created:**
  - `RiskFlagsPanelComponent` — inputs: `clientId: string`, `canManage: boolean`, `canReadNotes: boolean`; displays active risk flags, opens add/resolve dialogs, reloads on save/resolve
  - `RiskFlagFormDialogComponent` — inputs: `clientId: string`; outputs: `saved`, `cancelled`; reactive form with `flagTypeId` (required), `reviewDate` (required), `clinicalNote` (optional)
  - `RiskFlagResolveDialogComponent` — inputs: `clientId: string`, `flagId: string`; outputs: `resolved`, `cancelled`; reactive form with `resolutionNote` (required)
  - `ClientDetailComponent.hasPermission(authority: string): boolean` — decodes JWT and checks `decoded.roles` array (same pattern as `CarePlanListComponent.hasAuthority()`)
- **Files created:**
  - `frontend/src/app/features/clients/components/risk-flags/risk-flags-panel/risk-flags-panel.component.ts`
  - `frontend/src/app/features/clients/components/risk-flags/risk-flags-panel/risk-flags-panel.component.spec.ts` (18 tests)
  - `frontend/src/app/features/clients/components/risk-flags/risk-flag-form-dialog/risk-flag-form-dialog.component.ts`
  - `frontend/src/app/features/clients/components/risk-flags/risk-flag-form-dialog/risk-flag-form-dialog.component.spec.ts` (11 tests)
  - `frontend/src/app/features/clients/components/risk-flags/risk-flag-resolve-dialog/risk-flag-resolve-dialog.component.ts`
  - `frontend/src/app/features/clients/components/risk-flags/risk-flag-resolve-dialog/risk-flag-resolve-dialog.component.spec.ts` (9 tests)
- **Files modified:**
  - `frontend/src/app/features/clients/client-detail/client-detail.component.ts` — added `RiskFlagsPanelComponent` import + `jwtDecode` import, added component to `imports` array, added `<app-risk-flags-panel>` element to template with `hasPermission()` bindings, added `hasPermission()` public method
- **Decisions made:**
  - `hasPermission(authority: string): boolean` uses `jwtDecode` on `authService.token` and checks `decoded.roles.includes(authority)` — identical pattern to `CarePlanListComponent.hasAuthority()`. The `PermissionService.hasPermission()` was not used because it works with role-based `PermissionKey` constants and does not support arbitrary permission strings like `MANAGE_RISK_FLAGS`.
  - `RiskFlagsPanelComponent` calls `listAll()` when `canReadNotes=true` (supervisor/admin) and filters to `status === 'ACTIVE'` client-side, keeping the panel focused on actionable flags while still using the supervisor endpoint. `listActive()` is used for all other roles.
  - Component pattern matches `CarePlanListComponent` exactly: constructor injection (not `inject()`), `TranslocoModule` in `imports`, overlay-click dismissal in dialogs.
  - `clinicalNote` is coerced to `null` when blank on submit (trim + falsy check), preventing empty-string notes from reaching the backend.
- **Tests:** 38 new spec tests (11 + 9 + 18); `ng build --configuration production` exit 0, zero TypeScript errors. Pre-existing test TS errors in `session.service.spec.ts`, `permission.service.spec.ts`, and `complete-session-dialog.component.spec.ts` are unrelated to this increment.

---

## 2026-05-13 — Planning Complete

10 increments defined.

| # | Title | Status |
|---|-------|--------|
| 1 | V67 Flyway migration: schema + seed | completed |
| 2 | New permissions in Permission.java / RolePermissions.java | completed |
| 3 | Domain entities and repositories | pending |
| 4 | Service layer and unit tests | pending |
| 5 | REST controllers | completed |
| 6 | Extend AppointmentResponse with activeRiskFlagTypes | completed |
| 7 | Frontend: models and service | pending |
| 8 | Frontend: RiskFlagsPanel on client detail | completed |
| 9 | Frontend: risk flag indicator on appointment detail | pending |
| 10 | Frontend: admin risk flag type configuration | pending |

---

## 2026-05-13 — Code Review (Increments 1 and 2)
- Quality: PASS (Critical: 0, High: 0)
- Coverage: FULLY COVERED
- Recommendation: approve

---

## 2026-05-13 — Test Run (Increments 1 and 2, attempt 1)
- Passed: 413 | Failed: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Failures: none
- Key results:
  - `RolePermissionsRiskFlagsTest`: 15/15 passed (pure unit, no Spring context)
  - `RbacIntegrationTest`: 10/10 passed (full Spring context + H2, no regression)
  - `TokenServiceTest`: 16/16 passed (new permissions propagate to JWT without breakage)
  - `AuthServiceTest`: 12/12 passed
  - `AuthControllerTest`: 8/8 passed
- Action: all clear — no failures, no regressions

---

## 2026-05-13 — Test Run (Increment 3, attempt 1)
- Passed: 431 | Failed: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Failures: none
- Key results:
  - `RiskFlagTypeTest`: 6/6 passed (pure unit, no Spring context)
  - `ClientRiskFlagTest`: 8/8 passed (pure unit, no Spring context)
  - `RiskFlagAuditLogTest`: 4/4 passed (pure unit, no Spring context)
  - All 18 new Increment 3 tests passed; 413 pre-existing tests passed without regression
- Action: all clear — no failures, no regressions

---

## 2026-05-13 — Code Review (Increment 3)
- Quality: PASS (Critical: 0, High: 0, Medium: 3)
- Coverage: FULLY COVERED
- Recommendation: approve

---

## 2026-05-13 — Code Review (Increment 4)
- Quality: PASS (Critical: 0, High: 1, Medium: 3, Low: 3)
- Coverage: FULLY COVERED
- Recommendation: approve with remediation required for N+1 before Increment 5 ships

---

## 2026-05-13 — Test Run (Increment 4, attempt 1)
- Passed: 441 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Failures: none
- Key results:
  - `RiskFlagServiceTest`: 10/10 passed (pure unit, Mockito only — no Spring context)
  - `RiskFlagTypeTest`: 6/6 passed
  - `ClientRiskFlagTest`: 8/8 passed
  - `RiskFlagAuditLogTest`: 4/4 passed
  - `RolePermissionsRiskFlagsTest`: 15/15 passed (no regression)
  - `RbacIntegrationTest`: 10/10 passed (no regression)
  - `TokenServiceTest`: 16/16 passed (no regression)
  - All 441 pre-existing and new tests passed without regression
- Action: all clear — no failures, no regressions

---

## 2026-05-13 — Test Run (Increment 5, attempt 1)
- Passed: 441 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Duration: 10.823s
- Failures: none
- Key results:
  - `RiskFlagServiceTest`: 10/10 passed (no regression from Increment 4)
  - `RiskFlagTypeTest`: 6/6 passed
  - `ClientRiskFlagTest`: 8/8 passed
  - `RiskFlagAuditLogTest`: 4/4 passed
  - `RolePermissionsRiskFlagsTest`: 15/15 passed (no regression)
  - `RbacIntegrationTest`: 10/10 passed (no regression)
  - `TokenServiceTest`: 16/16 passed (no regression)
  - `AuthControllerTest`: 8/8 passed
  - `AuthServiceTest`: 12/12 passed
  - All 441 pre-existing and Increment 5 tests passed without regression
- Action: all clear — no failures, no regressions

## 2026-05-13 — Code Review (Increment 5)
- Quality: PASS (Critical: 0, High: 1, Medium: 1)
- Coverage: FULLY COVERED
- Recommendation: fix and re-review (High finding must be addressed before this increment ships)

---

## 2026-05-13 — Code Review (Increment 6)
- Quality: PASS (Critical: 0, High: 1, Medium: 2)
- Coverage: FULLY COVERED
- Recommendation: fix and re-review (High finding must be addressed before this increment ships)

---

## 2026-05-13 — Test Run (Increment 6, attempt 1)
- Passed: 461 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Duration: ~8s
- Failures: none
- Key results:
  - `AppointmentMapperTest`: 7/7 passed (new Increment 6 tests — all branches covered)
  - `RiskFlagServiceTest`: 10/10 passed (no regression from Increment 4)
  - `RiskFlagControllerTest`: 13/13 passed (no regression from Increment 5)
  - `RolePermissionsRiskFlagsTest`: 15/15 passed (no regression)
  - `RbacIntegrationTest`: 10/10 passed (no regression)
  - `TokenServiceTest`: 16/16 passed (no regression)
  - `AppointmentServiceTest`: 13/13 passed (no regression)
  - All 461 pre-existing and Increment 6 tests passed without regression
  - Confirmed: 454 pre-existing + 7 new `AppointmentMapperTest` = 461 total
- Action: all clear — no failures, no regressions

---

## 2026-05-13 — Code Review (Increment 7)
- Quality: PASS (Critical: 0, High: 0, Medium: 0)
- Coverage: FULLY COVERED
- Recommendation: approve

---

## 2026-05-13 — Test Run (Increment 7, attempt 1)
- Passed: 464 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Duration: 11.619s
- Failures: none
- Angular build: EXIT_CODE 0 — zero TypeScript errors; 13 bundle warnings (pre-existing CSS budget overruns and quill CommonJS warning, all pre-existing and unrelated to Increment 7)
- Key results:
  - All 464 backend tests passed (no regression from Increment 7 frontend-only changes)
  - `AppointmentMapperTest`: 10/10 passed (no regression from Increment 6)
  - `RiskFlagControllerTest`: 13/13 passed
  - `RiskFlagServiceTest`: 10/10 passed
  - `AppointmentServiceTest`: 13/13 passed
  - `RolePermissionsRiskFlagsTest`: 15/15 passed
  - `RbacIntegrationTest`: 10/10 passed
  - `TokenServiceTest`: 16/16 passed
  - Angular `ng build --configuration production` completed successfully (exit 0)
- Action: all clear — no failures, no regressions

---

## 2026-05-13 — Code Review (Increment 8)
- Quality: PASS (Critical: 0, High: 0, Medium: 4)
- Coverage: FULLY COVERED
- Recommendation: approve (medium findings are improvements, not blockers)

---

## 2026-05-13 — Test Run (Increment 8, attempt 1)
- Passed: 464 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Duration: 10.966s (backend); Angular build exit 0
- Failures: none
- Key results:
  - All 464 backend tests passed (no regression — Increment 8 is frontend-only)
  - `AppointmentMapperTest`: 10/10 passed
  - `RiskFlagControllerTest`: 13/13 passed
  - `RiskFlagServiceTest`: 10/10 passed
  - `AppointmentServiceTest`: 13/13 passed
  - `RolePermissionsRiskFlagsTest`: 15/15 passed
  - `RbacIntegrationTest`: 10/10 passed
  - `TokenServiceTest`: 16/16 passed
  - Angular `ng build --configuration production` exit code: 0, zero TypeScript errors
  - 5 pre-existing warnings (CSS budget overruns for 4 unrelated components, quill CommonJS) — all pre-existing, none from Increment 8
- Action: all clear — no failures, no regressions

---

## 2026-05-13 — Increment 9: Frontend: risk flag indicator on appointment detail

- **What was completed:** Added `activeRiskFlagTypes: string[]` to the `Appointment` interface and added a risk flag chips section to the appointment-edit-dialog template. The chips section renders only when at least one active flag is present.
- **Interfaces/methods created:**
  - `Appointment.activeRiskFlagTypes: string[]` — new field on the `Appointment` interface in `schedule.model.ts`
- **Files created/modified:**
  - `frontend/src/app/features/schedule/models/schedule.model.ts` (modified — added `activeRiskFlagTypes: string[]` with a PA-27 comment)
  - `frontend/src/app/features/schedule/components/appointment-edit-dialog/appointment-edit-dialog.component.ts` (modified — risk flag chips section added to template between appointment details and status selection; CSS for `.risk-flag-chips` and `.risk-flag-chip` added to component styles)
  - `frontend/src/app/features/schedule/components/appointment-edit-dialog/appointment-edit-dialog.component.spec.ts` (created — 12 tests covering chip rendering, zero-flag no-render, undefined guard, existing dialog behaviour)
  - `frontend/src/assets/i18n/en.json` (modified — added `schedule.appointment.edit.riskFlagsLabel: "Risk Flags"`)
  - `frontend/src/assets/i18n/uk.json` (modified — added `schedule.appointment.edit.riskFlagsLabel: "Прапорці ризику"`)
- **Decisions made:**
  - `CalendarAppointmentBlock` in `calendar.model.ts` was confirmed separate from `Appointment` in `schedule.model.ts` — it was NOT modified, avoiding any N+1 risk on the calendar grid.
  - Chip section placed between appointment details (time/duration) and status selection — visible context before the user interacts.
  - `*ngIf="appointment.activeRiskFlagTypes && appointment.activeRiskFlagTypes.length > 0"` guards against both `undefined` and empty arrays, future-proofing callers that populate the detail endpoint response vs. list endpoint responses.
  - Chip style matches spec exactly: `background: #FEE2E2; color: #991B1B; border-radius: 4px; padding: 2px 8px; font-size: 0.75rem`.
- **Tests:** 12 new spec tests; `ng build --configuration production` exit 0, zero TypeScript errors. Pre-existing warnings (CSS budget overruns, quill CommonJS) are unrelated.

---

## 2026-05-13 — Test Run (Increment 9, attempt 1)
- Passed: 464 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Duration: 11.244s (backend); Angular build exit 0
- Failures: none
- Key results:
  - Backend: 464/464 passed — no regression (Increment 9 is frontend-only)
  - AppointmentMapperTest: 10/10 passed
  - RiskFlagControllerTest: 13/13 passed
  - RiskFlagServiceTest: 10/10 passed
  - AppointmentServiceTest: 13/13 passed
  - RolePermissionsRiskFlagsTest: 15/15 passed
  - RbacIntegrationTest: 10/10 passed
  - TokenServiceTest: 16/16 passed
  - Angular `ng build --configuration production` exit code: 0, zero TypeScript errors
  - 3 pre-existing warnings (CSS budget overruns: pending-leave-requests.component.scss, schedule-management.component.ts; quill CommonJS) — all pre-existing, none from Increment 9
- Action: all clear — no failures, no regressions

---

## 2026-05-13 — Code Review (Increment 9)
- Quality: PASS (Critical: 0, High: 0)
- Coverage: FULLY COVERED
- Recommendation: approve

---

## 2026-05-13 — Increment 10: Frontend: admin risk flag type configuration

- **What was completed:** Created the full admin panel for SYSTEM_ADMINISTRATOR to view, create, and deactivate risk flag types. All five files specified were created; two existing files were modified; i18n translations added for both `en.json` and `uk.json`.
- **Interfaces/methods created:**
  - `RiskFlagType` interface in `risk-flag-type-admin.model.ts`: `id, name, displayOrder, active` — mirrors `RiskFlagType` from Increment 7
  - `RiskFlagTypeAdminService.listAll(): Observable<RiskFlagType[]>` — `GET /api/v1/admin/risk-flag-types`
  - `RiskFlagTypeAdminService.create(name: string, displayOrder: number): Observable<RiskFlagType>` — `POST /api/v1/admin/risk-flag-types`
  - `RiskFlagTypeAdminService.deactivate(id: string): Observable<void>` — `PATCH /api/v1/admin/risk-flag-types/{id}/deactivate`
  - `RiskFlagTypeListComponent` — standalone; loads list on init, "Add Flag Type" button opens dialog, per-row "Deactivate" button for active types
  - `RiskFlagTypeFormDialogComponent` — standalone; reactive form with `name` (`required`, `maxLength(100)`) and `displayOrder` (`required`, `min(0)`); outputs `saved` and `cancelled`
- **Files created:**
  - `frontend/src/app/features/admin/risk-flag-types/models/risk-flag-type-admin.model.ts`
  - `frontend/src/app/features/admin/risk-flag-types/services/risk-flag-type-admin.service.ts`
  - `frontend/src/app/features/admin/risk-flag-types/services/risk-flag-type-admin.service.spec.ts` (11 tests)
  - `frontend/src/app/features/admin/risk-flag-types/components/risk-flag-type-list/risk-flag-type-list.component.ts`
  - `frontend/src/app/features/admin/risk-flag-types/components/risk-flag-type-list/risk-flag-type-list.component.spec.ts` (11 tests)
  - `frontend/src/app/features/admin/risk-flag-types/components/risk-flag-type-form-dialog/risk-flag-type-form-dialog.component.ts`
  - `frontend/src/app/features/admin/risk-flag-types/components/risk-flag-type-form-dialog/risk-flag-type-form-dialog.component.spec.ts` (13 tests)
  - `frontend/src/app/features/admin/risk-flag-types/risk-flag-types.routes.ts`
- **Files modified:**
  - `frontend/src/app/features/admin/admin.routes.ts` — added `risk-flag-types` lazy-loaded child route matching `notification-templates` pattern exactly
  - `frontend/src/app/features/admin/admin-layout.component.ts` — added `rla6` nav tab for `/admin/risk-flag-types`
  - `frontend/src/assets/i18n/en.json` — added `admin.tabs.riskFlagTypes`, `admin.riskFlagTypes.title`, `admin.riskFlagTypes.list.*`, `admin.riskFlagTypes.form.*`
  - `frontend/src/assets/i18n/uk.json` — same keys in Ukrainian
- **Decisions made:**
  - Constructor injection (`constructor(private http: HttpClient)`) used throughout — matches all existing services in this codebase; `inject()` is not used in any existing service.
  - `RiskFlagTypeAdminService` uses a single `baseUrl = '/api/v1/admin/risk-flag-types'` constant — consistent with `NotificationTemplateService.baseUrl` pattern.
  - `RiskFlagTypeFormDialogComponent` uses `ReactiveFormsModule` (not template-driven `FormsModule`) because the spec calls for reactive forms with `Validators.required`, `Validators.maxLength`, `Validators.min` — same pattern as `RiskFlagFormDialogComponent` from Increment 8.
  - `RiskFlagTypeListComponent` does not import `FormsModule` since it has no two-way bindings — minimises imports.
  - Translation keys follow existing `admin.<feature>.*` pattern established by `notificationTemplates`.
- **Tests:** 35 new spec tests (11 service + 11 list + 13 form dialog); `ng build --configuration production` exit 0, zero TypeScript errors. All pre-existing warnings (CSS budget overruns, quill CommonJS) are unrelated to Increment 10.

---

## 2026-05-13 — Test Run (Increment 10, attempt 1)
- Passed: 464 | Failed: 0 | Skipped: 0 | Coverage: N/A (JaCoCo not configured in pom.xml)
- Coverage gate: N/A
- Duration: 10.592s (backend); Angular build exit 0
- Failures: none
- Key results:
  - Backend: 464/464 passed — exact expected count confirmed, no regression (Increment 10 is frontend-only)
  - AppointmentMapperTest: 10/10 passed
  - RiskFlagControllerTest: 13/13 passed
  - RiskFlagServiceTest: 10/10 passed
  - AppointmentServiceTest: 13/13 passed
  - RolePermissionsRiskFlagsTest: 15/15 passed
  - RbacIntegrationTest: 10/10 passed
  - TokenServiceTest: 16/16 passed
  - Angular `ng build --configuration production` exit code: 0, zero TypeScript errors
  - 3 pre-existing warnings (CSS budget overruns: session-list.component.scss, appointment-booking-dialog.component.ts; quill CommonJS) — all pre-existing, none from Increment 10
- Action: all clear — final increment clean, no failures, no regressions

---

## 2026-05-13 — Code Review (Increment 10)
- Quality: PASS (Critical: 0, High: 0, Medium: 2, Suggestion: 2)
- Coverage: FULLY COVERED
- Recommendation: approve
