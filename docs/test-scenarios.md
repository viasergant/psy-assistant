# PSY-ASSISTANT — Comprehensive Test Scenarios Reference

## Summary Table

| Area | Scenario Count | IDs |
|---|---|---|
| Authentication | 16 | AUTH-001 – AUTH-016 |
| User Management (Admin) | 14 | USR-001 – USR-014 |
| User Preferences | 5 | PREF-001 – PREF-005 |
| Lead Management | 22 | LEAD-001 – LEAD-022 |
| Client Management | 20 | CLT-001 – CLT-020 |
| Therapist Onboarding | 16 | THXP-001 – THXP-016 |
| Schedule Management | 18 | SCH-001 – SCH-018 |
| Availability Query | 8 | AVAIL-001 – AVAIL-008 |
| Leave Management | 20 | LVE-001 – LVE-020 |
| Appointment Booking | 24 | APPT-001 – APPT-024 |
| Session Records | 14 | SESS-001 – SESS-014 |
| Care Plans | 14 | CP-001 – CP-014 |
| Progress Tracking & Outcome Measures | 16 | PT-001 – PT-016 |
| Cross-Entity / Business Rules | 12 | XE-001 – XE-012 |
| **Total** | **219** | |

---

## Conventions

- **Actor identifiers**: `ADMIN` = SYSTEM_ADMINISTRATOR, `STAFF` = RECEPTION_ADMIN_STAFF, `THERAPIST`, `SUPERVISOR`, `FINANCE`, `ANON` = unauthenticated.
- **HTTP status** listed as the expected value.
- Preconditions assume a clean test database unless stated otherwise.
- "Version field" in update requests refers to the optimistic-lock `version` long field.

---

## 1. Authentication

### AUTH-001 — Successful login returns access token in body and refresh token in HttpOnly cookie

| Field | Value |
|---|---|
| Preconditions | Active user account exists with email `admin@example.com` |
| Actor | ANON |
| Action | `POST /api/v1/auth/login` `{"email":"admin@example.com","password":"correct-password"}` |
| Expected result | Body contains `accessToken` (non-empty JWT string). `Set-Cookie` header contains `refreshToken` with `HttpOnly;Secure;SameSite=Strict;Path=/api/v1/auth` attributes. |
| HTTP status | 200 |

### AUTH-002 — Login with wrong password returns 401

| Field | Value |
|---|---|
| Preconditions | User account exists |
| Actor | ANON |
| Action | `POST /api/v1/auth/login` with correct email, wrong password |
| Expected result | Error body; no access token in response; no refresh cookie set |
| HTTP status | 401 |

### AUTH-003 — Login with non-existent email returns 401 (not 404, to prevent user enumeration)

| Field | Value |
|---|---|
| Preconditions | Email does not exist |
| Actor | ANON |
| Action | `POST /api/v1/auth/login` `{"email":"ghost@example.com","password":"anything"}` |
| Expected result | Generic 401 error; no user enumeration information in response body |
| HTTP status | 401 |

### AUTH-004 — Login with deactivated account returns 401

| Field | Value |
|---|---|
| Preconditions | User account exists with `active=false` |
| Actor | ANON |
| Action | `POST /api/v1/auth/login` with valid credentials for that deactivated account |
| Expected result | 401; error message must not reveal whether account exists vs is disabled (or it may — verify policy) |
| HTTP status | 401 |

### AUTH-005 — Login with missing email field returns 400

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `POST /api/v1/auth/login` `{"password":"pw"}` |
| Expected result | Validation error listing missing `email` field |
| HTTP status | 400 |

### AUTH-006 — Login with missing password field returns 400

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `POST /api/v1/auth/login` `{"email":"a@b.com"}` |
| HTTP status | 400 |

### AUTH-007 — Login with malformed email format returns 400

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `POST /api/v1/auth/login` `{"email":"not-an-email","password":"pw"}` |
| Expected result | Validation error referencing email format |
| HTTP status | 400 |

### AUTH-008 — Token refresh with valid HttpOnly cookie returns new tokens

| Field | Value |
|---|---|
| Preconditions | User is logged in; refresh token cookie is live |
| Actor | ANON (with valid cookie) |
| Action | `POST /api/v1/auth/refresh` with `Cookie: refreshToken=<valid>` |
| Expected result | New `accessToken` in body; new `refreshToken` cookie set; old refresh token is invalidated (rotation) |
| HTTP status | 200 |

### AUTH-009 — Token refresh without cookie returns 401

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `POST /api/v1/auth/refresh` with no cookie |
| HTTP status | 401 |

### AUTH-010 — Token refresh with expired refresh token returns 401

| Field | Value |
|---|---|
| Preconditions | Refresh token TTL has expired |
| Actor | ANON |
| Action | `POST /api/v1/auth/refresh` with expired cookie value |
| HTTP status | 401 |

### AUTH-011 — Logout clears cookie and returns 204

| Field | Value |
|---|---|
| Preconditions | Valid session; refresh cookie present |
| Actor | Authenticated user |
| Action | `POST /api/v1/auth/logout` with `Cookie: refreshToken=<valid>` |
| Expected result | Response contains `Set-Cookie: refreshToken=; Max-Age=0` (cleared); subsequent refresh with same token returns 401 |
| HTTP status | 204 |

### AUTH-012 — Logout is idempotent (no refresh cookie present still returns 204)

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `POST /api/v1/auth/logout` with no cookie |
| HTTP status | 204 |

### AUTH-013 — First-login password change succeeds with valid current password and new password ≥10 chars

| Field | Value |
|---|---|
| Preconditions | User with `mustChangePassword=true` exists; caller has valid access token |
| Actor | That user |
| Action | `POST /api/v1/auth/first-login-password-change` Bearer token + `{"currentPassword":"temp","newPassword":"NewSecure123!"}` |
| Expected result | New access token and refresh cookie; `mustChangePassword` flag cleared |
| HTTP status | 200 |

### AUTH-014 — First-login password change rejected if new password is < 10 chars

| Field | Value |
|---|---|
| Preconditions | User with `mustChangePassword=true`, valid access token |
| Actor | That user |
| Action | `newPassword` = `"short"` (5 chars) |
| HTTP status | 400 |

### AUTH-015 — First-login password change rejected if currentPassword is wrong

| Field | Value |
|---|---|
| Preconditions | User with `mustChangePassword=true`, valid access token |
| Actor | That user |
| Action | `currentPassword` = wrong value |
| HTTP status | 401 |

### AUTH-016 — First-login password change requires Bearer token (unauthenticated call returns 401)

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `POST /api/v1/auth/first-login-password-change` without Authorization header |
| HTTP status | 401 |

---

## 2. User Management (Admin)

### USR-001 — ADMIN can create user with valid email, fullName, and role

| Field | Value |
|---|---|
| Preconditions | Email does not exist |
| Actor | ADMIN |
| Action | `POST /api/v1/admin/users` `{"email":"new@example.com","fullName":"Jane Doe","role":"THERAPIST"}` |
| Expected result | Response contains user `id`, `email`, `fullName`, `role`, `active=true`, `mustChangePassword=true`, and a `temporaryPassword` of ≥10 chars |
| HTTP status | 201 |

### USR-002 — Create user with duplicate email returns 409

| Field | Value |
|---|---|
| Preconditions | User with that email already exists |
| Actor | ADMIN |
| Action | `POST /api/v1/admin/users` with existing email |
| HTTP status | 409 |

### USR-003 — Create user with invalid role enum returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/admin/users` `{"role":"SUPER_USER"}` |
| HTTP status | 400 |

### USR-004 — Create user with missing required fields returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/admin/users` `{"email":"x@x.com"}` (no fullName, no role) |
| HTTP status | 400 |

### USR-005 — Non-ADMIN cannot create users (returns 403)

| Field | Value |
|---|---|
| Actor | THERAPIST / STAFF |
| Action | `POST /api/v1/admin/users` with valid payload |
| HTTP status | 403 |

### USR-006 — `POST /api/v1/admin/users/therapists` rejects non-THERAPIST role

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `{"email":"x@x.com","fullName":"X","role":"FINANCE"}` |
| Expected result | Error indicating this endpoint is only for THERAPIST role |
| HTTP status | 400 |

### USR-007 — List users returns paginated result with correct metadata

| Field | Value |
|---|---|
| Preconditions | At least 5 users exist |
| Actor | ADMIN |
| Action | `GET /api/v1/admin/users?page=0&size=3` |
| Expected result | Response has `content` array of length ≤3, `totalElements`, `totalPages`, `page=0`, `size=3` |
| HTTP status | 200 |

### USR-008 — List users with role filter returns only matching users

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `GET /api/v1/admin/users?role=THERAPIST` |
| Expected result | All returned users have `role=THERAPIST` |
| HTTP status | 200 |

### USR-009 — List users with active=false filter returns only inactive users

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `GET /api/v1/admin/users?active=false` |
| Expected result | All returned users have `active=false` |
| HTTP status | 200 |

### USR-010 — ADMIN can update another user's role

| Field | Value |
|---|---|
| Preconditions | Target user exists with `role=THERAPIST` |
| Actor | ADMIN |
| Action | `PATCH /api/v1/admin/users/{id}` `{"role":"SUPERVISOR"}` |
| Expected result | Response shows `role=SUPERVISOR`; other fields unchanged |
| HTTP status | 200 |

### USR-011 — ADMIN cannot self-deactivate

| Field | Value |
|---|---|
| Preconditions | Caller is the target user |
| Actor | ADMIN |
| Action | `PATCH /api/v1/admin/users/{ownId}` `{"active":false}` |
| Expected result | Business-rule error preventing self-deactivation |
| HTTP status | 400 |

### USR-012 — Update non-existent user returns 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `PATCH /api/v1/admin/users/00000000-0000-0000-0000-000000000099` `{"active":true}` |
| HTTP status | 404 |

### USR-013 — Admin password reset returns 204 and subsequent login requires new password

| Field | Value |
|---|---|
| Preconditions | Target user exists |
| Actor | ADMIN |
| Action | `POST /api/v1/admin/users/{id}/password-reset` |
| Expected result | 204; target user's `mustChangePassword` is now true |
| HTTP status | 204 |

### USR-014 — Password reset for non-existent user returns 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/admin/users/00000000-0000-0000-0000-000000000099/password-reset` |
| HTTP status | 404 |

---

## 3. User Preferences

### PREF-001 — Authenticated user can set language to "uk"

| Field | Value |
|---|---|
| Actor | Any authenticated role |
| Action | `PATCH /api/v1/users/me/language` `{"language":"uk"}` |
| HTTP status | 204 |

### PREF-002 — Authenticated user can set language to "en"

| Field | Value |
|---|---|
| Actor | Any authenticated role |
| Action | `PATCH /api/v1/users/me/language` `{"language":"en"}` |
| HTTP status | 204 |

### PREF-003 — Unsupported language code returns 400

| Field | Value |
|---|---|
| Actor | Any authenticated role |
| Action | `PATCH /api/v1/users/me/language` `{"language":"de"}` |
| HTTP status | 400 |

### PREF-004 — Empty language string returns 400

| Field | Value |
|---|---|
| Actor | Any authenticated role |
| Action | `PATCH /api/v1/users/me/language` `{"language":""}` |
| HTTP status | 400 |

### PREF-005 — Unauthenticated language update returns 401

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `PATCH /api/v1/users/me/language` `{"language":"en"}` |
| HTTP status | 401 |

---

## 4. Lead Management

### LEAD-001 — Create lead with minimum required fields (fullName + one contact method)

| Field | Value |
|---|---|
| Actor | STAFF or ADMIN (requires `MANAGE_LEADS`) |
| Action | `POST /api/v1/leads` `{"fullName":"John Doe","contactMethods":[{"type":"EMAIL","value":"j@e.com","isPrimary":true}]}` |
| Expected result | Lead created with `status=NEW`; returned DTO contains `id`, `fullName`, `status`, `contactMethods` |
| HTTP status | 201 |

### LEAD-002 — Create lead with PHONE contact method type

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `POST /api/v1/leads` with `contactMethods=[{"type":"PHONE","value":"+380991234567","isPrimary":true}]` |
| Expected result | Lead created successfully |
| HTTP status | 201 |

### LEAD-003 — Create lead with both EMAIL and PHONE contact methods

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | Two contact methods, only one with `isPrimary=true` |
| Expected result | Both methods stored; only the correct one flagged primary |
| HTTP status | 201 |

### LEAD-004 — Create lead with no contact methods returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `POST /api/v1/leads` `{"fullName":"X","contactMethods":[]}` |
| HTTP status | 400 |

### LEAD-005 — Create lead with invalid contact method type returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `contactMethods=[{"type":"TWITTER","value":"@x","isPrimary":true}]` |
| HTTP status | 400 |

### LEAD-006 — Create lead with missing fullName returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `POST /api/v1/leads` with only `contactMethods` and no `fullName` |
| HTTP status | 400 |

### LEAD-007 — User without MANAGE_LEADS cannot create a lead

| Field | Value |
|---|---|
| Actor | THERAPIST (read-only `READ_LEADS` at most) |
| Action | `POST /api/v1/leads` valid payload |
| HTTP status | 403 |

### LEAD-008 — FSM transition NEW → CONTACTED succeeds

| Field | Value |
|---|---|
| Preconditions | Lead in `NEW` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/status` `{"status":"CONTACTED"}` |
| Expected result | `status=CONTACTED` in response |
| HTTP status | 200 |

### LEAD-009 — FSM transition NEW → QUALIFIED is rejected (invalid FSM step)

| Field | Value |
|---|---|
| Preconditions | Lead in `NEW` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/status` `{"status":"QUALIFIED"}` |
| HTTP status | 422 |

### LEAD-010 — FSM transition CONTACTED → QUALIFIED succeeds

| Field | Value |
|---|---|
| Preconditions | Lead in `CONTACTED` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/status` `{"status":"QUALIFIED"}` |
| HTTP status | 200 |

### LEAD-011 — FSM transition from terminal state CONVERTED is rejected

| Field | Value |
|---|---|
| Preconditions | Lead in `CONVERTED` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/status` `{"status":"CONTACTED"}` |
| HTTP status | 422 |

### LEAD-012 — FSM transition from terminal state INACTIVE is rejected

| Field | Value |
|---|---|
| Preconditions | Lead in `INACTIVE` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/status` `{"status":"NEW"}` |
| HTTP status | 422 |

### LEAD-013 — Archive lead from NEW status transitions to INACTIVE

| Field | Value |
|---|---|
| Preconditions | Lead in `NEW` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/archive` |
| Expected result | `status=INACTIVE`; lead marked as archived |
| HTTP status | 200 |

### LEAD-014 — Archive already INACTIVE lead returns 422

| Field | Value |
|---|---|
| Preconditions | Lead in `INACTIVE` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/archive` |
| HTTP status | 422 |

### LEAD-015 — Archive already CONVERTED lead returns 422

| Field | Value |
|---|---|
| Preconditions | Lead in `CONVERTED` status |
| Actor | STAFF |
| Action | `PATCH /api/v1/leads/{id}/archive` |
| HTTP status | 422 |

### LEAD-016 — Convert QUALIFIED lead creates client and transitions lead to CONVERTED

| Field | Value |
|---|---|
| Preconditions | Lead in `QUALIFIED` status |
| Actor | STAFF |
| Action | `POST /api/v1/leads/{id}/convert` `{"fullName":"John Doe","contactMethods":[...]}` |
| Expected result | Response contains `clientId`; lead status becomes `CONVERTED`; `GET /api/v1/clients/{clientId}` returns client |
| HTTP status | 201 |

### LEAD-017 — Convert already CONVERTED lead returns 409 with existing clientId

| Field | Value |
|---|---|
| Preconditions | Lead already converted |
| Actor | STAFF |
| Action | `POST /api/v1/leads/{id}/convert` |
| Expected result | Response body contains existing `clientId` (ConversionErrorResponse) |
| HTTP status | 409 |

### LEAD-018 — Convert non-QUALIFIED lead returns 422

| Field | Value |
|---|---|
| Preconditions | Lead in `CONTACTED` status |
| Actor | STAFF |
| Action | `POST /api/v1/leads/{id}/convert` |
| HTTP status | 422 |

### LEAD-019 — List leads default excludes INACTIVE leads (includeArchived=false)

| Field | Value |
|---|---|
| Preconditions | At least one ACTIVE lead and one INACTIVE lead exist |
| Actor | STAFF |
| Action | `GET /api/v1/leads` (no includeArchived param) |
| Expected result | INACTIVE lead is absent from response |
| HTTP status | 200 |

### LEAD-020 — List leads with includeArchived=true includes INACTIVE leads

| Field | Value |
|---|---|
| Preconditions | INACTIVE lead exists |
| Actor | STAFF |
| Action | `GET /api/v1/leads?includeArchived=true` |
| Expected result | INACTIVE lead is included |
| HTTP status | 200 |

### LEAD-021 — Full update (PUT) replaces all contact methods atomically

| Field | Value |
|---|---|
| Preconditions | Lead exists with two contact methods |
| Actor | STAFF |
| Action | `PUT /api/v1/leads/{id}` with body containing only one contact method |
| Expected result | Lead now has exactly the one contact method from the PUT body; previous methods removed |
| HTTP status | 200 |

### LEAD-022 — Get non-existent lead returns 404

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/v1/leads/00000000-0000-0000-0000-000000000099` |
| HTTP status | 404 |

---

## 5. Client Management

### CLT-001 — List all clients returns alphabetically ordered result

| Field | Value |
|---|---|
| Preconditions | Multiple clients with varied names exist |
| Actor | STAFF (requires `MANAGE_CLIENTS` or `READ_CLIENTS_ALL`) |
| Action | `GET /api/v1/clients` |
| Expected result | Array ordered by display name A–Z |
| HTTP status | 200 |

### CLT-002 — Get client by ID returns full detail including contact methods and tags

| Field | Value |
|---|---|
| Preconditions | Client exists |
| Actor | STAFF |
| Action | `GET /api/v1/clients/{id}` |
| Expected result | Full `ClientDetailDto` including `id`, `fullName`, `version`, `contactMethods`, `tags` |
| HTTP status | 200 |

### CLT-003 — Get non-existent client returns 404

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/v1/clients/00000000-0000-0000-0000-000000000099` |
| HTTP status | 404 |

### CLT-004 — Update client with correct version succeeds

| Field | Value |
|---|---|
| Preconditions | Client exists with `version=0` |
| Actor | STAFF (requires `MANAGE_CLIENTS`) |
| Action | `PUT /api/v1/clients/{id}` body includes all required fields + `"version":0` |
| Expected result | Updated fields reflected; response `version=1` |
| HTTP status | 200 |

### CLT-005 — Update client with stale version triggers optimistic lock conflict

| Field | Value |
|---|---|
| Preconditions | Client has been updated since version 0 (now version=1) |
| Actor | STAFF |
| Action | `PUT /api/v1/clients/{id}` with `"version":0` |
| Expected result | Conflict error indicating optimistic lock failure |
| HTTP status | 409 |

### CLT-006 — Update client with invalid (missing) fullName returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `PUT /api/v1/clients/{id}` with `"fullName":""` |
| HTTP status | 400 |

### CLT-007 — Only user with MANAGE_CLIENTS can update client

| Field | Value |
|---|---|
| Actor | SUPERVISOR (READ_CLIENTS_ALL, no MANAGE_CLIENTS) |
| Action | `PUT /api/v1/clients/{id}` valid payload |
| HTTP status | 403 |

### CLT-008 — Update client tags with valid list replaces previous tags atomically

| Field | Value |
|---|---|
| Preconditions | Client has tags `["A","B"]` at version 0 |
| Actor | STAFF |
| Action | `PATCH /api/v1/clients/{id}/tags` `{"tags":["C","D"],"version":0}` |
| Expected result | Tags are now `["C","D"]`; version incremented |
| HTTP status | 200 |

### CLT-009 — Update tags to empty list clears all tags

| Field | Value |
|---|---|
| Preconditions | Client has existing tags |
| Actor | STAFF |
| Action | `PATCH /api/v1/clients/{id}/tags` `{"tags":[],"version":N}` |
| Expected result | Tags array is empty |
| HTTP status | 200 |

### CLT-010 — Update tags exceeding maximum count (21 tags) returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `PATCH /api/v1/clients/{id}/tags` with 21 tag strings |
| HTTP status | 400 |

### CLT-011 — Update tags with individual tag > 64 chars returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | Include a tag of 65 characters |
| HTTP status | 400 |

### CLT-012 — Update tags with stale version returns 409

| Field | Value |
|---|---|
| Preconditions | Client tags updated since version 0 |
| Actor | STAFF |
| Action | `PATCH /api/v1/clients/{id}/tags` with stale version |
| HTTP status | 409 |

### CLT-013 — Upload profile photo (multipart) succeeds for valid image

| Field | Value |
|---|---|
| Preconditions | Client exists at version N |
| Actor | STAFF |
| Action | `POST /api/v1/clients/{id}/photo` multipart: `version=N`, `file=<JPEG image bytes>` |
| Expected result | Updated `ClientDetailDto` with photo reference; version incremented |
| HTTP status | 200 |

### CLT-014 — Photo upload with stale version returns 409

| Field | Value |
|---|---|
| Preconditions | Client version changed since fetch |
| Actor | STAFF |
| Action | `POST /api/v1/clients/{id}/photo` with stale `version` |
| HTTP status | 409 |

### CLT-015 — Get client photo returns image bytes with correct Content-Type header

| Field | Value |
|---|---|
| Preconditions | Client has a photo uploaded |
| Actor | STAFF |
| Action | `GET /api/v1/clients/{id}/photo` |
| Expected result | Binary response; `Content-Type: image/jpeg` (or actual type); `Cache-Control: no-store` |
| HTTP status | 200 |

### CLT-016 — Get photo for client with no photo returns 404

| Field | Value |
|---|---|
| Preconditions | Client has no photo |
| Actor | STAFF |
| Action | `GET /api/v1/clients/{id}/photo` |
| HTTP status | 404 |

### CLT-017 — Search clients by partial name returns matching results ordered by relevance

| Field | Value |
|---|---|
| Preconditions | Clients named "Anna Smith" and "Anna Jones" exist |
| Actor | STAFF |
| Action | `GET /api/v1/clients/search?q=Anna` |
| Expected result | Both clients returned; limit defaults applied |
| HTTP status | 200 |

### CLT-018 — Search with limit param caps result count

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/v1/clients/search?q=a&limit=3` |
| Expected result | At most 3 results |
| HTTP status | 200 |

### CLT-019 — Client timeline returns chronologically ordered events

| Field | Value |
|---|---|
| Preconditions | Client has at least one appointment and one profile change event |
| Actor | STAFF |
| Action | `GET /api/v1/clients/{id}/timeline` |
| Expected result | Events in descending chronological order; each event has `eventType`, `timestamp`, `description` |
| HTTP status | 200 |

### CLT-020 — User with only READ_ASSIGNED_CLIENTS cannot update client profile

| Field | Value |
|---|---|
| Actor | THERAPIST role with `READ_ASSIGNED_CLIENTS` but not `MANAGE_CLIENTS` |
| Action | `PUT /api/v1/clients/{id}` valid payload |
| HTTP status | 403 |

---

## 6. Therapist Onboarding

### THXP-001 — Create therapist with account atomically (user + profile + temp password)

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/with-account` `{"email":"t@t.com","fullName":"Dr. X","employmentStatus":"ACTIVE","primarySpecializationId":"<valid-uuid>"}` |
| Expected result | Response contains `therapistProfile` (with `id`, `email`, `fullName`, `employmentStatus=ACTIVE`) and `userDetails` (with `email`, `temporaryPassword` ≥10 chars); corresponding user account exists |
| HTTP status | 201 |

### THXP-002 — Create therapist atomicity: if user creation fails, no profile is persisted

| Field | Value |
|---|---|
| Preconditions | Email already exists as a user |
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/with-account` with duplicate email |
| Expected result | 409; no orphaned therapist profile created |
| HTTP status | 409 |

### THXP-003 — Create therapist without primarySpecializationId returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/with-account` missing `primarySpecializationId` |
| HTTP status | 400 |

### THXP-004 — Create therapist with non-existent specialization UUID returns 400 or 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/with-account` with `primarySpecializationId` that does not exist |
| HTTP status | 400 or 404 |

### THXP-005 — Create therapist profile only (no account) via POST /api/v1/therapists

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists` with valid profile payload (no email/account data) |
| Expected result | Therapist profile created; no user account |
| HTTP status | 201 |

### THXP-006 — Get therapist by ID returns full profile with version for optimistic locking

| Field | Value |
|---|---|
| Actor | ADMIN or THERAPIST (own profile) |
| Action | `GET /api/v1/therapists/{id}` |
| Expected result | Response includes `version` field for subsequent update |
| HTTP status | 200 |

### THXP-007 — THERAPIST role can only access own profile (not another therapist's)

| Field | Value |
|---|---|
| Preconditions | Two therapist accounts exist; caller is therapist A |
| Actor | THERAPIST A |
| Action | `GET /api/v1/therapists/{id-of-therapist-B}` |
| Expected result | 403 Forbidden |
| HTTP status | 403 |

### THXP-008 — Get therapist by email returns profile matching that email

| Field | Value |
|---|---|
| Preconditions | Therapist with specific email exists |
| Actor | ADMIN |
| Action | `GET /api/v1/therapists/by-email/{email}` |
| Expected result | Profile with matching email |
| HTTP status | 200 |

### THXP-009 — Partial update (PATCH) with correct version succeeds

| Field | Value |
|---|---|
| Preconditions | Therapist at version 0 |
| Actor | ADMIN |
| Action | `PATCH /api/v1/therapists/{id}` `{"fullName":"Dr. Updated","version":0}` |
| Expected result | `fullName` updated; `version=1` |
| HTTP status | 200 |

### THXP-010 — Partial update with stale version returns 409

| Field | Value |
|---|---|
| Preconditions | Profile updated since version 0 |
| Actor | ADMIN |
| Action | `PATCH /api/v1/therapists/{id}` with `"version":0` (stale) |
| HTTP status | 409 |

### THXP-011 — Deactivate therapist with reason records reason

| Field | Value |
|---|---|
| Preconditions | Active therapist exists |
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/{id}/deactivate` `{"reason":"Left organization"}` |
| Expected result | `employmentStatus=INACTIVE`; reason stored |
| HTTP status | 200 |

### THXP-012 — Deactivate therapist without reason is accepted (reason is optional)

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/{id}/deactivate` `{}` |
| HTTP status | 200 |

### THXP-013 — List therapists returns paginated results with correct structure

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `GET /api/v1/therapists?page=0&size=5` |
| Expected result | Response has `content`, `totalElements`, `totalPages`, `page=0`, `size=5` |
| HTTP status | 200 |

### THXP-014 — Non-ADMIN (THERAPIST role) cannot list all therapists

| Field | Value |
|---|---|
| Actor | THERAPIST |
| Action | `GET /api/v1/therapists` |
| HTTP status | 403 |

### THXP-015 — Get non-existent therapist returns 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `GET /api/v1/therapists/00000000-0000-0000-0000-000000000099` |
| HTTP status | 404 |

### THXP-016 — Temporary password returned on creation is unique and meets minimum length

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | Create two therapists; compare temporary passwords |
| Expected result | Passwords differ; each is ≥10 chars |
| HTTP status | 201 per creation |

---

## 7. Schedule Management

### SCH-001 — Create recurring schedule for valid dayOfWeek (1–7) and time range

| Field | Value |
|---|---|
| Preconditions | Active therapist exists |
| Actor | ADMIN or STAFF |
| Action | `POST /api/v1/therapists/{id}/schedule/recurring` `{"dayOfWeek":1,"startTime":"09:00:00","endTime":"17:00:00","timezone":"Europe/Kiev"}` |
| Expected result | Schedule created with the supplied values; `id` returned |
| HTTP status | 201 |

### SCH-002 — Create recurring schedule with endTime before startTime returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `dayOfWeek=1`, `startTime=17:00:00`, `endTime=09:00:00` |
| HTTP status | 400 |

### SCH-003 — Create recurring schedule with startTime equal to endTime returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `startTime=09:00:00`, `endTime=09:00:00` |
| HTTP status | 400 |

### SCH-004 — Create recurring schedule with invalid dayOfWeek (0 or 8) returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `dayOfWeek=0` or `dayOfWeek=8` |
| HTTP status | 400 |

### SCH-005 — Create two recurring schedules for same day of week (check whether duplicate is allowed or rejected)

| Field | Value |
|---|---|
| Preconditions | Schedule for Monday already exists for therapist |
| Actor | ADMIN |
| Action | Create a second recurring schedule for Monday |
| Expected result | Verify behavior: either 409 if duplicates are not allowed, or 201 if multiple windows per day are supported |
| HTTP status | 201 or 409 (document actual behavior) |

### SCH-006 — Update recurring schedule (PUT) changes time range

| Field | Value |
|---|---|
| Preconditions | Recurring schedule exists |
| Actor | ADMIN |
| Action | `PUT /api/v1/therapists/{id}/schedule/recurring/{scheduleId}` with new `startTime`, `endTime` |
| Expected result | Updated values returned |
| HTTP status | 200 |

### SCH-007 — Update non-existent schedule returns 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `PUT /api/v1/therapists/{id}/schedule/recurring/00000000-0000-0000-0000-000000000099` |
| HTTP status | 404 |

### SCH-008 — Delete recurring schedule succeeds and schedule is absent in subsequent GET

| Field | Value |
|---|---|
| Preconditions | Recurring schedule exists |
| Actor | ADMIN |
| Action | `DELETE /api/v1/therapists/{id}/schedule/recurring/{scheduleId}` |
| Expected result | 204; subsequent `GET /api/v1/therapists/{id}/schedule` does not include that schedule |
| HTTP status | 204 |

### SCH-009 — Create unavailability override for a specific date

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/{id}/schedule/overrides` `{"date":"2026-05-01","isAvailable":false,"timezone":"Europe/Kiev"}` |
| Expected result | Override created; `isAvailable=false`; no startTime/endTime required |
| HTTP status | 201 |

### SCH-010 — Create availability override for a specific date requires startTime and endTime

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/{id}/schedule/overrides` `{"date":"2026-05-02","isAvailable":true,"timezone":"Europe/Kiev"}` (no times) |
| Expected result | Validation error: startTime and endTime required when isAvailable=true |
| HTTP status | 400 |

### SCH-011 — Create availability override with valid times succeeds

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `{"date":"2026-05-02","isAvailable":true,"startTime":"10:00:00","endTime":"14:00:00","timezone":"Europe/Kiev"}` |
| HTTP status | 201 |

### SCH-012 — Create duplicate override for same date (second call) — verify replace or conflict behavior

| Field | Value |
|---|---|
| Preconditions | Override for date 2026-05-01 exists |
| Actor | ADMIN |
| Action | Create another override for 2026-05-01 |
| Expected result | Either 409 if duplicates disallowed, or replaces previous override |
| HTTP status | 409 or 200/201 (document actual) |

### SCH-013 — Update schedule override changes isAvailable from false to true with times

| Field | Value |
|---|---|
| Preconditions | Override exists as `isAvailable=false` |
| Actor | ADMIN |
| Action | `PUT /api/v1/therapists/{id}/schedule/overrides/{overrideId}` with `isAvailable=true, startTime, endTime` |
| HTTP status | 200 |

### SCH-014 — Delete schedule override succeeds

| Field | Value |
|---|---|
| Preconditions | Override exists |
| Actor | ADMIN |
| Action | `DELETE /api/v1/therapists/{id}/schedule/overrides/{overrideId}` |
| HTTP status | 204 |

### SCH-015 — Get schedule summary with date range returns recurring, overrides, and leave arrays

| Field | Value |
|---|---|
| Preconditions | At least one of each: recurring schedule, override, approved leave |
| Actor | ADMIN |
| Action | `GET /api/v1/therapists/{id}/schedule?startDate=2026-05-01&endDate=2026-05-31` |
| Expected result | Response contains `recurring`, `overrides`, `leave` arrays; each is present |
| HTTP status | 200 |

### SCH-016 — Get schedule summary with no date range returns all entries

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `GET /api/v1/therapists/{id}/schedule` (no params) |
| HTTP status | 200 |

### SCH-017 — Schedule operations for non-existent therapist return 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/00000000-0000-0000-0000-000000000099/schedule/recurring` valid payload |
| HTTP status | 404 |

### SCH-018 — Unauthenticated schedule access returns 401

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `GET /api/v1/therapists/{id}/schedule` |
| HTTP status | 401 |

---

## 8. Availability Query

### AVAIL-001 — Availability returns 30-minute slots inside working hours

| Field | Value |
|---|---|
| Preconditions | Therapist has recurring schedule Monday 09:00–11:00 |
| Actor | ADMIN or STAFF |
| Action | `GET /api/v1/therapists/{id}/availability?startDate=<next-monday>&endDate=<next-monday>` |
| Expected result | Slots at 09:00, 09:30, 10:00, 10:30 (4 slots); none outside 09:00–11:00 |
| HTTP status | 200 |

### AVAIL-002 — Availability returns empty array for day with no recurring schedule

| Field | Value |
|---|---|
| Preconditions | Therapist has no schedule for Sunday |
| Actor | STAFF |
| Action | `GET /api/v1/therapists/{id}/availability` for a Sunday date range |
| Expected result | Empty array |
| HTTP status | 200 |

### AVAIL-003 — Unavailability override removes slots for that day

| Field | Value |
|---|---|
| Preconditions | Therapist has Monday schedule AND an unavailability override for next Monday |
| Actor | STAFF |
| Action | `GET /api/v1/therapists/{id}/availability?startDate=<that-monday>&endDate=<that-monday>` |
| Expected result | Empty array (override removes all slots) |
| HTTP status | 200 |

### AVAIL-004 — Approved leave removes all slots within leave period

| Field | Value |
|---|---|
| Preconditions | Therapist has recurring schedule; approved leave spans 3 days |
| Actor | STAFF |
| Action | `GET /api/v1/therapists/{id}/availability` for the leave period |
| Expected result | No slots returned for leave days |
| HTTP status | 200 |

### AVAIL-005 — Availability for date range spanning multiple days aggregates correctly

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `startDate` = Monday, `endDate` = Friday (therapist has Mon–Fri schedule) |
| Expected result | Slots for all 5 days; total slot count is consistent with schedule window |
| HTTP status | 200 |

### AVAIL-006 — Availability with `me` path resolves to caller's therapist profile

| Field | Value |
|---|---|
| Preconditions | Caller is a THERAPIST with a therapistProfileId in JWT |
| Actor | THERAPIST |
| Action | `GET /api/v1/therapists/me/availability?startDate=...&endDate=...` |
| Expected result | Returns own availability (same as querying by explicit UUID) |
| HTTP status | 200 |

### AVAIL-007 — Availability with missing startDate or endDate returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/v1/therapists/{id}/availability?startDate=2026-05-01` (no endDate) |
| HTTP status | 400 |

### AVAIL-008 — Availability for non-existent therapist returns 404

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/v1/therapists/00000000-0000-0000-0000-000000000099/availability?startDate=...&endDate=...` |
| HTTP status | 404 |

---

## 9. Leave Management

### LVE-001 — ADMIN submits leave request for therapist with valid ANNUAL type

| Field | Value |
|---|---|
| Preconditions | Therapist exists |
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/{id}/leave` `{"startDate":"2026-07-01","endDate":"2026-07-05","leaveType":"ANNUAL","requestNotes":"Summer vacation"}` |
| Expected result | Leave created with `status=PENDING`; all fields preserved |
| HTTP status | 201 |

### LVE-002 — Submit leave with each valid leaveType enum value (ANNUAL, SICK, PUBLIC_HOLIDAY, OTHER)

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | Four separate requests, one for each `leaveType` |
| Expected result | Each succeeds with `status=PENDING` |
| HTTP status | 201 each |

### LVE-003 — Submit leave with invalid leaveType enum returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `"leaveType":"MATERNITY"` |
| HTTP status | 400 |

### LVE-004 — Submit leave with endDate before startDate returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `{"startDate":"2026-07-05","endDate":"2026-07-01","leaveType":"SICK"}` |
| HTTP status | 400 |

### LVE-005 — Submit leave with same startDate and endDate (single day) succeeds

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `startDate=endDate=2026-07-15` |
| HTTP status | 201 |

### LVE-006 — Submit leave with missing leaveType returns 400

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | No `leaveType` field |
| HTTP status | 400 |

### LVE-007 — Admin approves pending leave request via admin endpoint

| Field | Value |
|---|---|
| Preconditions | Leave in `PENDING` status |
| Actor | ADMIN |
| Action | `PUT /api/v1/admin/leave/{leaveId}/approve` with `{"reviewerUserId":"<uuid>","adminNotes":"OK"}` |
| Expected result | `status=APPROVED`; `adminNotes` preserved; `reviewedAt` populated |
| HTTP status | 200 |

### LVE-008 — Admin rejects pending leave request via admin endpoint

| Field | Value |
|---|---|
| Preconditions | Leave in `PENDING` status |
| Actor | ADMIN |
| Action | `PUT /api/v1/admin/leave/{leaveId}/reject` with rejection notes |
| Expected result | `status=REJECTED` |
| HTTP status | 200 |

### LVE-009 — Cannot approve already approved leave (idempotency guard)

| Field | Value |
|---|---|
| Preconditions | Leave in `APPROVED` status |
| Actor | ADMIN |
| Action | `PUT /api/v1/admin/leave/{leaveId}/approve` |
| Expected result | Error: invalid state transition or idempotent 200 |
| HTTP status | 400 or 200 (document actual) |

### LVE-010 — Cannot approve already rejected leave

| Field | Value |
|---|---|
| Preconditions | Leave in `REJECTED` status |
| Actor | ADMIN |
| Action | Approve |
| HTTP status | 400 |

### LVE-011 — Cancel pending leave succeeds

| Field | Value |
|---|---|
| Preconditions | Leave in `PENDING` |
| Actor | ADMIN or THERAPIST (own leave) |
| Action | `PUT /api/v1/therapists/{id}/leave/{leaveId}/cancel` |
| Expected result | `status=CANCELLED` |
| HTTP status | 200 |

### LVE-012 — Cancel approved leave succeeds or is rejected (clarify business rule)

| Field | Value |
|---|---|
| Preconditions | Leave in `APPROVED` |
| Actor | ADMIN |
| Action | Cancel |
| Expected result | Verify: either `status=CANCELLED` or 400 if cancellation of approved leave is blocked |
| HTTP status | 200 or 400 (document actual behavior) |

### LVE-013 — Cannot cancel already cancelled leave

| Field | Value |
|---|---|
| Preconditions | Leave in `CANCELLED` |
| Actor | ADMIN |
| Action | Cancel again |
| HTTP status | 400 |

### LVE-014 — `GET /api/v1/admin/leave/pending` returns only PENDING leaves across all therapists

| Field | Value |
|---|---|
| Preconditions | Mix of PENDING and APPROVED leaves in the system |
| Actor | ADMIN |
| Action | `GET /api/v1/admin/leave/pending` |
| Expected result | All returned items have `status=PENDING`; approved leaves absent |
| HTTP status | 200 |

### LVE-015 — `GET /api/v1/therapists/{id}/leave` with status filter returns only matching leaves

| Field | Value |
|---|---|
| Preconditions | Therapist has PENDING and APPROVED leaves |
| Actor | ADMIN |
| Action | `GET /api/v1/therapists/{id}/leave?status=APPROVED` |
| Expected result | Only APPROVED leaves returned |
| HTTP status | 200 |

### LVE-016 — `GET /api/v1/therapists/{id}/leave/pending` ignores therapist ID (returns all pending)

| Field | Value |
|---|---|
| Preconditions | Two therapists each have a pending leave |
| Actor | ADMIN |
| Action | `GET /api/v1/therapists/{id-of-therapist-A}/leave/pending` |
| Expected result | Both leaves returned (endpoint ignores therapist ID path param) |
| HTTP status | 200 |

### LVE-017 — FINANCE role cannot access leave management endpoints

| Field | Value |
|---|---|
| Actor | FINANCE |
| Action | `GET /api/v1/admin/leave/pending` |
| HTTP status | 403 |

### LVE-018 — Approve non-existent leave returns 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `PUT /api/v1/admin/leave/00000000-0000-0000-0000-000000000099/approve` |
| HTTP status | 404 |

### LVE-019 — Check leave conflicts returns existing appointment conflicts within range

| Field | Value |
|---|---|
| Preconditions | Therapist has appointment on 2026-07-03 |
| Actor | STAFF |
| Action | `GET /api/v1/therapists/{id}/leave/conflicts?startDate=2026-07-01&endDate=2026-07-05` |
| Expected result | Response includes the conflicting appointment |
| HTTP status | 200 |

### LVE-020 — Submit leave for non-existent therapist returns 404

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/00000000-0000-0000-0000-000000000099/leave` valid payload |
| HTTP status | 404 |

---

## 10. Appointment Booking

### APPT-001 — Create appointment with all required fields and no conflicts succeeds

| Field | Value |
|---|---|
| Preconditions | Therapist has availability; client exists; session type is active |
| Actor | STAFF |
| Action | `POST /api/v1/appointments` `{"therapistProfileId":"...","clientId":"...","sessionTypeId":"...","startTime":"<ISO8601>","durationMinutes":60,"timezone":"Europe/Kiev","allowConflictOverride":false}` |
| Expected result | Appointment created with `status=SCHEDULED`; all IDs in response |
| HTTP status | 201 |

### APPT-002 — Create appointment with durationMinutes not a multiple of 15 returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `durationMinutes=50` |
| HTTP status | 400 |

### APPT-003 — Create appointment with durationMinutes below 15 returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `durationMinutes=10` |
| HTTP status | 400 |

### APPT-004 — Create appointment with durationMinutes above 480 returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `durationMinutes=495` |
| HTTP status | 400 |

### APPT-005 — Create appointment with durationMinutes at boundary values (15 and 480) succeeds

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | Two requests: `durationMinutes=15` and `durationMinutes=480` |
| HTTP status | 201 each |

### APPT-006 — Create appointment with conflicting time slot returns 409 when allowConflictOverride=false

| Field | Value |
|---|---|
| Preconditions | Appointment exists at time T for therapist X |
| Actor | STAFF |
| Action | Create second appointment at overlapping time for same therapist; `allowConflictOverride=false` |
| Expected result | Response contains conflict details (conflicting appointment IDs) |
| HTTP status | 409 |

### APPT-007 — Create appointment with conflicting time slot succeeds when allowConflictOverride=true

| Field | Value |
|---|---|
| Preconditions | Conflicting appointment exists |
| Actor | STAFF (or role with override permission) |
| Action | Same as above but `allowConflictOverride=true` |
| Expected result | Appointment created despite conflict; `conflictOverride=true` flag in response |
| HTTP status | 201 |

### APPT-008 — Pre-flight conflict check (POST /check-conflicts) returns hasConflicts=true for overlap

| Field | Value |
|---|---|
| Preconditions | Appointment exists at T |
| Actor | STAFF |
| Action | `POST /api/v1/appointments/check-conflicts` `{"therapistProfileId":"...","startTime":"<same-T>","durationMinutes":60}` |
| Expected result | `hasConflicts=true`; `conflicts` array non-empty |
| HTTP status | 200 |

### APPT-009 — Pre-flight conflict check returns hasConflicts=false for free slot

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | Check-conflicts for time slot with no existing appointments |
| Expected result | `hasConflicts=false`; `conflicts=[]` |
| HTTP status | 200 |

### APPT-010 — Get appointment by ID returns full detail

| Field | Value |
|---|---|
| Preconditions | Appointment exists |
| Actor | STAFF |
| Action | `GET /api/v1/appointments/{id}` |
| Expected result | Response contains all appointment fields including therapist, client, session type IDs, status |
| HTTP status | 200 |

### APPT-011 — Get non-existent appointment returns 404

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/v1/appointments/00000000-0000-0000-0000-000000000099` |
| HTTP status | 404 |

### APPT-012 — Get appointments by therapist and date range returns matching appointments ordered by startTime

| Field | Value |
|---|---|
| Preconditions | Therapist has 3 appointments across a week |
| Actor | STAFF |
| Action | `GET /api/v1/appointments/therapist/{id}?startDate=2026-06-01&endDate=2026-06-07` |
| Expected result | Only appointments within range; ordered ascending by startTime |
| HTTP status | 200 |

### APPT-013 — Cancelled appointments excluded from therapist date-range query

| Field | Value |
|---|---|
| Preconditions | One SCHEDULED and one CANCELLED appointment in the same date range |
| Actor | STAFF |
| Action | `GET /api/v1/appointments/therapist/{id}?startDate=...&endDate=...` |
| Expected result | Only SCHEDULED appointment returned |
| HTTP status | 200 |

### APPT-014 — Reschedule appointment to new conflict-free time succeeds

| Field | Value |
|---|---|
| Preconditions | Appointment is SCHEDULED |
| Actor | STAFF |
| Action | `PUT /api/v1/appointments/{id}/reschedule` `{"newStartTime":"<future-time>","reason":"Client request to move time slot"}` (reason ≥10 chars) |
| Expected result | Updated `startTime`; original time preserved in history; `rescheduledAt` populated |
| HTTP status | 200 |

### APPT-015 — Reschedule with reason shorter than 10 chars returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `"reason":"Too short"` (9 chars) |
| HTTP status | 400 |

### APPT-016 — Reschedule already-cancelled appointment returns 400

| Field | Value |
|---|---|
| Preconditions | Appointment is CANCELLED |
| Actor | STAFF |
| Action | Attempt reschedule |
| HTTP status | 400 |

### APPT-017 — Cancel appointment with CLIENT_INITIATED type and reason succeeds

| Field | Value |
|---|---|
| Preconditions | Appointment is SCHEDULED |
| Actor | STAFF |
| Action | `PUT /api/v1/appointments/{id}/cancel` `{"cancellationType":"CLIENT_INITIATED","reason":"Client is unwell and cannot attend"}` |
| Expected result | `status=CANCELLED`; cancellationType and reason stored |
| HTTP status | 200 |

### APPT-018 — Cancel appointment with each valid cancellationType value

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | Three requests using `CLIENT_INITIATED`, `THERAPIST_INITIATED`, `LATE_CANCELLATION` |
| HTTP status | 200 each |

### APPT-019 — Cancel with reason shorter than 10 chars returns 400

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `"reason":"Too short"` |
| HTTP status | 400 |

### APPT-020 — Cancel already-cancelled appointment returns 400

| Field | Value |
|---|---|
| Preconditions | Appointment is CANCELLED |
| Actor | STAFF |
| Action | Cancel again |
| HTTP status | 400 |

### APPT-021 — PATCH /status cannot transition appointment to CANCELLED (use cancel endpoint)

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `PATCH /api/v1/appointments/{id}/status` `{"status":"CANCELLED"}` |
| Expected result | Error: cancellation must go through cancel endpoint |
| HTTP status | 400 |

### APPT-022 — Status update SCHEDULED → CONFIRMED succeeds

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `PATCH /api/v1/appointments/{id}/status` `{"status":"CONFIRMED"}` |
| HTTP status | 200 |

### APPT-023 — FINANCE role cannot create appointments

| Field | Value |
|---|---|
| Actor | FINANCE |
| Action | `POST /api/v1/appointments` valid payload |
| HTTP status | 403 |

### APPT-024 — Get session types returns only active session types ordered by name

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/v1/appointments/session-types` |
| Expected result | All returned types have `isActive=true`; ordered alphabetically by name |
| HTTP status | 200 |

---

## 11. Session Records

### SESS-001 — Start session from SCHEDULED appointment succeeds

| Field | Value |
|---|---|
| Preconditions | Appointment in SCHEDULED status |
| Actor | STAFF or THERAPIST |
| Action | `POST /api/sessions/start` `{"appointmentId":"<uuid>"}` |
| Expected result | Session created with `status=IN_PROGRESS`; linked `appointmentId`, `therapistId`, `clientId`; appointment status updated to `IN_PROGRESS` |
| HTTP status | 201 |

### SESS-002 — Start session for already-started appointment returns 409

| Field | Value |
|---|---|
| Preconditions | Session already exists for this appointment |
| Actor | STAFF |
| Action | `POST /api/sessions/start` with same `appointmentId` |
| HTTP status | 409 |

### SESS-003 — Start session for non-existent appointment returns 404

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `POST /api/sessions/start` `{"appointmentId":"00000000-0000-0000-0000-000000000099"}` |
| HTTP status | 404 |

### SESS-004 — Start session for CANCELLED appointment returns 400 (cannot start a cancelled appointment)

| Field | Value |
|---|---|
| Preconditions | Appointment is CANCELLED |
| Actor | STAFF |
| Action | `POST /api/sessions/start` |
| HTTP status | 400 |

### SESS-005 — Get session by session ID returns full session detail

| Field | Value |
|---|---|
| Preconditions | Session exists |
| Actor | STAFF |
| Action | `GET /api/sessions/{sessionId}` |
| Expected result | Response includes `id`, `appointmentId`, `therapistId`, `clientId`, `status`, `sessionDate` |
| HTTP status | 200 |

### SESS-006 — Get session by non-existent ID returns 404

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/sessions/00000000-0000-0000-0000-000000000099` |
| HTTP status | 404 |

### SESS-007 — Get session by appointment ID returns the linked session

| Field | Value |
|---|---|
| Preconditions | Session linked to appointment exists |
| Actor | STAFF |
| Action | `GET /api/sessions/by-appointment/{appointmentId}` |
| Expected result | Session with matching `appointmentId` |
| HTTP status | 200 |

### SESS-008 — Get session by appointment ID for appointment with no session returns 404

| Field | Value |
|---|---|
| Preconditions | Appointment exists but no session started |
| Actor | STAFF |
| Action | `GET /api/sessions/by-appointment/{appointmentId}` |
| HTTP status | 404 |

### SESS-009 — List sessions filtered by therapistId returns only that therapist's sessions

| Field | Value |
|---|---|
| Preconditions | Two therapists each have sessions |
| Actor | ADMIN |
| Action | `GET /api/sessions?therapistId=<therapist-A-uuid>` |
| Expected result | Only sessions with `therapistId=<A>` |
| HTTP status | 200 |

### SESS-010 — List sessions filtered by date range returns only sessions within range

| Field | Value |
|---|---|
| Actor | STAFF |
| Action | `GET /api/sessions?startDate=2026-06-01&endDate=2026-06-30` |
| Expected result | Sessions with `sessionDate` within June 2026 only |
| HTTP status | 200 |

### SESS-011 — List sessions filtered by status returns only matching sessions

| Field | Value |
|---|---|
| Preconditions | Mix of IN_PROGRESS and COMPLETED sessions |
| Actor | STAFF |
| Action | `GET /api/sessions?status=IN_PROGRESS` |
| Expected result | Only `IN_PROGRESS` sessions |
| HTTP status | 200 |

### SESS-012 — List sessions with no filters returns all sessions (paginated or full list)

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `GET /api/sessions` |
| Expected result | Non-error response; list of all sessions |
| HTTP status | 200 |

### SESS-013 — FINANCE role cannot access session records

| Field | Value |
|---|---|
| Actor | FINANCE |
| Action | `GET /api/sessions` |
| HTTP status | 403 |

### SESS-014 — Unauthenticated session access returns 401

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `GET /api/sessions/{someId}` |
| HTTP status | 401 |

---

## 12. Care Plans

> Base path: `/api/v1/care-plans`
> Write operations require `MANAGE_CARE_PLANS` (THERAPIST, SYS_ADMIN).
> Read operations require `READ_CARE_PLANS` (all authenticated roles).
> `READ_CLIENTS_ALL` grants supervisors/admin visibility of plans belonging to any therapist.

### CP-001 — Therapist creates care plan for own client

| Field | Value |
|---|---|
| Preconditions | Active client assigned to THERAPIST |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans` `{"clientId":"…","title":"Anxiety management","goals":[{"description":"Reduce avoidance","priority":1}]}` |
| Expected result | Body contains plan with `status=ACTIVE`, one goal with `status=PENDING` |
| HTTP status | 201 |

### CP-002 — Therapist cannot create plan for another therapist's client

| Field | Value |
|---|---|
| Preconditions | Client assigned to THERAPIST-B |
| Actor | THERAPIST-A |
| Action | `POST /api/v1/care-plans` with THERAPIST-B's client ID |
| Expected result | Error response; plan not created |
| HTTP status | 403 |

### CP-003 — Admin can create plan for any client

| Field | Value |
|---|---|
| Preconditions | Active client |
| Actor | ADMIN |
| Action | `POST /api/v1/care-plans` with any client ID |
| Expected result | Plan created successfully |
| HTTP status | 201 |

### CP-004 — Therapist updates goal status to IN_PROGRESS

| Field | Value |
|---|---|
| Preconditions | Active plan with goal in PENDING status |
| Actor | THERAPIST (plan owner) |
| Action | `PUT /api/v1/care-plans/{planId}/goals/{goalId}/status` `{"status":"IN_PROGRESS"}` |
| Expected result | Goal status updated to `IN_PROGRESS` |
| HTTP status | 200 |

### CP-005 — Goal status update to PAUSED is accepted

| Field | Value |
|---|---|
| Preconditions | Active plan with goal in IN_PROGRESS |
| Actor | THERAPIST |
| Action | `PUT /api/v1/care-plans/{planId}/goals/{goalId}/status` `{"status":"PAUSED"}` |
| Expected result | Goal transitions to `PAUSED` |
| HTTP status | 200 |

### CP-006 — Goal status update rejected on closed plan

| Field | Value |
|---|---|
| Preconditions | Plan with `status=CLOSED` |
| Actor | THERAPIST |
| Action | `PUT /api/v1/care-plans/{planId}/goals/{goalId}/status` `{"status":"IN_PROGRESS"}` |
| Expected result | Error response |
| HTTP status | 409 |

### CP-007 — Milestone marked as achieved

| Field | Value |
|---|---|
| Preconditions | Active plan with an unachieved milestone |
| Actor | THERAPIST |
| Action | `PATCH /api/v1/care-plans/{planId}/goals/{goalId}/milestones/{milestoneId}/achieve` |
| Expected result | Milestone `achievedAt` is populated with current date |
| HTTP status | 200 |

### CP-008 — Close plan changes status to CLOSED

| Field | Value |
|---|---|
| Preconditions | Plan with `status=ACTIVE` |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/close` |
| Expected result | Plan `status=CLOSED` |
| HTTP status | 200 |

### CP-009 — Cannot close already-closed plan

| Field | Value |
|---|---|
| Preconditions | Plan with `status=CLOSED` |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/close` |
| Expected result | Error response |
| HTTP status | 409 |

### CP-010 — Archive plan changes status to ARCHIVED

| Field | Value |
|---|---|
| Preconditions | Plan with `status=CLOSED` |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/archive` |
| Expected result | Plan `status=ARCHIVED` |
| HTTP status | 200 |

### CP-011 — Cannot archive an active plan

| Field | Value |
|---|---|
| Preconditions | Plan with `status=ACTIVE` |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/archive` |
| Expected result | Error response |
| HTTP status | 409 |

### CP-012 — Supervisor can read plans for any therapist's clients

| Field | Value |
|---|---|
| Preconditions | Plan belonging to THERAPIST-A |
| Actor | SUPERVISOR |
| Action | `GET /api/v1/care-plans/{planId}` |
| Expected result | Full plan details returned |
| HTTP status | 200 |

### CP-013 — RECEPTION_ADMIN_STAFF cannot create care plans (no MANAGE_CARE_PLANS)

| Field | Value |
|---|---|
| Preconditions | Active client |
| Actor | STAFF |
| Action | `POST /api/v1/care-plans` |
| Expected result | Forbidden |
| HTTP status | 403 |

### CP-014 — Unauthenticated request to care plan endpoint returns 401

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `GET /api/v1/care-plans` |
| HTTP status | 401 |

---

## 13. Progress Tracking & Outcome Measures

> Goal progress notes: `/api/v1/care-plans/{planId}/goals/{goalId}/progress-notes`
> Progress history: `/api/v1/care-plans/{planId}/goals/{goalId}/progress-history`
> Outcome measures: `/api/v1/care-plans/{planId}/outcome-measures`
> Definitions: `/api/v1/outcome-measure-definitions`

### PT-001 — Therapist adds progress note to own plan's goal

| Field | Value |
|---|---|
| Preconditions | Active plan owned by THERAPIST with one goal |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/goals/{goalId}/progress-notes` `{"noteText":"Client shows improved sleep patterns."}` |
| Expected result | Note created with `authorName` and `createdAt` populated |
| HTTP status | 201 |

### PT-002 — Non-owner therapist cannot add progress note

| Field | Value |
|---|---|
| Preconditions | Active plan owned by THERAPIST-A |
| Actor | THERAPIST-B |
| Action | `POST /api/v1/care-plans/{planId}/goals/{goalId}/progress-notes` |
| Expected result | Forbidden |
| HTTP status | 403 |

### PT-003 — Progress note rejected on closed plan

| Field | Value |
|---|---|
| Preconditions | Plan with `status=CLOSED`, THERAPIST is plan owner |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/goals/{goalId}/progress-notes` |
| Expected result | Error response |
| HTTP status | 409 |

### PT-004 — Therapist sees only own notes (no READ_CLIENTS_ALL)

| Field | Value |
|---|---|
| Preconditions | Two notes: one by THERAPIST-A, one by THERAPIST-B (via a shared plan scenario or supervisor backdoor). THERAPIST-A has no READ_CLIENTS_ALL authority. |
| Actor | THERAPIST-A |
| Action | `GET /api/v1/care-plans/{planId}/goals/{goalId}/progress-notes` |
| Expected result | Returns only notes authored by THERAPIST-A |
| HTTP status | 200 |

### PT-005 — Supervisor sees all notes (has READ_CLIENTS_ALL)

| Field | Value |
|---|---|
| Preconditions | Two notes on same goal by different authors |
| Actor | SUPERVISOR |
| Action | `GET /api/v1/care-plans/{planId}/goals/{goalId}/progress-notes` |
| Expected result | Both notes returned |
| HTTP status | 200 |

### PT-006 — Progress history includes status-change events and notes in chronological order

| Field | Value |
|---|---|
| Preconditions | Goal has had status changes (PENDING → IN_PROGRESS) and at least one progress note |
| Actor | THERAPIST |
| Action | `GET /api/v1/care-plans/{planId}/goals/{goalId}/progress-history` |
| Expected result | Array contains both status-change events (`type=STATUS_CHANGE`) and notes (`type=PROGRESS_NOTE`), ordered by timestamp ascending |
| HTTP status | 200 |

### PT-007 — List outcome measure definitions returns active instruments only

| Field | Value |
|---|---|
| Preconditions | Seeded instruments (PHQ-9, GAD-7, DASS-21-DEP, DASS-21-ANX, DASS-21-STR, WHODAS, PCL-5); all active |
| Actor | THERAPIST |
| Action | `GET /api/v1/outcome-measure-definitions` |
| Expected result | Returns 7 definitions; each has `code`, `displayName`, `minScore`, `maxScore` |
| HTTP status | 200 |

### PT-008 — Record PHQ-9 score within range; no threshold breach

| Field | Value |
|---|---|
| Preconditions | Active plan |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/outcome-measures` `{"measureCode":"PHQ9","score":8,"assessmentDate":"2026-04-01","notes":""}` |
| Expected result | Entry created; `thresholdBreached=false` |
| HTTP status | 201 |

### PT-009 — Record PHQ-9 score at or above alert threshold; thresholdBreached = true

| Field | Value |
|---|---|
| Preconditions | Active plan; PHQ-9 alertThreshold = 15 |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/outcome-measures` `{"measureCode":"PHQ9","score":16,"assessmentDate":"2026-04-01"}` |
| Expected result | Entry created; `thresholdBreached=true`, `alertLabel` and `alertSeverity` populated |
| HTTP status | 201 |

### PT-010 — Score above maxScore is rejected

| Field | Value |
|---|---|
| Preconditions | Active plan; PHQ-9 maxScore = 27 |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/outcome-measures` `{"measureCode":"PHQ9","score":28,"assessmentDate":"2026-04-01"}` |
| Expected result | Validation error |
| HTTP status | 400 |

### PT-011 — Future assessment date is rejected

| Field | Value |
|---|---|
| Preconditions | Active plan |
| Actor | THERAPIST |
| Action | `POST /api/v1/care-plans/{planId}/outcome-measures` `{"measureCode":"PHQ9","score":10,"assessmentDate":"2099-01-01"}` |
| Expected result | Validation error |
| HTTP status | 400 |

### PT-012 — Non-owner therapist cannot record outcome measure entry

| Field | Value |
|---|---|
| Preconditions | Active plan owned by THERAPIST-A |
| Actor | THERAPIST-B |
| Action | `POST /api/v1/care-plans/{planId}/outcome-measures` |
| Expected result | Forbidden |
| HTTP status | 403 |

### PT-013 — Outcome measure recording rejected on closed plan

| Field | Value |
|---|---|
| Preconditions | Plan with `status=CLOSED` |
| Actor | THERAPIST (plan owner) |
| Action | `POST /api/v1/care-plans/{planId}/outcome-measures` |
| Expected result | Error response |
| HTTP status | 409 |

### PT-014 — List outcome measure entries returns paginated results

| Field | Value |
|---|---|
| Preconditions | 3 PHQ-9 entries recorded on plan |
| Actor | THERAPIST |
| Action | `GET /api/v1/care-plans/{planId}/outcome-measures?measureCode=PHQ9&page=0&size=2` |
| Expected result | Returns 2 entries; `page.totalElements=3` |
| HTTP status | 200 |

### PT-015 — Chart data returns time-series ordered by assessment date

| Field | Value |
|---|---|
| Preconditions | 3 PHQ-9 entries on different dates |
| Actor | THERAPIST |
| Action | `GET /api/v1/care-plans/{planId}/outcome-measures/chart-data?measureCode=PHQ9` |
| Expected result | `dataPoints` array ordered by `assessmentDate` ascending; each point has `date` and `score` |
| HTTP status | 200 |

### PT-016 — Unauthenticated request to outcome measures endpoint returns 401

| Field | Value |
|---|---|
| Actor | ANON |
| Action | `GET /api/v1/care-plans/{planId}/outcome-measures` |
| HTTP status | 401 |

---

## 14. Cross-Entity Business Rule Validations

### XE-001 — Booking appointment during therapist's approved leave period is rejected

| Field | Value |
|---|---|
| Preconditions | Therapist has approved leave for 2026-07-01 to 2026-07-05; therapist has recurring schedule including those dates |
| Actor | STAFF |
| Action | `POST /api/v1/appointments` with `startTime` during the leave period |
| Expected result | Conflict or availability violation error |
| HTTP status | 409 or 422 |

### XE-002 — Converting lead copies all contact methods to client record exactly

| Field | Value |
|---|---|
| Preconditions | Lead has both EMAIL and PHONE contact methods |
| Actor | STAFF |
| Action | Convert lead; GET the resulting client |
| Expected result | Client has exactly the same contact methods (type, value, isPrimary) as the lead at time of conversion |
| HTTP status | 201 then 200 |

### XE-003 — Deactivating a therapist does not affect existing SCHEDULED appointments

| Field | Value |
|---|---|
| Preconditions | Therapist has 2 future SCHEDULED appointments |
| Actor | ADMIN |
| Action | `POST /api/v1/therapists/{id}/deactivate` |
| Expected result | Therapist `employmentStatus=INACTIVE`; existing appointments remain in SCHEDULED status |
| HTTP status | 200 (deactivate), 200 per appointment GET |

### XE-004 — Rescheduling appointment checks conflicts excluding itself

| Field | Value |
|---|---|
| Preconditions | Appointment A at time T1; appointment B at time T2 (no overlap) |
| Actor | STAFF |
| Action | Reschedule A to T2 |
| Expected result | Conflict detected (A conflicts with B); not a false self-conflict |
| HTTP status | 409 |

### XE-005 — After approving therapist leave, availability returns no slots within leave range

| Field | Value |
|---|---|
| Preconditions | Therapist has Mon–Fri schedule; leave approved for one week |
| Actor | STAFF |
| Action | `GET /api/v1/therapists/{id}/availability` for the approved leave week |
| Expected result | No slots returned |
| HTTP status | 200 |

### XE-006 — Lead notes are transferred to client notes with "Transferred from lead" header

| Field | Value |
|---|---|
| Preconditions | Lead has `notes="Referred by Dr. Smith"` |
| Actor | STAFF |
| Action | Convert lead; GET client |
| Expected result | Client notes contain original note content with a dated "Transferred from lead" prefix or similar marker |
| HTTP status | 201 then 200 |

### XE-007 — Starting a session transitions appointment status from SCHEDULED to IN_PROGRESS

| Field | Value |
|---|---|
| Preconditions | Appointment in SCHEDULED status |
| Actor | STAFF |
| Action | `POST /api/sessions/start` with that appointment ID; then `GET /api/v1/appointments/{id}` |
| Expected result | Appointment `status=IN_PROGRESS` |
| HTTP status | 201 then 200 |

### XE-008 — Client timeline includes both lead conversion event and subsequent appointment events

| Field | Value |
|---|---|
| Preconditions | Client was converted from a lead; has at least one appointment |
| Actor | STAFF |
| Action | `GET /api/v1/clients/{id}/timeline` |
| Expected result | Timeline contains at minimum a `LEAD_CONVERTED` event and at least one appointment event, in chronological order |
| HTTP status | 200 |

### XE-009 — Concurrent updates to same client profile: one succeeds and one receives 409

| Field | Value |
|---|---|
| Preconditions | Client at version 0 |
| Actor | Two STAFF users simultaneously |
| Action | Both send `PUT /api/v1/clients/{id}` with version=0 |
| Expected result | Exactly one receives 200 (version incremented to 1); the other receives 409 |
| HTTP status | One 200, one 409 |

### XE-010 — Therapist cannot book appointment for a client they are not assigned to (if RBAC enforced)

| Field | Value |
|---|---|
| Preconditions | Therapist A is logged in; client is assigned to therapist B |
| Actor | THERAPIST A (with `READ_ASSIGNED_CLIENTS` only) |
| Action | `POST /api/v1/appointments` referencing the unassigned client |
| Expected result | 403 or 404 (depending on whether unassigned client is visible) |
| HTTP status | 403 or 404 |

### XE-011 — Availability override for a date overrides (but does not permanently delete) recurring schedule

| Field | Value |
|---|---|
| Preconditions | Therapist has Monday recurring 09:00–17:00 |
| Actor | ADMIN |
| Action | Add unavailability override for a specific Monday; delete the override; query availability for that Monday |
| Expected result | After override deleted, original recurring schedule slots are restored |
| HTTP status | 204 (delete), 200 (availability query) |

### XE-012 — Pagination sort parameter with invalid field name falls back gracefully (no 500)

| Field | Value |
|---|---|
| Actor | ADMIN |
| Action | `GET /api/v1/admin/users?sort=nonExistentField,desc` |
| Expected result | 200 with results sorted by default (`createdAt desc`); not a 500 Internal Server Error |
| HTTP status | 200 |

---

## Notes for Implementation

**Priority order for test implementation:**

1. AUTH scenarios (foundation for all others)
2. USR (admin bootstrap)
3. THXP-001 to THXP-005 (therapist setup needed for schedule/leave/appointment tests)
4. SCH + AVAIL (prerequisite for appointment conflict tests)
5. LEAD + CLT (CRM core)
6. LVE + APPT + SESS (scheduling workflow)
7. XE cross-entity scenarios (require full system state)

**Known areas of partial/TODO implementation noted in code:**

- `TherapistLeaveController.checkLeaveConflicts` currently always returns empty conflicts (TODO noted in source)
- Session controller uses `UUID.randomUUID()` as actor ID instead of extracting from JWT (TODO noted)
- Leave controller has TODO comments for therapist self-access enforcement
- `AvailabilityController` currently does not subtract already-booked appointments from availability slots (noted in source comment)

These TODOs indicate tests for those behaviors should be written but may be expected to fail until the implementation is completed.

---

### Critical Files for Implementation

- `/Users/Serhiy_Piddubchak/Documents/prj/psy-assistant/backend/tests/integration/conftest.py`
- `/Users/Serhiy_Piddubchak/Documents/prj/psy-assistant/backend/tests/integration/utils/data_factory.py`
- `/Users/Serhiy_Piddubchak/Documents/prj/psy-assistant/backend/tests/integration/utils/auth_helper.py`
- `/Users/Serhiy_Piddubchak/Documents/prj/psy-assistant/backend/src/main/java/com/psyassistant/scheduling/rest/AppointmentController.java`
- `/Users/Serhiy_Piddubchak/Documents/prj/psy-assistant/backend/src/main/java/com/psyassistant/crm/clients/ClientController.java`

---

## Care Plans (PA-43)

### Happy Path
1. THERAPIST creates a care plan for their assigned client with at least one goal → HTTP 201, plan returned in ACTIVE status
2. THERAPIST lists care plans for client → returns plan in list
3. THERAPIST retrieves care plan detail → goals, interventions, milestones nested in response
4. THERAPIST updates goal status to IN_PROGRESS → goal status updated
5. THERAPIST marks milestone as achieved → milestone shows achievedAt timestamp
6. THERAPIST closes an ACTIVE plan → status changes to CLOSED
7. THERAPIST archives a CLOSED plan → status changes to ARCHIVED
8. GET care plan audit log → returns audit entries for all mutations
9. GET /api/v1/config/care-plan-intervention-types → returns configured intervention types list

### Authorization
10. RECEPTION_ADMIN_STAFF can read care plans (READ_CARE_PLANS) but cannot create/update/close/archive (MANAGE_CARE_PLANS)
11. THERAPIST cannot read care plans of clients assigned to other therapists (assignment-based access)
12. SYSTEM_ADMINISTRATOR can read and manage all care plans
13. Unauthenticated request → 401

### Business Rules
14. Creating a 4th active care plan for a client when max is 3 → 409 MaxActivePlansExceeded
15. Attempting to close/archive an already CLOSED or ARCHIVED plan → 409 CarePlanNotActive
16. Adding an intervention with an invalid interventionType → 400 validation error
17. Creating a care plan with no goals → 400 validation error
