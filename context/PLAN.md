# Multi-Role Support (PA-XX)

## Overview

Allow a single internal CRM user to hold multiple roles simultaneously (e.g., THERAPIST + SUPERVISOR). The union of permissions for all assigned roles must be reflected in the JWT and enforced in the backend. The admin UI must support assigning/removing multiple roles when creating or editing a user.

## Architecture Notes

### Data model change — junction table replaces single column

The existing `users.role VARCHAR(50)` column is replaced by a `user_roles` junction table:

```
user_roles (user_id UUID FK, role VARCHAR(50), PRIMARY KEY (user_id, role))
```

The old `users.role` column is dropped only after the data migration copies every existing single-role row into the junction table. A `CHECK` constraint on `user_roles.role` replaces the one that was on `users.role`.

The `User` JPA entity gains an `@ElementCollection` `Set<UserRole> roles` field; the `getRole()` / `setRole()` single-role methods are kept as deprecated bridge methods (returning/setting the first role) so that callers outside the scope of this feature do not break.

### JWT — union of permissions

`TokenService.buildAuthorities(Collection<UserRole> roles)` replaces the single-role variant. The `roles` claim becomes the union set of `ROLE_X` values and all distinct permissions across every assigned role. `hasRole('THERAPIST')` works if `ROLE_THERAPIST` appears anywhere in the union; `hasAuthority('WRITE_SESSION_NOTE')` works if that permission comes from any role. No Spring Security configuration changes are required.

### TTL rule

If ANY assigned role is `SYSTEM_ADMINISTRATOR`, admin TTL applies (15 min access / 24 h refresh). Otherwise standard TTL applies.

### Therapist profile ID injection

`TokenService.buildAccessToken` checks whether `THERAPIST` is among the user's roles (not the only role) before injecting `therapistProfileId`. This is a one-line predicate change.

### Session cap rule

If ANY assigned role is `SYSTEM_ADMINISTRATOR`, the admin session cap (1) applies.

### Admin filter

`GET /api/v1/admin/users?role=THERAPIST` returns users that have THERAPIST among their roles. `UserSpecification` must JOIN against `user_roles` instead of matching `users.role`.

### `AuthResult` record

`AuthResult` carries a single `UserRole role` field used by the controller to compute the refresh cookie max-age. This must become `Collection<UserRole> roles`; the controller picks the shortest TTL (admin TTL if any is SYSTEM_ADMINISTRATOR, otherwise standard TTL). The `LoginResponse` is unchanged.

### Frontend

`PermissionService.roles` currently reads one `ROLE_X` entry. With multi-role, all `ROLE_X` entries are collected. `permissions.config.ts`'s `ROUTE_ROLES` and `PERMISSIONS` maps do set-membership checks that already iterate an array, so they remain correct. The user admin dialogs replace a single-select `<select>` with checkboxes so multiple roles can be chosen.

### Backward compatibility

Existing single-role tokens issued before this migration will decode correctly: the `roles` claim will still include the single `ROLE_X` value plus permissions, which the Spring Security converter already accepts. No token invalidation is needed.

### Deprecated legacy roles

`UserRole.ADMIN` and `UserRole.USER` canonical mappings are already handled by `canonical()`. The migration converts any stale rows in `users.role` before the column is dropped. No special handling is needed at the entity layer.

---

## Increments

---

### Increment 1 — Flyway migration: `user_roles` junction table

**Status:** completed

**Goal:** Add the `user_roles` junction table, copy existing role data from `users.role`, drop the old column and its CHECK constraint.

**Files to create:**
- `backend/src/main/resources/db/migration/V68__multi_role_junction.sql`

**Tasks:**
1. Drop constraint `chk_user_role` on `users`.
2. Create `user_roles (user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, role VARCHAR(50) NOT NULL CHECK (role IN ('RECEPTION_ADMIN_STAFF','THERAPIST','SUPERVISOR','FINANCE','SYSTEM_ADMINISTRATOR')), CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role))`.
3. Insert into `user_roles` by selecting from `users.role`: `INSERT INTO user_roles (user_id, role) SELECT id, role FROM users WHERE role IN (...)`.
4. Handle legacy aliases in the migration: also insert canonical mappings for any `ADMIN` or `USER` rows that may have been missed by V5.
5. Drop column `users.role`.
6. Create index `idx_user_roles_role ON user_roles(role)` to support the role-filter query.

**Acceptance criteria:**
- Migration runs clean on a fresh schema with all V2–V67 already applied.
- Every existing user row has exactly one row in `user_roles` with the canonical role value.
- `users` table no longer has a `role` column.
- `user_roles` rejects inserts with values outside the five valid roles.

**Dependencies:** none

---

### Increment 2 — `User` entity: multi-role `@ElementCollection`

**Status:** completed

**Goal:** Update the `User` JPA entity to use an `@ElementCollection` `Set<UserRole> roles` mapped to `user_roles`. Retain `getRole()` and `setRole()` as deprecated bridge methods so that all existing callers (auth service, token service, management service) continue to compile.

**Files to modify:**
- `backend/src/main/java/com/psyassistant/users/User.java`

**Tasks:**
1. Remove the `@Enumerated @Column private UserRole role` field.
2. Add:
   ```java
   @ElementCollection(fetch = FetchType.EAGER)
   @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
   @Column(name = "role", nullable = false, length = 50)
   @Enumerated(EnumType.STRING)
   private Set<UserRole> roles = new LinkedHashSet<>();
   ```
3. Add `getRoles(): Set<UserRole>` and `setRoles(Set<UserRole>)` methods.
4. Add `addRole(UserRole)`, `removeRole(UserRole)`, `hasRole(UserRole)` convenience methods.
5. Keep `getRole()` as a `@Deprecated` bridge that returns `roles.iterator().next()` (or throws if empty). Keep `setRole(UserRole)` as a `@Deprecated` bridge that calls `setRoles(Set.of(role))`.
6. Update both constructors to accept `Set<UserRole>` and set the `roles` collection; keep the existing `UserRole role` constructors as `@Deprecated` bridges that delegate.
7. Invariant: `roles` must never be empty — enforce in `setRoles` and `removeRole`.

**Expected interfaces:**
- `User.getRoles()`, `User.setRoles(Set<UserRole>)`, `User.addRole(UserRole)`, `User.removeRole(UserRole)`, `User.hasRole(UserRole)`
- `User.getRole()` (deprecated bridge), `User.setRole(UserRole)` (deprecated bridge)

**Acceptance criteria:**
- Existing `UserManagementServiceTest` and `TokenServiceTest` still compile and pass (they use `new User(..., UserRole.THERAPIST, true)` — this constructor bridge must remain).
- Application context starts (Hibernate must see `user_roles` table, which was created in Increment 1).
- A user entity loaded from the DB has `roles` populated.

**Dependencies:** Increment 1

---

### Increment 3 — `TokenService`: multi-role authority building

**Status:** completed

**Goal:** Change `TokenService` to build JWT authorities and compute TTL/therapist-profile-ID from a set of roles rather than a single role. All callers that pass `user.getRole()` must be updated to pass `user.getRoles()`.

**Files to modify:**
- `backend/src/main/java/com/psyassistant/auth/service/TokenService.java`
- `backend/src/main/java/com/psyassistant/auth/service/AuthResult.java`
- `backend/src/main/java/com/psyassistant/auth/service/AuthService.java`
- `backend/src/test/java/com/psyassistant/auth/TokenServiceTest.java`

**Tasks:**

`TokenService`:
1. Add `buildAuthorities(Collection<UserRole> roles)`: union of `ROLE_X` strings + union of permission names across all roles (deduplication via `LinkedHashSet`).
2. Change `buildAccessToken(User user, UUID therapistProfileId)` to call `buildAuthorities(user.getRoles())` and use `accessTtlFor(user.getRoles())`.
3. `accessTtlFor(Collection<UserRole> roles)`: admin TTL if any role is `SYSTEM_ADMINISTRATOR` or `ADMIN`; standard otherwise.
4. `refreshTtlFor(Collection<UserRole> roles)`: same logic for refresh TTL.
5. Keep `accessTokenExpiresAt(UserRole role)` signature but add a `accessTokenExpiresAt(Collection<UserRole> roles)` overload; deprecate single-role version.
6. The `therapistProfileId` injection check becomes: `roles.contains(UserRole.THERAPIST)`.

`AuthResult`:
1. Change field `UserRole role` to `Collection<UserRole> roles` (keeps record semantics, rename field).
2. Update the single constructor call-site in `AuthService`.

`AuthService`:
1. All three places that call `tokenService.refreshTtlFor(user.getRole())` change to `tokenService.refreshTtlFor(user.getRoles())`.
2. All three places that check `user.getRole() == UserRole.THERAPIST` for `therapistProfileId` change to `user.hasRole(UserRole.THERAPIST)`.
3. `enforceSessionCap` check changes from `user.getRole() == UserRole.SYSTEM_ADMINISTRATOR || user.getRole() == UserRole.ADMIN` to `user.getRoles().stream().anyMatch(r -> r == UserRole.SYSTEM_ADMINISTRATOR || r == UserRole.ADMIN)`.
4. Return `new AuthResult(response, rawToken, user.getRoles(), tokenService.refreshTtlFor(user.getRoles()))`.

`AuthController` (check — currently reads `result.role()` to compute cookie max-age):
1. Change to read `result.roles()` and pass to `tokenService.refreshTtlFor(result.roles())`.

`TokenServiceTest`:
1. Update to set `user.setRoles(Set.of(UserRole.SYSTEM_ADMINISTRATOR))` etc. instead of single-role variants.
2. Add test: `buildAccessTokenForUserWithTwoRolesContainsBothRoleClaimsAndUnionPermissions`.
3. Add test: `accessTtlUsesAdminTtlWhenOneRoleIsSysAdmin`.

**Expected interfaces:**
- `TokenService.buildAuthorities(Collection<UserRole>)`
- `TokenService.refreshTtlFor(Collection<UserRole>)` (and deprecated single-role overload)
- `AuthResult.roles(): Collection<UserRole>` (replacing `role()`)

**Acceptance criteria:**
- `TokenServiceTest` passes including new multi-role tests.
- JWT from a user with `[THERAPIST, SUPERVISOR]` contains `ROLE_THERAPIST`, `ROLE_SUPERVISOR`, and the union of their permissions — no duplicate entries.
- JWT from a user with `[THERAPIST, SYSTEM_ADMINISTRATOR]` uses 15-min access TTL.
- `AuthServiceTest` passes without modification (the service now calls multi-role methods).

**Dependencies:** Increment 2

---

### Increment 4 — `UserManagementService` and DTOs: multi-role create/update

**Status:** completed

**Goal:** Update the request/response DTOs and service so that creating and patching a user operates on a `Set<UserRole>` (minimum one element). The `UserSummaryDto` gains a `roles: Set<UserRole>` field alongside the deprecated single `role`.

**Files to modify:**
- `backend/src/main/java/com/psyassistant/users/dto/CreateUserRequest.java`
- `backend/src/main/java/com/psyassistant/users/dto/PatchUserRequest.java`
- `backend/src/main/java/com/psyassistant/users/dto/UserSummaryDto.java`
- `backend/src/main/java/com/psyassistant/users/dto/UserCreationResponseDto.java`
- `backend/src/main/java/com/psyassistant/users/UserManagementService.java`
- `backend/src/main/java/com/psyassistant/users/UserSpecification.java`
- `backend/src/test/java/com/psyassistant/users/UserManagementServiceTest.java`
- `backend/src/test/java/com/psyassistant/admin/AdminUserControllerTest.java`

**Tasks:**

`CreateUserRequest`:
1. Replace `@NotNull UserRole role` with `@NotNull @Size(min=1, message="at least one role required") Set<@NotNull UserRole> roles`.

`PatchUserRequest`:
1. Replace `UserRole role` with `Set<@Size(min=1) UserRole> roles` (optional, null means no change).

`UserSummaryDto`:
1. Add `Set<UserRole> roles` field.
2. Keep `UserRole role` as a deprecated bridge (returns `roles.iterator().next()` for backward compat in serialised JSON — the first role alphabetically/insertion-order).
3. Update `from(User)` factory method to populate `roles` from `user.getRoles()`.

`UserCreationResponseDto`:
1. Add `Set<UserRole> roles` (mirror the same pattern as `UserSummaryDto`).

`UserManagementService`:
1. `createUser` / `createUserWithTemporaryPassword`: call `request.roles().stream().map(UserRole::canonical).collect(...)`, pass the set to `new User(...)`.
2. `updateUser` with `PatchUserRequest`: if `request.roles() != null && !request.roles().isEmpty()`, compute canonical set, call `user.setRoles(canonicalSet)`, log old and new roles.
3. `listUsers`: the `UserSpecification.withFilters(role, active)` must be updated for multi-role (see below).
4. Audit log detail strings: use `roles=` instead of `role=`.

`UserSpecification`:
1. `withFilters(UserRole role, Boolean active)`: change role filter from `cb.equal(root.get("role"), role)` to a sub-query or JOIN on the `user_roles` collection: `root.join("roles", JoinType.INNER)` and then `cb.equal(join, role)`. Use `DISTINCT` or `countDistinct` to avoid duplicate rows.

`UserManagementServiceTest`:
1. Update all `new CreateUserRequest(email, name, UserRole.THERAPIST)` to `new CreateUserRequest(email, name, Set.of(UserRole.THERAPIST))`.
2. Add test: creating user with empty roles throws validation error.
3. Add test: `updateUser` with multiple roles persists all of them.
4. Add test: `listUsers` with role filter returns user that has that role plus another.

`AdminUserControllerTest`:
1. Update request JSON bodies in tests to use `"roles": ["THERAPIST"]` instead of `"role": "THERAPIST"`.

**Expected interfaces:**
- `CreateUserRequest.roles(): Set<UserRole>`
- `PatchUserRequest.roles(): Set<UserRole>` (nullable)
- `UserSummaryDto.roles(): Set<UserRole>` + deprecated `role()`

**Acceptance criteria:**
- `mvn test` passes.
- `POST /api/v1/admin/users` with `{"roles": ["THERAPIST","SUPERVISOR"]}` creates a user with both roles.
- `POST /api/v1/admin/users` with `{"roles": []}` returns 400.
- `GET /api/v1/admin/users?role=THERAPIST` returns a user who has roles `[THERAPIST, SUPERVISOR]`.
- `UserSummaryDto` JSON contains both `"roles": ["THERAPIST","SUPERVISOR"]` and the deprecated `"role": "THERAPIST"` (first entry).

**Dependencies:** Increment 2

---

### Increment 5 — `AdminUserController`: update endpoint to accept `roles`

**Status:** completed

**Goal:** Align the controller with the updated DTOs. The existing `/api/v1/admin/users/therapists` endpoint is checked to confirm it still works — it creates a user with a single role and now passes `Set.of(UserRole.THERAPIST)` internally.

**Files to modify:**
- `backend/src/main/java/com/psyassistant/admin/AdminUserController.java`

**Tasks:**
1. The `/api/v1/admin/users/therapists` endpoint currently validates `request.role() != UserRole.THERAPIST`. Change to validate `!request.roles().equals(Set.of(UserRole.THERAPIST))` (or `!request.roles().contains(UserRole.THERAPIST)`).
2. Review all `@Operation` and `@ApiResponse` Swagger annotations — update descriptions where they mention "single role".
3. No new endpoints are needed — the existing `POST /api/v1/admin/users` and `PATCH /api/v1/admin/users/{id}` already accept the updated DTOs from Increment 4.

**Acceptance criteria:**
- `POST /api/v1/admin/users/therapists` still returns 201 for a THERAPIST-only role request.
- `POST /api/v1/admin/users/therapists` with `{"roles": ["FINANCE"]}` returns 400 (not THERAPIST).
- All `AdminUserControllerTest` tests pass.

**Dependencies:** Increments 3, 4

---

### Increment 6 — `PermissionService`: read all roles from JWT

**Status:** completed

**Goal:** Update the Angular `PermissionService` to collect all `ROLE_X` entries from the JWT `roles` claim instead of only the first one. Update `JwtClaims` model as needed. All route guards and permission checks continue to work as before.

**Files to modify:**
- `frontend/src/app/core/auth/permission.service.ts`
- `frontend/src/app/core/auth/jwt-claims.model.ts` (comment update only, no structural change)

**Tasks:**
1. In `PermissionService.roles` signal: replace `claims.roles?.find(r => r.startsWith('ROLE_'))` (returns first match) with `claims.roles?.filter(r => r.startsWith('ROLE_')).map(r => r.replace('ROLE_', ''))` (returns all).
2. `hasAnyRole(required: AppRole[])` is already a `.some(r => required.includes(r))` call — no change needed.
3. `hasPermission(key: PermissionKey)` delegates to `hasAnyRole` — no change needed.
4. Update the comment in `jwt-claims.model.ts` to say "Backend emits multiple ROLE_X entries for multi-role users".

**Acceptance criteria:**
- A user with roles `[THERAPIST, SUPERVISOR]` in the JWT has `PermissionService.roles()` returning `['THERAPIST', 'SUPERVISOR']`.
- Route guard for `/reports` (allowed for SUPERVISOR) grants access when the user has `[THERAPIST, SUPERVISOR]`.
- Route guard for `/admin` (SYSTEM_ADMINISTRATOR only) still denies access for `[THERAPIST, SUPERVISOR]`.

**Dependencies:** Increment 3 (backend emits multiple `ROLE_X` values)

---

### Increment 7 — Frontend: user model and admin dialogs for multi-role

**Status:** pending

**Goal:** Update the Angular user model interfaces and the create/edit user dialogs to support multiple roles.

**Files to modify:**
- `frontend/src/app/features/admin/users/models/user.model.ts`
- `frontend/src/app/features/admin/users/components/create-user-dialog/create-user-dialog.component.ts`
- `frontend/src/app/features/admin/users/components/edit-user-dialog/edit-user-dialog.component.ts`
- `frontend/src/app/features/admin/users/components/user-list/user-list.component.ts`
- `frontend/src/app/features/admin/therapists/components/create-therapist-dialog/create-therapist-dialog.component.ts`
- `frontend/src/app/features/admin/therapists/components/edit-therapist-dialog/edit-therapist-dialog.component.ts`
- `frontend/src/app/features/admin/therapists/components/therapist-list/therapist-list.component.ts`
- `frontend/src/assets/i18n/en.json`
- `frontend/src/assets/i18n/uk.json`

**Tasks:**

`user.model.ts`:
1. Add `roles: UserRole[]` to `UserSummary` (alongside the deprecated `role: UserRole` for backward compat).
2. Change `CreateUserPayload.role: UserRole` to `roles: UserRole[]`.
3. Change `PatchUserPayload.role?: UserRole` to `roles?: UserRole[]`.
4. Add `normalizeRoles(roles: string[]): UserRole[]` helper.

`CreateUserDialogComponent`:
1. Replace the `<select id="role">` single-select with a checkbox group — one checkbox per `ASSIGNABLE_ROLES` entry.
2. Form control changes from `role: ['', Validators.required]` to `roles: [[], rolesValidator]` where `rolesValidator` checks `value.length >= 1`.
3. The THERAPIST-redirect logic: if `formValue.roles.includes('THERAPIST')` (still redirect to therapist wizard, unchanged behaviour).
4. Payload sent: `{ email, fullName, roles: formValue.roles }`.

`EditUserDialogComponent`:
1. Replace single-role `<select>` with checkbox group.
2. Pre-populate from `user.roles` (or fall back to `[normalizeRole(user.role)]` for backward compat).
3. Validate: at least one role must remain checked before save.
4. Payload sent: `{ roles: selectedRoles }`.

`user-list.component.ts`:
1. Display all roles in the table cell instead of one — comma-separated role labels or multiple chips.

Therapist-related components: these use `UserSummary` and display `user.role`. Update to handle `user.roles` (show all, or show THERAPIST as primary with additional roles noted).

`en.json` / `uk.json`: add keys for the multi-role select label and validation message if needed.

**Acceptance criteria:**
- Admin can create a user with `[THERAPIST, SUPERVISOR]` by checking two checkboxes.
- At least one checkbox must be selected (validation error shown otherwise).
- Editing a user shows existing roles as pre-checked and allows adding/removing roles (cannot remove the last one).
- The user list table shows all roles for multi-role users.
- `ng build --configuration production` passes with zero TypeScript errors.

**Dependencies:** Increment 4 (backend multi-role DTOs deployed), Increment 6 (frontend permission service updated)

---

### Increment 8 — `AuthService` and `AuthController` wiring + tests

**Status:** pending

**Goal:** Verify the full authentication flow (login, refresh, first-login password change) works end-to-end with multi-role users. Fix the `AuthController` cookie max-age computation to use `result.roles()`. Add integration tests covering multi-role login.

**Files to modify:**
- `backend/src/main/java/com/psyassistant/auth/AuthController.java`
- `backend/src/test/java/com/psyassistant/auth/AuthServiceTest.java`
- `backend/src/test/java/com/psyassistant/auth/AuthControllerTest.java`

**Tasks:**

`AuthController`:
1. Find all uses of `result.role()` (single-role `AuthResult` field) — change to `result.roles()`.
2. The cookie max-age line typically looks like `tokenService.refreshTtlFor(result.role()).toSeconds()` — change to `tokenService.refreshTtlFor(result.roles()).toSeconds()`.

`AuthServiceTest`:
1. Update `setUp()` to use `user.setRoles(Set.of(UserRole.THERAPIST))` instead of `user.setRole(...)` (wherever `User` entities are constructed with a role).
2. Add test: `authenticateUserWithTwoRoles_jwtContainsBothRoleValues`.
3. Add test: `authenticateUserWithSysAdminRole_usesAdminTtl`.

`AuthControllerTest`:
1. Update mock stubs that return `AuthResult` to use `new AuthResult(response, rawToken, Set.of(UserRole.THERAPIST), ttl)`.

**Acceptance criteria:**
- `AuthServiceTest` passes.
- `AuthControllerTest` passes.
- Login for a user with `[THERAPIST, SUPERVISOR]` produces a token containing `ROLE_THERAPIST`, `ROLE_SUPERVISOR`, and the union of their permissions.
- Refresh token cookie max-age is correct for both single and multi-role users.

**Dependencies:** Increment 3

---

### Increment 9 — `UserManagementServiceTest` and integration validation

**Status:** pending

**Goal:** Ensure `UserManagementServiceTest` and `AdminUserControllerTest` are fully updated, and that the `RbacIntegrationTest` passes with the new multi-role infrastructure. Add a new integration test proving that a user with `[THERAPIST, SUPERVISOR]` can access both `/api/v1/sessions` and supervisor-only reports.

**Files to modify:**
- `backend/src/test/java/com/psyassistant/users/UserManagementServiceTest.java`
- `backend/src/test/java/com/psyassistant/common/rbac/RbacIntegrationTest.java`

**Tasks:**

`UserManagementServiceTest`:
1. Verify all test helper calls to `makeUser(...)` use the new multi-role constructors.
2. Add test: `createUser_withMultipleRoles_persistsAllRoles`.
3. Add test: `updateUser_withRolesSet_replacesRoles`.
4. Add test: `updateUser_withEmptyRolesSet_throwsValidationError`.
5. Add test: `listUsers_roleFilter_matchesUsersWithRoleAmongMany`.

`RbacIntegrationTest`:
1. Add test `AC8`: a JWT with `[ROLE_THERAPIST, ROLE_SUPERVISOR]` authorities can `GET /api/v1/clients/{id}/sessions` (therapist access) — HTTP 200.
2. Add test `AC9`: same JWT can access `GET /api/v1/reporting/team-workload` (supervisor-only permission `READ_TEAM_WORKLOAD`) — HTTP 200.
3. Add test `AC10`: same JWT cannot `POST /api/v1/admin/users` (no SYSTEM_ADMINISTRATOR) — HTTP 403.

**Acceptance criteria:**
- `mvn test` passes, all existing tests unbroken.
- AC8, AC9, AC10 pass.
- `UserManagementServiceTest` new multi-role tests pass.

**Dependencies:** Increments 4, 5, 8

---

## Risks

1. **Hibernate `@ElementCollection` lazy vs. eager loading**: `FetchType.EAGER` is required so that `user.getRoles()` is available when `TokenService.buildAuthorities()` is called outside a transaction. If set to LAZY, a `LazyInitializationException` will occur. This is the correct choice given the small set size (max 5 roles per user).

2. **`DISTINCT` in `UserSpecification`**: joining `user_roles` in a JPA `Specification` can produce duplicate `User` rows. A `query.distinct(true)` call or a subquery approach must be used to avoid the count/total-pages in pagination being wrong.

3. **`UserSummaryDto` JSON backward compatibility**: existing API consumers (frontend, tests) expect `"role": "THERAPIST"` in the response. Adding `"roles": [...]` is additive and non-breaking. The deprecated `role` field must remain serialised for the duration of the migration.

4. **`AuthResult` record rename**: `result.role()` → `result.roles()` appears in `AuthController`; any other callers that reference the record accessor by name will not compile. A grep for `result.role()` and `AuthResult` before writing is mandatory.

5. **`AdminUserController /therapists` endpoint role check**: the current `request.role() != UserRole.THERAPIST` check must become `!request.roles().contains(UserRole.THERAPIST)` — failing to update this will silently accept non-therapist multi-role combinations or reject valid ones.

6. **Frontend therapist wizard redirect logic**: `CreateUserDialogComponent` currently redirects to the therapist wizard when `formValue.role === 'THERAPIST'`. With multi-role checkboxes, the trigger should be `formValue.roles.includes('THERAPIST')`. If this is missed, creating a THERAPIST+SUPERVISOR user will skip the wizard.

---

## Success Criteria

- A user can be assigned `[THERAPIST, SUPERVISOR]`; their JWT contains `ROLE_THERAPIST`, `ROLE_SUPERVISOR`, plus the union of THERAPIST and SUPERVISOR permissions.
- `hasRole('THERAPIST')` and `hasAuthority('READ_TEAM_WORKLOAD')` both pass for the same token.
- The admin UI checkboxes allow selecting zero to N roles; validation prevents saving with zero roles.
- `GET /api/v1/admin/users?role=THERAPIST` returns multi-role users that include THERAPIST.
- All existing backend tests pass (`mvn test`).
- Angular production build exits 0 with zero TypeScript errors.
- No existing single-role tokens are invalidated by the migration.
