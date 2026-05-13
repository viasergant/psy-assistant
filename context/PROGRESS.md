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
