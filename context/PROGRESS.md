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

## 2026-05-13 — Planning Complete

10 increments defined.

| # | Title | Status |
|---|-------|--------|
| 1 | V67 Flyway migration: schema + seed | completed |
| 2 | New permissions in Permission.java / RolePermissions.java | completed |
| 3 | Domain entities and repositories | pending |
| 4 | Service layer and unit tests | pending |
| 5 | REST controllers | pending |
| 6 | Extend AppointmentResponse with activeRiskFlagTypes | pending |
| 7 | Frontend: models and service | pending |
| 8 | Frontend: RiskFlagsPanel on client detail | pending |
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
