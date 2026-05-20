# Multi-Role Support — Progress

## 2026-05-20 — Planning Complete

9 increments defined.

| # | Title | Status |
|---|-------|--------|
| 1 | Flyway migration: `user_roles` junction table | completed |
| 2 | `User` entity: multi-role `@ElementCollection` | pending |
| 3 | `TokenService`: multi-role authority building | pending |
| 4 | `UserManagementService` and DTOs: multi-role create/update | completed |
| 5 | `AdminUserController`: update endpoint to accept `roles` | pending |
| 6 | `PermissionService`: read all roles from JWT | pending |
| 7 | Frontend: user model and admin dialogs for multi-role | pending |
| 8 | `AuthService` and `AuthController` wiring + tests | pending |
| 9 | `UserManagementServiceTest` and integration validation | pending |

---

## 2026-05-20 — Increment 1: Flyway migration `user_roles` junction table

- **What was completed:** Created `V68__multi_role_junction.sql` which drops `chk_user_role`, creates `user_roles`, migrates existing role data (including legacy ADMIN/USER aliases), drops `users.role`, and creates `idx_user_roles_role`.
- **Interfaces/methods created:** none (pure DDL/DML migration)
- **Files created/modified:**
  - `backend/src/main/resources/db/migration/V68__multi_role_junction.sql` (created)
  - `context/PROGRESS.md` (updated)
  - `context/PLAN.md` (increment 1 status → completed)
- **Decisions made:**
  - The `CASE` expression in the INSERT handles legacy `ADMIN` and `USER` values before the CHECK constraint on `user_roles` rejects them, avoiding a FK/constraint violation on any DB that V5 was not applied to cleanly.
  - The WHERE clause uses an explicit allowlist of all six possible values (five canonical + two legacy) so rows with any other unexpected value are silently skipped rather than failing the migration; a DBA can investigate those rows separately.
  - The `role` column is dropped only after the data copy is complete, satisfying the acceptance criterion that every existing user has exactly one row in `user_roles` before the column disappears.
- **Tests:** N/A — migration file has no unit-testable code; correctness is validated by Flyway executing the script on a running DB.

---

## 2026-05-20 — Increment 2: `User` entity multi-role `@ElementCollection`

- **What was completed:** Replaced the single `@Enumerated @Column private UserRole role` field with an `@ElementCollection(fetch = FetchType.EAGER)` `Set<UserRole> roles` backed by the `user_roles` junction table. Added full role-collection API. Kept deprecated bridge methods so existing callers compile unchanged. Added 26-test unit suite for the entity.
- **Interfaces/methods created:**
  - `User(String email, String passwordHash, String fullName, Set<UserRole> roles, boolean active)` — primary constructor; canonicalises each role, rejects null/empty set
  - `User(String email, String passwordHash, Set<UserRole> roles, boolean active)` — delegates with null fullName
  - `User(String email, String passwordHash, UserRole role, boolean active)` — `@Deprecated`, delegates with `Set.of(role.canonical())`
  - `User(String email, String passwordHash, String fullName, UserRole role, boolean active)` — `@Deprecated`, delegates with `Set.of(role.canonical())`
  - `Set<UserRole> getRoles()` — returns the live set
  - `void setRoles(Set<UserRole> roles)` — replaces the set; throws `IllegalArgumentException("roles must not be empty")` if null or empty; refreshes `updatedAt`
  - `void addRole(UserRole role)` — adds to set, refreshes `updatedAt`
  - `void removeRole(UserRole role)` — removes from set; throws `IllegalStateException("user must have at least one role")` if removal would empty the set; refreshes `updatedAt`
  - `boolean hasRole(UserRole role)` — returns `roles.contains(role)`
  - `UserRole getRole()` — `@Deprecated` bridge; returns first element or null if set is empty
  - `void setRole(UserRole role)` — `@Deprecated` bridge; calls `setRoles(Set.of(role))`
- **Files created/modified:**
  - `backend/src/main/java/com/psyassistant/users/User.java` (modified)
  - `backend/src/test/java/com/psyassistant/users/UserTest.java` (created)
  - `context/PROGRESS.md` (updated)
  - `context/PLAN.md` (increment 2 status → completed)
- **Decisions made:**
  - Null/empty guard is applied directly in the primary constructor before `canonicalize` is invoked, avoiding a `NullPointerException` propagating from the helper.
  - `removeRole` updates `updatedAt` even when the removed role was not in the set (no-op removal still touches the timestamp). This is consistent with the other mutators.
  - The `canonicalize` helper is a private static method; it is only called from constructors so the public `setRoles` receives already-canonical or raw values from callers. Callers that need canonicalization must apply `UserRole.canonical()` themselves (enforced in Increment 4 service layer).
- **Tests:** 26 passing (UserTest); 490 total suite passing

---

## 2026-05-20 — Test Run (Increments 1 & 2, attempt 1)
- Passed: 490 | Failed: 0 | Skipped: 0 | Coverage: N/A (Jacoco not configured in pom.xml)
- Coverage gate: N/A — no Jacoco plugin present; gate cannot be evaluated
- Failures: none
- Action: all clear — suite passes cleanly after Flyway V68 migration and User entity @ElementCollection change

---

## 2026-05-20 — Code Review (Increments 1 and 2)
- Quality: FAIL (Critical: 0, High: 3, Medium: 2, Suggestion: 2)
- Coverage: GAPS FOUND (1 item)
- Recommendation: fix and re-review

---

## 2026-05-20 — Code Review Fixes (Increments 1 and 2)

- **What was completed:** Applied four code-review fixes to `User.java` and `V68__multi_role_junction.sql`, and updated `UserTest.java` accordingly.
- **Fixes applied:**
  1. `getRole()` — now throws `IllegalStateException("user has no roles")` instead of returning `null` when the roles set is empty.
  2. `addRole(UserRole)` — canonicalises the argument via `role.canonical()` before inserting into the set, preventing deprecated aliases (e.g. `ADMIN`) from being persisted to the `user_roles` table.
  3. `removeRole(UserRole)` — canonicalises the argument via `role.canonical()` for both the size-guard check and the actual removal, so passing a deprecated alias correctly targets the stored canonical value.
  4. `getRoles()` — now returns `Collections.unmodifiableSet(roles)` instead of the live mutable field; `java.util.Collections` import added.
  5. `V68` SQL — added a `DO $$ ... $$` pre-flight block immediately before the `INSERT` that aborts the migration with a descriptive `RAISE EXCEPTION` if any user row has an unrecognised (non-mappable) `role` value.
- **Files modified:**
  - `backend/src/main/java/com/psyassistant/users/User.java`
  - `backend/src/main/resources/db/migration/V68__multi_role_junction.sql`
  - `backend/src/test/java/com/psyassistant/users/UserTest.java`
  - `context/PROGRESS.md`
- **Test changes:**
  - `deprecatedGetRoleReturnsNullWhenRolesSetIsEmpty` renamed to `deprecatedGetRoleThrowsIllegalStateWhenRolesSetIsEmpty` and updated to assert `IllegalStateException`.
  - Added `getRolesReturnsUnmodifiableView` — verifies that mutating the returned set throws `UnsupportedOperationException`.
  - Added `addRoleCanonicalizesLegacyAdminAlias` — verifies `ADMIN` is stored as `SYSTEM_ADMINISTRATOR`.
  - Added `removeRoleCanonicalizesLegacyAdminAliasBeforeRemoving` — verifies passing `ADMIN` alias removes the canonical `SYSTEM_ADMINISTRATOR` entry.
  - Added `removeRoleCanonicalizesLegacyAdminAliasForLastRoleGuard` — verifies the last-role guard fires even when the legacy alias is passed.
- **Tests:** 494 passing / 0 failures

---

## 2026-05-20 — Increment 3: `TokenService` multi-role authority building

- **What was completed:** Updated `TokenService` to build JWT authorities and compute TTL/therapist-profile-ID from a collection of roles. Updated `AuthResult` record to carry `Collection<UserRole> roles` instead of `UserRole role`. Updated `AuthService` to use multi-role methods throughout. Updated `AuthControllerTest` to construct `AuthResult` with `Set.of(UserRole)`. Updated `TokenServiceTest` and `AuthServiceTest` with multi-role API and new test cases.
- **Interfaces/methods created:**
  - `TokenService.buildAuthorities(Collection<UserRole>)` — union of ROLE_X + permissions, deduplicated via LinkedHashSet
  - `TokenService.accessTtlFor(Collection<UserRole>)` — admin TTL if any role is SYSTEM_ADMINISTRATOR or ADMIN
  - `TokenService.refreshTtlFor(Collection<UserRole>)` — same admin logic for refresh TTL
  - `TokenService.accessTokenExpiresAt(Collection<UserRole>)` — uses new accessTtlFor(Collection)
  - `TokenService.refreshTtlFor(UserRole)` — kept as `@Deprecated`
  - `TokenService.accessTokenExpiresAt(UserRole)` — kept as `@Deprecated`
  - `AuthResult.roles(): Collection<UserRole>` — replaces old `AuthResult.role(): UserRole`
- **Files created/modified:**
  - `backend/src/main/java/com/psyassistant/auth/service/TokenService.java` (modified)
  - `backend/src/main/java/com/psyassistant/auth/service/AuthResult.java` (modified)
  - `backend/src/main/java/com/psyassistant/auth/service/AuthService.java` (modified)
  - `backend/src/test/java/com/psyassistant/auth/TokenServiceTest.java` (modified)
  - `backend/src/test/java/com/psyassistant/auth/AuthServiceTest.java` (modified)
  - `backend/src/test/java/com/psyassistant/auth/AuthControllerTest.java` (modified)
  - `context/PROGRESS.md` (updated)
  - `context/PLAN.md` (increment 3 status → completed)
- **Decisions made:**
  - `AuthController` did not reference `result.role()` directly (it uses `result.refreshTtl()` which is already a Duration); no changes needed to controller logic.
  - `buildAuthorities` changed from private to package-private to allow direct unit testing in `TokenServiceTest`.
  - `accessTtlFor(Collection)` made public to allow direct unit testing in `TokenServiceTest`.
  - Test method `authenticateUserWithTwoRoles_jwtContainsBothRoleValues` renamed to `authenticateUserWithTwoRolesReturnsAuthResultWithBothRoles` to satisfy the project's checkstyle `MethodName` rule (no underscores allowed).
  - Existing deprecated single-role overloads (`refreshTtlFor(UserRole)`, `accessTokenExpiresAt(UserRole)`) retained in `TokenService` for backward compatibility with any callers outside the increment scope.
- **Tests:** 497 passing / 0 failures

---

## 2026-05-20 — Test Run (Increment 3, attempt 1)
- Passed: 497 | Failed: 0 | Skipped: 0 | Coverage: N/A (no Jacoco plugin in pom.xml)
- Coverage gate: N/A — Jacoco not configured; gate cannot be evaluated
- Failures: none
- Action: all clear — full suite passes cleanly after TokenService/AuthResult/AuthService multi-role changes

---

## 2026-05-20 — Code Review (Increment 3)
- Quality: FAIL (Critical: 0, High: 1, Medium: 3, Suggestion: 2)
- Coverage: GAPS FOUND (1 item: `AuthServiceTest` missing `authenticateUserWithSysAdminRole_usesAdminTtl` test required by plan)
- Recommendation: fix and re-review

---

## 2026-05-20 — Code Review Fixes (Increment 3)

- **What was completed:** Applied three code-review fixes across `AuthServiceTest`, `TokenService`, and `AuthResult`.
- **Fixes applied:**
  1. **Fix 1 (High) — `AuthServiceTest`:** Added `authenticateUserWithSysAdminRoleUsesAdminTtl` test. Creates a user with `Set.of(UserRole.SYSTEM_ADMINISTRATOR)`, stubs `tokenService.refreshTtlFor` via `ArgumentCaptor` to return `Duration.ofHours(24)`, calls `authService.authenticate(...)`, and asserts `result.refreshTtl()` equals `Duration.ofHours(24)`, is less than `Duration.ofDays(15)`, and that the captured roles collection contains `SYSTEM_ADMINISTRATOR` only. Method name uses camelCase (no underscores) to satisfy the project checkstyle `MethodName` rule. `ArgumentCaptor` import placed in alphabetical order within `org.*` group to satisfy `ImportOrder` checkstyle rule.
  2. **Fix 2 (Medium) — `TokenService.buildAuthorities`:** Changed `"ROLE_" + role.name()` to `"ROLE_" + role.canonical().name()` so deprecated alias roles (`ADMIN`, `USER`) that reach this method are canonicalized to their scoped names (`SYSTEM_ADMINISTRATOR`, `THERAPIST`) before being written to the JWT claim.
  3. **Fix 3 (Medium) — `AuthResult`:** Changed `Collection<UserRole> roles` record component to `Set<UserRole> roles`. Updated Javadoc to reflect the never-null/never-empty contract. All existing callers pass `user.getRoles()` which already returns `Set<UserRole>` (via `Collections.unmodifiableSet`) — no changes needed to `AuthService` or `AuthController`. `AuthControllerTest` already constructs `AuthResult` with `Set.of(...)` — no changes needed.
- **Files created/modified:**
  - `backend/src/test/java/com/psyassistant/auth/AuthServiceTest.java` (modified — new test + import)
  - `backend/src/main/java/com/psyassistant/auth/service/TokenService.java` (modified — canonical role name)
  - `backend/src/main/java/com/psyassistant/auth/service/AuthResult.java` (modified — `Collection` → `Set`)
  - `context/PROGRESS.md` (updated)
- **Tests:** 498 passing / 0 failures

---

## 2026-05-20 — Increment 4: `UserManagementService` and DTOs multi-role create/update

- **What was completed:** Updated request/response DTOs to use `Set<UserRole>` for roles. Updated `UserManagementService` to canonicalize and persist multi-role sets. Updated `UserSpecification` to JOIN `user_roles` collection for role filtering. Updated all tests. Added fix to `AdminUserController` therapist check and `TherapistProfileService` to maintain compilation.
- **Interfaces/methods created:**
  - `CreateUserRequest.roles(): Set<UserRole>` — replaces `role(): UserRole`
  - `PatchUserRequest.roles(): Set<UserRole>` — replaces `role(): UserRole`; nullable (null = no change)
  - `UserSummaryDto.roles(): Set<UserRole>` — new; `role(): UserRole` kept as `@Deprecated` bridge returning first element
  - `UserCreationResponseDto.roles(): Set<UserRole>` — new; `role(): UserRole` kept as `@Deprecated` bridge
  - `UserSpecification.withFilters(UserRole, Boolean)` — updated to JOIN `roles` collection with `query.distinct(true)`
- **Files created/modified:**
  - `backend/src/main/java/com/psyassistant/users/dto/CreateUserRequest.java` (modified)
  - `backend/src/main/java/com/psyassistant/users/dto/PatchUserRequest.java` (modified)
  - `backend/src/main/java/com/psyassistant/users/dto/UserSummaryDto.java` (modified)
  - `backend/src/main/java/com/psyassistant/users/dto/UserCreationResponseDto.java` (modified)
  - `backend/src/main/java/com/psyassistant/users/UserManagementService.java` (modified)
  - `backend/src/main/java/com/psyassistant/users/UserSpecification.java` (modified)
  - `backend/src/main/java/com/psyassistant/admin/AdminUserController.java` (minimal fix: `request.role()` → `request.roles().contains(UserRole.THERAPIST)`)
  - `backend/src/main/java/com/psyassistant/therapists/service/TherapistProfileService.java` (updated two `new CreateUserRequest(...)` calls to `Set.of(UserRole.THERAPIST)`)
  - `backend/src/test/java/com/psyassistant/users/UserManagementServiceTest.java` (modified — all assertions updated, 3 new tests added)
  - `backend/src/test/java/com/psyassistant/admin/AdminUserControllerTest.java` (modified — all request/response updated, 1 new test added)
  - `context/PROGRESS.md` (updated)
  - `context/PLAN.md` (increment 4 status → completed)
- **Decisions made:**
  - `AdminUserController.createTherapistWithTemporaryPassword` references `request.role()` which no longer exists after removing the single-role accessor; the minimal fix (changing to `request.roles().contains(UserRole.THERAPIST)`) was applied here rather than deferring to Increment 5 — without it the project would not compile.
  - `TherapistProfileService` had two `new CreateUserRequest(email, name, UserRole.THERAPIST)` calls that required updating to `Set.of(UserRole.THERAPIST)` for compilation.
  - `UserSummaryDto` and `UserCreationResponseDto` both gained `roles` as the primary field (before the deprecated `role` in the record component order) to match JSON serialisation priority.
  - The `updateUser` method compares canonical new roles against current roles before auditing, to avoid a spurious audit entry when the roles are effectively unchanged.
  - `createUserWithEmptyRolesThrowsValidationError` test uses `jakarta.validation.Validator` directly (no Spring context) for fast unit-level validation testing.
- **Tests:** 503 passing / 0 failures

---

## 2026-05-20 — Code Review (Increment 4)
- Quality: FAIL (Critical: 0, High: 3, Medium: 4, Suggestion: 2)
- Coverage: GAPS FOUND (3 items)
- Recommendation: fix and re-review

---

## 2026-05-20 — Test Run (Increment 4, attempt 1)
- Passed: 503 | Failed: 0 | Skipped: 0 | Coverage: N/A (Jacoco not configured in pom.xml)
- Coverage gate: N/A — no Jacoco plugin present; gate cannot be evaluated
- Failures: none
- Action: all clear — full suite passes cleanly after UserManagementService/DTOs multi-role changes

---

## 2026-05-20 — Code Review Fixes (Increment 4)

- **What was completed:** Applied seven code-review fixes across DTOs, `UserSpecification`, `UserManagementService`, and test files.
- **Fixes applied:**
  1. **Fix 1 (High) — `PatchUserRequest.roles` element constraint:** Changed `Set<UserRole> roles` to `Set<@NotNull UserRole> roles`. Added `import jakarta.validation.constraints.NotNull;`. Prevents a `null` element in the set from causing `NullPointerException` when `UserRole::canonical` is called in `UserManagementService.updateUser`.
  2. **Fix 2 (High) — `UserSummaryDto.from` guard:** Added `userRoles.isEmpty() ? null : userRoles.iterator().next()` guard before calling `iterator().next()` for the deprecated `role` bridge field. Prevents `NoSuchElementException` if the entity's roles set is ever empty.
  3. **Fix 2 (High) — `UserCreationResponseDto.from` guard:** Same guard applied for the deprecated `role` bridge field.
  4. **Fix 3 (High) — `UserSpecification` EXISTS subquery:** Replaced `root.join("roles", JoinType.INNER)` + `query.distinct(true)` with a correlated EXISTS subquery (`Subquery<Integer>` / `sub.correlate(root)` / `sub.select(cb.literal(1))`). Removed `query.distinct(true)` entirely. Added `Root` and `Subquery` imports. Eliminates the risk of incorrect count queries under JPA providers that do not honour `distinct(true)` on count queries.
  5. **Fix 4 (Medium) — Duplicate section header in `UserManagementService`:** Removed the first of two consecutive `// ---- private helpers` section headers that appeared between `generateTemporaryPassword()` and `generateRawToken()`.
  6. **Fix 5 — Test: `PATCH` with empty `roles` returns 400:** Added `updateUserReturns400OnEmptyRoles` to `AdminUserControllerTest`. Sends `{"roles":[]}` to `PATCH /api/v1/admin/users/{id}` and asserts HTTP 400.
  7. **Fix 6 — Test: `POST` with `roles` field omitted returns 400:** Added `createUserReturns400WhenRolesFieldOmitted` to `AdminUserControllerTest`. Sends a JSON body with no `roles` key and asserts HTTP 400.
  8. **Fix 7 — Test: `PatchUserRequest` validation with empty roles:** Added `updateUserWithEmptyRolesThrowsValidationError` to `UserManagementServiceTest`. Validates `new PatchUserRequest(null, Set.of(), null)` directly via `jakarta.validation.Validator` and asserts exactly one `ConstraintViolation` on the `roles` property.
- **Files modified:**
  - `backend/src/main/java/com/psyassistant/users/dto/PatchUserRequest.java`
  - `backend/src/main/java/com/psyassistant/users/dto/UserSummaryDto.java`
  - `backend/src/main/java/com/psyassistant/users/dto/UserCreationResponseDto.java`
  - `backend/src/main/java/com/psyassistant/users/UserSpecification.java`
  - `backend/src/main/java/com/psyassistant/users/UserManagementService.java`
  - `backend/src/test/java/com/psyassistant/admin/AdminUserControllerTest.java`
  - `backend/src/test/java/com/psyassistant/users/UserManagementServiceTest.java`
  - `context/PROGRESS.md`
- **Tests:** 506 passing / 0 failures

---

## 2026-05-20 — Test Run (Increment 4 code-review fixes, attempt 2)
- Passed: 506 | Failed: 0 | Skipped: 0 | Coverage: N/A (no Jacoco plugin in pom.xml)
- Coverage gate: N/A — Jacoco not configured; gate cannot be evaluated
- Failures: none
- Action: all clear — full suite passes cleanly after all Increment 4 code-review fixes

---

## 2026-05-20 — Code Review (Increment 4, re-review after fixes)
- Quality: PASS (Critical: 0, High: 0, Medium: 2)
- Coverage: FULLY COVERED
- Recommendation: approve

---

## 2026-05-20 — Increment 5: `AdminUserController` OpenAPI annotation updates and therapist endpoint tests

- **What was completed:** Updated `@Operation` and `@ApiResponse` Swagger descriptions to replace "role" (singular) with "roles" (plural). Confirmed `!request.roles().contains(UserRole.THERAPIST)` `contains` semantics are correct (a request with `[THERAPIST, SUPERVISOR]` is accepted). Added two new tests to `AdminUserControllerTest` covering the therapist endpoint validation.
- **Interfaces/methods created:** none (no new public APIs; annotation text only)
- **Files created/modified:**
  - `backend/src/main/java/com/psyassistant/admin/AdminUserController.java` (modified — Javadoc and `@Operation`/`@ApiResponse` descriptions updated)
  - `backend/src/test/java/com/psyassistant/admin/AdminUserControllerTest.java` (modified — two new tests added)
  - `context/PROGRESS.md` (updated)
  - `context/PLAN.md` (increment 5 status → completed)
- **Decisions made:**
  - `contains` semantics confirmed as correct: the therapist endpoint creates accounts where THERAPIST is one of the roles, so `[THERAPIST, SUPERVISOR]` should be accepted.
  - The `@Operation` description for `createTherapistWithTemporaryPassword` was expanded to explicitly state that additional roles beyond THERAPIST are allowed.
  - The validation logic (`!request.roles().contains(UserRole.THERAPIST)`) was already applied in Increment 4 and required no further changes.
  - The `updateUser` `@Operation` description was updated from "role, full name, or active status" to "roles, full name, or active status".
- **Tests:** 508 passing / 0 failures

---

## 2026-05-20 — Test Run (Increment 5, attempt 1)
- Passed: 508 | Failed: 0 | Skipped: 0 | Coverage: N/A (no Jacoco plugin in pom.xml)
- Coverage gate: N/A — Jacoco not configured; gate cannot be evaluated
- Failures: none
- Action: all clear — full suite passes cleanly; increments 1-5 complete

---

## 2026-05-20 — Code Review (Increment 5)
- Quality: FAIL (Critical: 0, High: 1, Medium: 1)
- Coverage: GAPS FOUND (1 item: no test for THERAPIST-only roles returning 201 — the base-case acceptance criterion from PLAN.md)
- Recommendation: fix and re-review

---

## 2026-05-20 — Code Review Fixes (Increment 5)

- **What was completed:** Applied two code-review fixes — replaced the generic `IllegalArgumentException` with a typed domain exception and added the missing THERAPIST-only base-case test.
- **Fixes applied:**
  1. **Fix 1 (High) — Typed domain exception:** Created `RoleValidationException` in `com.psyassistant.users`, registered a `handleRoleValidation` handler in `GlobalExceptionHandler` (returns 400, code `ROLE_VALIDATION_ERROR`). Replaced `throw new IllegalArgumentException(...)` in `AdminUserController.createTherapistWithTemporaryPassword` with `throw new RoleValidationException(...)`. Added `import com.psyassistant.users.RoleValidationException` to `AdminUserController`. Removed the inline comment `// Validate that roles include THERAPIST` that restated the code.
  2. **Fix 2 (Coverage) — THERAPIST-only base case test:** Added `createTherapistReturns201WhenRolesContainOnlyTherapist` to `AdminUserControllerTest`. Sends `{"roles":["THERAPIST"]}` with all required fields to `POST /api/v1/admin/users/therapists` and asserts HTTP 201, `$.email`, and `$.roles[0]`.
- **Files created/modified:**
  - `backend/src/main/java/com/psyassistant/users/RoleValidationException.java` (created)
  - `backend/src/main/java/com/psyassistant/common/exception/GlobalExceptionHandler.java` (modified — import + new `handleRoleValidation` handler)
  - `backend/src/main/java/com/psyassistant/admin/AdminUserController.java` (modified — import + `IllegalArgumentException` → `RoleValidationException`, inline comment removed)
  - `backend/src/test/java/com/psyassistant/admin/AdminUserControllerTest.java` (modified — new test added)
  - `context/PROGRESS.md` (updated)
- **Decisions made:**
  - `RoleValidationException` was placed in `com.psyassistant.users` (not `com.psyassistant.admin`) following the established pattern of `SelfDeactivationException`, which is also a user-domain business-rule exception that maps to 400.
  - The `GlobalExceptionHandler` already had a fallback `IllegalArgumentException` handler that conditionally returned 400 or 404 based on message text. The new typed handler eliminates the need for that text inspection for this specific error path.
- **Tests:** 509 passing / 0 failures

---

## 2026-05-20 — Test Run (Increment 5 code-review fixes, attempt 2)
- Passed: 509 | Failed: 0 | Skipped: 0 | Coverage: N/A (no Jacoco plugin in pom.xml)
- Coverage gate: N/A — Jacoco not configured; gate cannot be evaluated
- Failures: none
- Action: all clear — full suite passes cleanly; increments 1-5 (including all code-review fixes) complete

## 2026-05-20 — Code Review (Increment 5, re-review after fixes)
- Quality: PASS (Critical: 0, High: 0, Medium: 1)
- Coverage: FULLY COVERED
- Recommendation: approve
