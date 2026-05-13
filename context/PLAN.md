# PA-27 — Risk Flag Indicators on Client Profiles

## Overview

Structured, role-protected risk flag indicators on client profiles. Replaces the coarse `is_at_risk` boolean (added in V59 for no-show tracking) with a full risk-flag subsystem: configurable flag types, per-flag clinical notes, mandatory review dates, resolve workflow with resolution notes, and an append-only audit log. Access to clinical notes is restricted by role; flag type labels are visible to all staff with client access.

## Architecture Notes

### Backend

- New package: `com.psyassistant.riskflags` with subpackages `domain/`, `dto/`, `repository/`, `rest/`, `service/`
- Three domain entities: `RiskFlagType`, `ClientRiskFlag` (status enum `ACTIVE`/`RESOLVED`), `RiskFlagAuditLog`
- `RiskFlagAuditLog` is append-only — no update/delete methods exposed at any layer. Modelled after the existing `care_plan_audit` pattern (inline fields, no FK on `flag_id` to survive flag deletion, server-managed `action_timestamp`)
- Assignment check for THERAPIST role mirrors `ClientProfileService.enforceAssignedTherapistRead()` — compares `currentPrincipalId()` against `client.getAssignedTherapistId()`
- `AppointmentResponse` is a Java `record` — to add `activeRiskFlagTypes` it must be replaced with a new record that includes the field. `AppointmentMapper.toResponse()` must be extended accordingly; `CalendarAppointmentBlock` is a separate record used for calendar views and is NOT changed (avoids N+1 on the calendar grid)
- `RiskFlagTypeController` uses two base paths: public read at `/api/v1/risk-flag-types` (all authenticated staff) and admin CRUD at `/api/v1/admin/risk-flag-types`
- New permissions added to `Permission.java` and `RolePermissions.java` — propagate automatically to JWTs via `TokenService` (no other wiring needed)

### Frontend

- New Angular feature module at `frontend/src/app/features/clients/components/risk-flags/`
- Admin sub-feature at `frontend/src/app/features/admin/risk-flag-types/`
- All components are standalone with `inject()` for DI, reactive forms, `TranslocoModule` for i18n, matching existing component conventions
- `RiskFlagService` lives at `providedIn: 'root'`
- Risk flag chips on appointment view are added to `AppointmentBlockComponent` — requires extending `CalendarAppointmentBlock` frontend model with `activeRiskFlagTypes: string[]` and updating `CalendarMapper` backend to populate it

### DB Migration

- V67 — three new tables plus seed data. `risk_flag_types.active` column enables soft-delete without data loss. `client_risk_flags.flag_type_id` carries a FK; `risk_flag_audit_log` intentionally has no FK on `flag_id` (mirrors `care_plan_audit` pattern) to preserve history if a flag is ever hard-deleted in the future.

---

## Increments

---

### Increment 1 — V67 Flyway migration: schema + seed [completed]

**Goal:** Create all three tables and seed the initial five flag types. No Java code changes.

**Files to create/modify:**
- `backend/src/main/resources/db/migration/V67__risk_flag_tables.sql` (create)

**Tasks:**
1. Create `risk_flag_types` table: `id UUID PK`, `name VARCHAR(100) NOT NULL UNIQUE`, `display_order SMALLINT NOT NULL DEFAULT 0`, `active BOOLEAN NOT NULL DEFAULT TRUE`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
2. Create `client_risk_flags` table: `id UUID PK DEFAULT gen_random_uuid()`, `client_id UUID NOT NULL REFERENCES clients(id)`, `flag_type_id UUID NOT NULL REFERENCES risk_flag_types(id)`, `status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','RESOLVED'))`, `clinical_note TEXT`, `review_date DATE NOT NULL`, `created_by_user_id UUID NOT NULL REFERENCES users(id)`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `resolved_by_user_id UUID REFERENCES users(id)`, `resolved_at TIMESTAMPTZ`, `resolution_note TEXT`
3. Create `risk_flag_audit_log` table: `id UUID PK DEFAULT gen_random_uuid()`, `flag_id UUID NOT NULL` (no FK — append-only intent), `client_id UUID NOT NULL`, `actor_user_id UUID NOT NULL REFERENCES users(id)`, `actor_name VARCHAR(255) NOT NULL`, `action_type VARCHAR(30) NOT NULL CHECK (action_type IN ('FLAG_CREATED','FLAG_RESOLVED','FLAG_UPDATED'))`, `flag_type_name VARCHAR(100) NOT NULL`, `status VARCHAR(20) NOT NULL`, `action_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()`
4. Add indexes: `idx_risk_flags_client_status ON client_risk_flags(client_id, status)`, `idx_risk_flag_audit_client ON risk_flag_audit_log(client_id, action_timestamp DESC)`, `idx_risk_flag_audit_flag ON risk_flag_audit_log(flag_id)`
5. Seed five initial flag types with `display_order` 1–5: Self-Harm Risk, Crisis History, Safeguarding Concern, Domestic Abuse Concern, Suicidal Ideation

**Acceptance criteria:**
- `mvn flyway:migrate` runs clean on a fresh schema
- All five seed rows present in `risk_flag_types`
- FK constraints enforce referential integrity
- `risk_flag_audit_log` has no FK on `flag_id`

**Dependencies:** none

---

### Increment 2 — New permissions in Permission.java and RolePermissions.java [completed]

**Goal:** Add four new permission constants and assign them to roles. JWT picks them up automatically via existing `TokenService`.

**Files to modify:**
- `backend/src/main/java/com/psyassistant/common/security/Permission.java`
- `backend/src/main/java/com/psyassistant/common/security/RolePermissions.java`

**Tasks:**
1. Add to `Permission.java` (in a new `// ---- Risk flags` section):
   - `MANAGE_RISK_FLAGS` — create/update/resolve risk flags
   - `READ_RISK_FLAGS` — read risk flag type labels
   - `READ_RISK_FLAG_NOTES` — read clinical notes
   - `MANAGE_RISK_FLAG_TYPES` — CRUD for flag type config
2. Update `RolePermissions.java` role matrix:
   - `RECEPTION_ADMIN_STAFF`: add `READ_RISK_FLAGS`
   - `THERAPIST`: add `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`
   - `SUPERVISOR`: add `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES`
   - `FINANCE`: no new permissions
   - `SYSTEM_ADMINISTRATOR`: add all four new permissions
3. Update the Javadoc permission matrix table in `RolePermissions.java`

**Acceptance criteria:**
- `RolePermissions.permissionsFor(UserRole.RECEPTION_ADMIN_STAFF)` contains `READ_RISK_FLAGS` but not `MANAGE_RISK_FLAGS`
- `RolePermissions.permissionsFor(UserRole.THERAPIST)` contains `MANAGE_RISK_FLAGS`, `READ_RISK_FLAGS`, `READ_RISK_FLAG_NOTES` but not `MANAGE_RISK_FLAG_TYPES`
- `RolePermissions.permissionsFor(UserRole.SYSTEM_ADMINISTRATOR)` contains all four
- Existing `RbacIntegrationTest` still passes

**Dependencies:** none (can run parallel to Increment 1)

---

### Increment 3 — Domain entities and repositories

**Goal:** Define the three JPA entities and their Spring Data repositories.

**Files to create:**
- `backend/src/main/java/com/psyassistant/riskflags/domain/RiskFlagType.java`
- `backend/src/main/java/com/psyassistant/riskflags/domain/ClientRiskFlag.java`
- `backend/src/main/java/com/psyassistant/riskflags/domain/ClientRiskFlagStatus.java` (enum)
- `backend/src/main/java/com/psyassistant/riskflags/domain/RiskFlagAuditLog.java`
- `backend/src/main/java/com/psyassistant/riskflags/domain/RiskFlagAuditActionType.java` (enum)
- `backend/src/main/java/com/psyassistant/riskflags/repository/RiskFlagTypeRepository.java`
- `backend/src/main/java/com/psyassistant/riskflags/repository/ClientRiskFlagRepository.java`
- `backend/src/main/java/com/psyassistant/riskflags/repository/RiskFlagAuditLogRepository.java`

**Tasks:**

`RiskFlagType` — extends `SimpleBaseEntity`:
- `name VARCHAR(100) NOT NULL UNIQUE`
- `displayOrder SMALLINT`
- `active BOOLEAN NOT NULL DEFAULT TRUE`
- No `updatedAt` needed (config data) — but `SimpleBaseEntity` provides it automatically; acceptable

`ClientRiskFlag` — plain `@Entity` (not extending base classes since it has no `updatedAt` or `createdBy` from Spring auditing — use explicit columns to match schema):
- `@Id UUID id` with `@GeneratedValue(strategy = GenerationType.UUID)`
- `UUID clientId`, `UUID flagTypeId`, `@Enumerated(EnumType.STRING) ClientRiskFlagStatus status`
- `String clinicalNote` (TEXT), `LocalDate reviewDate` (NOT NULL)
- `UUID createdByUserId`, `Instant createdAt` (`insertable=true, updatable=false, nullable=false`)
- `UUID resolvedByUserId`, `Instant resolvedAt`, `String resolutionNote`

`RiskFlagAuditLog` — append-only, plain `@Entity`:
- `@Id UUID id` with `@GeneratedValue(strategy = GenerationType.UUID)`
- `UUID flagId`, `UUID clientId` (NOT NULL)
- `UUID actorUserId`, `String actorName` (NOT NULL)
- `@Enumerated(EnumType.STRING) RiskFlagAuditActionType actionType`
- `String flagTypeName`, `String status`
- `Instant actionTimestamp` (`insertable=false, updatable=false` — DB default NOW())

Repositories:
- `RiskFlagTypeRepository extends JpaRepository<RiskFlagType, UUID>`: query `findAllByActiveTrueOrderByDisplayOrderAsc()`
- `ClientRiskFlagRepository extends JpaRepository<ClientRiskFlag, UUID>`: queries `findAllByClientIdAndStatus(UUID, ClientRiskFlagStatus)`, `findAllByClientId(UUID)` (for supervisor full history)
- `RiskFlagAuditLogRepository extends JpaRepository<RiskFlagAuditLog, UUID>` — no update/delete methods

**Acceptance criteria:**
- Application context loads cleanly with Flyway V67 in place
- `RiskFlagAuditLogRepository` exposes only `save()` and read methods (no `delete*`, no `update*`)

**Dependencies:** Increment 1

---

### Increment 4 — Service layer and unit tests

**Goal:** Implement `RiskFlagService` and `RiskFlagTypeService` with all business rules; cover with unit tests.

**Files to create:**
- `backend/src/main/java/com/psyassistant/riskflags/dto/CreateRiskFlagRequest.java`
- `backend/src/main/java/com/psyassistant/riskflags/dto/ResolveRiskFlagRequest.java`
- `backend/src/main/java/com/psyassistant/riskflags/dto/RiskFlagResponse.java`
- `backend/src/main/java/com/psyassistant/riskflags/dto/RiskFlagTypeResponse.java`
- `backend/src/main/java/com/psyassistant/riskflags/service/RiskFlagService.java`
- `backend/src/main/java/com/psyassistant/riskflags/service/RiskFlagTypeService.java`
- `backend/src/test/java/com/psyassistant/riskflags/service/RiskFlagServiceTest.java`

**Tasks:**

`CreateRiskFlagRequest` record: `flagTypeId UUID @NotNull`, `clinicalNote String`, `reviewDate LocalDate @NotNull @FutureOrPresent`

`ResolveRiskFlagRequest` record: `resolutionNote String @NotBlank`

`RiskFlagResponse` record: all `ClientRiskFlag` fields + `flagTypeName String`. Include `clinicalNote` in the response object — the controller will null it out for callers without `READ_RISK_FLAG_NOTES`.

`RiskFlagTypeResponse` record: `id UUID`, `name String`, `displayOrder int`, `active boolean`

`RiskFlagService`:
- `createFlag(UUID clientId, CreateRiskFlagRequest request, UUID actorId, String actorName)`:
  1. Require `MANAGE_RISK_FLAGS` authority — throw `AccessDeniedException` if absent
  2. If THERAPIST role (has `READ_ASSIGNED_CLIENTS` but not `READ_CLIENTS_ALL`): load client, compare `client.getAssignedTherapistId()` with `actorId` — throw `AccessDeniedException` if not assigned
  3. Load `RiskFlagType` by ID — throw `EntityNotFoundException` if missing or inactive
  4. Build and persist `ClientRiskFlag` with `status=ACTIVE`, `createdByUserId=actorId`, `createdAt=Instant.now()`
  5. Append to `risk_flag_audit_log` with `action_type=FLAG_CREATED`
  6. Return `RiskFlagResponse`

- `resolveFlag(UUID clientId, UUID flagId, ResolveRiskFlagRequest request, UUID actorId, String actorName)`:
  1. Require `MANAGE_RISK_FLAGS`
  2. Load flag — throw `EntityNotFoundException` if not found or `clientId` doesn't match
  3. If already `RESOLVED` — throw `ResponseStatusException(422, "Flag is already resolved")`
  4. THERAPIST assignment check (same as create)
  5. Set `status=RESOLVED`, `resolvedByUserId=actorId`, `resolvedAt=Instant.now()`, `resolutionNote`
  6. Append audit with `action_type=FLAG_RESOLVED`
  7. Return `RiskFlagResponse`

- `listActiveFlags(UUID clientId)`: require `READ_RISK_FLAGS`; return flags with `status=ACTIVE`; strip `clinicalNote` if caller lacks `READ_RISK_FLAG_NOTES`
- `listAllFlags(UUID clientId)`: require `READ_RISK_FLAG_NOTES` (supervisor/SYS only — full history with all details)

`RiskFlagTypeService`:
- `listActive()`: returns `findAllByActiveTrueOrderByDisplayOrderAsc()` — no permission check (used by create form)
- `listAll()`: requires `MANAGE_RISK_FLAG_TYPES`
- `create(String name, int displayOrder)`: requires `MANAGE_RISK_FLAG_TYPES`
- `deactivate(UUID id)`: requires `MANAGE_RISK_FLAG_TYPES`

Unit tests for `RiskFlagService` covering:
- `createFlag` happy path: flag persisted + audit appended
- `createFlag` with caller lacking `MANAGE_RISK_FLAGS` → `AccessDeniedException`
- `createFlag` by THERAPIST not assigned to client → `AccessDeniedException`
- `createFlag` by THERAPIST who is assigned → succeeds
- `createFlag` with inactive flag type → `EntityNotFoundException`
- `resolveFlag` happy path: status updated + audit appended
- `resolveFlag` when already resolved → 422
- `resolveFlag` for flag belonging to different client → `EntityNotFoundException`
- `listActiveFlags` strips `clinicalNote` when caller lacks `READ_RISK_FLAG_NOTES`
- `listActiveFlags` includes `clinicalNote` when caller has `READ_RISK_FLAG_NOTES`

**Acceptance criteria:**
- All unit tests pass via `mvn test`
- No infrastructure dependency in tests (Mockito only)
- Audit log `save()` is called exactly once per `createFlag` and once per `resolveFlag`

**Dependencies:** Increments 2, 3

---

### Increment 5 — REST controllers

**Goal:** Expose the service layer via REST endpoints following existing controller conventions.

**Files to create:**
- `backend/src/main/java/com/psyassistant/riskflags/rest/RiskFlagController.java`
- `backend/src/main/java/com/psyassistant/riskflags/rest/RiskFlagTypeController.java`

**Tasks:**

`RiskFlagController` at `/api/v1/clients/{id}/risk-flags`:
- `GET /` — `@PreAuthorize("hasAuthority('READ_RISK_FLAGS')")` — calls `listActiveFlags()`; response omits `clinicalNote` for `RECEPTION_ADMIN_STAFF` (already handled in service)
- `GET /history` — `@PreAuthorize("hasAuthority('READ_RISK_FLAG_NOTES')")` — calls `listAllFlags()`
- `POST /` — `@PreAuthorize("hasAuthority('MANAGE_RISK_FLAGS')")` — calls `createFlag()`; returns 201 + `Location` header
- `PATCH /{flagId}/resolve` — `@PreAuthorize("hasAuthority('MANAGE_RISK_FLAGS')")` — calls `resolveFlag()`; returns 200

`RiskFlagTypeController`:
- `GET /api/v1/risk-flag-types` — no `@PreAuthorize` beyond authenticated (any role sees active types for the create form)
- `GET /api/v1/admin/risk-flag-types` — `@PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")`
- `POST /api/v1/admin/risk-flag-types` — `@PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")`
- `PATCH /api/v1/admin/risk-flag-types/{id}/deactivate` — `@PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")`

Both controllers use `UserManagementService.currentPrincipalId()` for `actorId` and `auth.getName()` for `actorName`, matching `ClientController` pattern.

**Acceptance criteria:**
- `GET /api/v1/clients/{id}/risk-flags` with a RECEPTION_ADMIN_STAFF token returns 200 with flags, no `clinicalNote` field populated
- `POST /api/v1/clients/{id}/risk-flags` with a RECEPTION_ADMIN_STAFF token returns 403
- `PATCH /api/v1/clients/{id}/risk-flags/{flagId}/resolve` with a FINANCE token returns 403
- `POST /api/v1/admin/risk-flag-types` with a THERAPIST token returns 403

**Dependencies:** Increment 4

---

### Increment 6 — Extend AppointmentResponse with activeRiskFlagTypes

**Goal:** Add `activeRiskFlagTypes: List<String>` to `AppointmentResponse` so appointment detail views can show risk flag labels without an extra HTTP call.

**Files to modify:**
- `backend/src/main/java/com/psyassistant/scheduling/dto/AppointmentResponse.java`
- `backend/src/main/java/com/psyassistant/scheduling/dto/AppointmentMapper.java`
- `backend/src/main/java/com/psyassistant/scheduling/service/AppointmentService.java`

**Tasks:**
1. `AppointmentResponse` record: add `List<String> activeRiskFlagTypes` as the last field
2. `AppointmentMapper`: add `ClientRiskFlagRepository` dependency; in `toResponse()`, query `findAllByClientIdAndStatus(clientId, ACTIVE)` and map to flag type names; pass the list into the record constructor
3. Update all existing `new AppointmentResponse(...)` call-sites (only `AppointmentMapper.toResponse()` constructs it — verify with grep before writing)
4. Verify `AppointmentMapper` tests still pass

Note: `CalendarAppointmentBlock` is intentionally NOT changed — the calendar grid renders many blocks at once and the N+1 cost is unacceptable. Risk flag chips are only shown on the appointment detail panel (opened on click), which uses `AppointmentResponse`.

**Acceptance criteria:**
- `GET /api/v1/appointments/{id}` response contains `"activeRiskFlagTypes": [...]`
- Existing appointment tests pass without modification (list may be empty but field must be present)
- No N+1 query regression on the calendar week view

**Dependencies:** Increments 3, 5

---

### Increment 7 — Frontend: models and service

**Goal:** Define TypeScript models and an HTTP service for the risk flags feature.

**Files to create:**
- `frontend/src/app/features/clients/components/risk-flags/models/risk-flag.model.ts`
- `frontend/src/app/features/clients/components/risk-flags/services/risk-flag.service.ts`

**Tasks:**

`risk-flag.model.ts`:
```typescript
export type RiskFlagStatus = 'ACTIVE' | 'RESOLVED';

export interface RiskFlagType {
  id: string;
  name: string;
  displayOrder: number;
  active: boolean;
}

export interface RiskFlag {
  id: string;
  clientId: string;
  flagTypeId: string;
  flagTypeName: string;
  status: RiskFlagStatus;
  clinicalNote: string | null;      // null when caller lacks READ_RISK_FLAG_NOTES
  reviewDate: string;               // ISO date
  createdByUserId: string;
  createdAt: string;
  resolvedByUserId: string | null;
  resolvedAt: string | null;
  resolutionNote: string | null;
}

export interface CreateRiskFlagPayload {
  flagTypeId: string;
  clinicalNote: string | null;
  reviewDate: string;               // ISO date
}

export interface ResolveRiskFlagPayload {
  resolutionNote: string;
}
```

`RiskFlagService` (`@Injectable({ providedIn: 'root' })`):
- `listActive(clientId: string): Observable<RiskFlag[]>` — `GET /api/v1/clients/{id}/risk-flags`
- `listAll(clientId: string): Observable<RiskFlag[]>` — `GET /api/v1/clients/{id}/risk-flags/history`
- `create(clientId: string, payload: CreateRiskFlagPayload): Observable<RiskFlag>` — `POST /api/v1/clients/{id}/risk-flags`
- `resolve(clientId: string, flagId: string, payload: ResolveRiskFlagPayload): Observable<RiskFlag>` — `PATCH /api/v1/clients/{id}/risk-flags/{flagId}/resolve`
- `listTypes(): Observable<RiskFlagType[]>` — `GET /api/v1/risk-flag-types`

**Acceptance criteria:**
- Service compiles with no TypeScript errors
- Models match backend `RiskFlagResponse` field names exactly

**Dependencies:** Increment 5

---

### Increment 8 — Frontend: RiskFlagsPanel on client detail

**Goal:** Render active risk flags on the client detail page, with add-flag and resolve-flag dialogs. Visibility of clinical notes is governed by the permission embedded in the JWT.

**Files to create:**
- `frontend/src/app/features/clients/components/risk-flags/risk-flags-panel/risk-flags-panel.component.ts`
- `frontend/src/app/features/clients/components/risk-flags/risk-flag-form-dialog/risk-flag-form-dialog.component.ts`
- `frontend/src/app/features/clients/components/risk-flags/risk-flag-resolve-dialog/risk-flag-resolve-dialog.component.ts`

**Files to modify:**
- `frontend/src/app/features/clients/client-detail/client-detail.component.ts` (add `RiskFlagsPanelComponent` to the imports and template)

**Tasks:**

`RiskFlagsPanelComponent` (standalone):
- Inputs: `clientId: string`, `canManage: boolean` (derived from `MANAGE_RISK_FLAGS` in token), `canReadNotes: boolean`
- On `ngOnInit`: call `riskFlagService.listActive(clientId)` (or `listAll` if supervisor)
- Display list of active flags as cards: flag type name chip (color-coded by status), review date, resolution info if resolved
- Show `clinicalNote` block only when `canReadNotes && flag.clinicalNote != null`
- "Add flag" button (visible when `canManage`): opens `RiskFlagFormDialogComponent`
- "Resolve" button per active flag (visible when `canManage`): opens `RiskFlagResolveDialogComponent`

`RiskFlagFormDialogComponent` (standalone):
- Inputs: `clientId: string`; Outputs: `saved: EventEmitter<void>`, `cancelled: EventEmitter<void>`
- Reactive form: `flagTypeId` (select, required), `clinicalNote` (textarea), `reviewDate` (date, required, today or future)
- On init: load flag types via `riskFlagService.listTypes()`
- On submit: call `riskFlagService.create()`; emit `saved` on success

`RiskFlagResolveDialogComponent` (standalone):
- Inputs: `clientId: string`, `flagId: string`; Outputs: `resolved: EventEmitter<void>`, `cancelled: EventEmitter<void>`
- Reactive form: `resolutionNote` (textarea, required, `@NotBlank` enforced in template with `Validators.required`)
- On submit: call `riskFlagService.resolve()`; emit `resolved` on success

Integration into `client-detail.component.ts`:
- Add `RiskFlagsPanelComponent` to the `imports` array
- Add a "Risk Flags" section/tab below the existing profile section
- Pass `[clientId]="client.id"`, `[canManage]="hasPermission('MANAGE_RISK_FLAGS')"`, `[canReadNotes]="hasPermission('READ_RISK_FLAG_NOTES')"`
- The existing `client-detail` component already checks permissions against `auth.authorities` — follow the same pattern

**Acceptance criteria:**
- Panel renders without error for RECEPTION_ADMIN_STAFF (list visible, no add/resolve buttons, no clinical notes)
- Panel renders with add/resolve controls for THERAPIST (assigned client)
- FormDialog validation prevents submission without `reviewDate` or `flagTypeId`
- Resolve dialog prevents submission with empty `resolutionNote`

**Dependencies:** Increments 7

---

### Increment 9 — Frontend: risk flag indicator on appointment detail

**Goal:** Show active risk flag type labels in the appointment detail panel.

**Files to modify:**
- `frontend/src/app/features/schedule/models/schedule.model.ts` — add `activeRiskFlagTypes: string[]` to `Appointment` interface
- `frontend/src/app/features/schedule/components/appointment-edit-dialog/appointment-edit-dialog.component.ts` — add flag chip display

**Tasks:**
1. In `schedule.model.ts`, add `activeRiskFlagTypes: string[]` to the `Appointment` interface (the model that maps to `AppointmentResponse`)
2. In `appointment-edit-dialog.component.ts`, add a risk flags section in the template: if `appointment.activeRiskFlagTypes.length > 0`, display a row of styled chips showing each flag type name
3. Style: small `background: #FEE2E2; color: #991B1B; border-radius: 4px; padding: 2px 8px; font-size: 0.75rem` chips — consistent with existing status badges

Note: `CalendarAppointmentBlock` model is NOT changed. Risk flag chips only appear in the appointment detail panel, not on the calendar grid blocks.

**Acceptance criteria:**
- When a client has active risk flags, opening an appointment detail panel shows the flag type name chips
- When no active flags, no chips section is rendered
- Existing appointment dialog behavior unchanged

**Dependencies:** Increments 6, 8

---

### Increment 10 — Frontend: admin risk flag type configuration

**Goal:** Allow SYSTEM_ADMINISTRATOR to view, create, and deactivate flag types through the admin panel.

**Files to create:**
- `frontend/src/app/features/admin/risk-flag-types/models/risk-flag-type-admin.model.ts`
- `frontend/src/app/features/admin/risk-flag-types/services/risk-flag-type-admin.service.ts`
- `frontend/src/app/features/admin/risk-flag-types/components/risk-flag-type-list/risk-flag-type-list.component.ts`
- `frontend/src/app/features/admin/risk-flag-types/components/risk-flag-type-form-dialog/risk-flag-type-form-dialog.component.ts`
- `frontend/src/app/features/admin/risk-flag-types/risk-flag-types.routes.ts`

**Files to modify:**
- `frontend/src/app/features/admin/admin.routes.ts` — add `risk-flag-types` route
- `frontend/src/app/features/admin/admin-layout.component.ts` — add nav link if sidebar exists

**Tasks:**

`risk-flag-type-admin.model.ts`: mirror `RiskFlagType` from Increment 7 model

`RiskFlagTypeAdminService` (`@Injectable({ providedIn: 'root' })`):
- `listAll(): Observable<RiskFlagType[]>` — `GET /api/v1/admin/risk-flag-types`
- `create(name: string, displayOrder: number): Observable<RiskFlagType>` — `POST /api/v1/admin/risk-flag-types`
- `deactivate(id: string): Observable<void>` — `PATCH /api/v1/admin/risk-flag-types/{id}/deactivate`

`RiskFlagTypeListComponent` (standalone):
- Table of all flag types: name, display order, active status, actions column
- "Add Flag Type" button: opens `RiskFlagTypeFormDialogComponent`
- "Deactivate" button per active type: calls service + reloads list

`RiskFlagTypeFormDialogComponent` (standalone):
- Reactive form: `name` (text, required, maxLength 100), `displayOrder` (number, required, min 0)
- On submit: call `riskFlagTypeAdminService.create()`

`risk-flag-types.routes.ts`:
```typescript
export default [
  { path: '', loadComponent: () => import('./components/risk-flag-type-list/...').then(m => m.RiskFlagTypeListComponent) }
] satisfies Routes;
```

Add to `admin.routes.ts`:
```typescript
{ path: 'risk-flag-types', loadChildren: () => import('./risk-flag-types/risk-flag-types.routes') }
```

**Acceptance criteria:**
- Route `/admin/risk-flag-types` loads list component
- Adding a new type via the form persists it and it appears in the list
- Deactivating a type causes it to show as inactive; it no longer appears in the create-flag dropdown
- Non-admin tokens cannot reach `POST /api/v1/admin/risk-flag-types` (403)

**Dependencies:** Increments 7, 5

---

## Risks

1. `AppointmentResponse` is a Java `record` — every call-site that constructs `new AppointmentResponse(...)` must be updated when the field is added. There is only one such site (`AppointmentMapper.toResponse()`), confirmed by code inspection, but a grep before writing is mandatory.
2. N+1 on appointment list endpoints — `AppointmentMapper` will now query `ClientRiskFlagRepository` per appointment. This is safe for single-appointment detail endpoints. It must NOT be applied to list/calendar endpoints. The plan scope explicitly excludes `CalendarAppointmentBlock`.
3. THERAPIST assignment check requires `client.getAssignedTherapistId()` to be populated. The `Client` entity has this field. If an older client record has a null `assignedTherapistId`, the THERAPIST will be denied — this is intentional and matches existing behavior in `enforceAssignedTherapistRead()`.
4. Frontend permission checking relies on JWT claims. The existing pattern in `client-detail.component.ts` (checking `auth.authorities`) must be followed exactly.

## Success Criteria

- All five acceptance criteria groups from the ticket are met
- Zero clinical notes exposed to RECEPTION_ADMIN_STAFF in any API response (verified by role-boundary tests in Increment 5)
- Audit log rows are immutable: no update/delete at any layer
- All unit tests pass: `mvn test`
- Frontend compiles: `ng build --configuration production`
