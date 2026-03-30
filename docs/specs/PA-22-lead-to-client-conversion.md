# PA-22 — Lead-to-Client Conversion Workflow
**Status:** Ready for Implementation
**Branch target:** `main`
**Last updated:** 2026-03-27
**Author:** Business Analyst (AI-assisted refinement)

---

## 1. Overview

### Feature Name
Lead-to-Client Conversion Workflow

### Problem Statement
When a prospective client agrees to begin therapy, reception/admin staff must manually transcribe all intake information from the lead record into a separate client record. This re-entry introduces transcription errors, loses the intake activity history, and delays the therapist's access to pre-session context. The conversion workflow eliminates this friction by atomically promoting a qualified lead to a fully-populated client record in a single confirmed action.

### Goal
Reception/admin staff can convert a lead with status `QUALIFIED` into a client record in one confirmed action. Zero fields from the lead need to be re-entered. The complete activity history is visible on the new client record. The end-to-end operation completes within 3 seconds under normal load.

### Primary Stakeholders
| Role | Interest |
|---|---|
| Reception / Admin Staff | Performing the conversion; saving data-entry time |
| Therapist | Receiving a client with pre-populated context and full history |
| Supervisor | Oversight; audit trail completeness |
| System Administrator | RBAC enforcement; data integrity |
| Developer | Implementing atomic transaction, FSM guard, and bidirectional link |
| QA Engineer | Verifying all happy-path and guard scenarios end-to-end |

---

## 2. Resolved Open Questions

### OQ-1: Should staff be able to review and edit pre-populated client fields before finalising?
**Resolution: Yes — a two-step confirmation dialog with editable fields.**

The conversion dialog opens pre-populated with all transferable lead fields. Staff may edit any field before confirming. The backend receives the final (possibly edited) values in the `POST /leads/{id}/convert` request body. This prevents a silent promotion of stale or incorrect data and matches the edit-then-confirm pattern already used in `EditLeadDialogComponent`.

### OQ-2: Which lead fields are transferable to the client record?
**Resolution — Transferable fields:**
| Lead field | Client field | Notes |
|---|---|---|
| `fullName` | `fullName` | Direct copy; editable in dialog |
| `contactMethods[]` (type, value, isPrimary) | `contactMethods[]` | All methods copied |
| `notes` | `notes` | Appended with a dated "Transferred from lead" header |
| `ownerId` | `ownerId` | See OQ-4 for null case |

**Non-transferable (stay on lead record only):**
- `source` — lead acquisition metadata, not a client property
- `status` — lead lifecycle field
- `lastContactDate` — lead CRM timestamp
- `createdAt` / `updatedAt` / `createdBy` — lead audit fields (the client record gets its own `BaseEntity` audit fields stamped at creation time)

### OQ-3: Should supervisors or therapists be notified automatically when a lead is converted?
**Resolution: No automatic notification in this MVP.**

The notification surface (channel, template, opt-in preference) is not yet defined. A `LEAD_CONVERTED` event is written to the audit log; a future notifications sprint can subscribe to it. This is recorded explicitly in Out-of-Scope.

### OQ-4: If a lead has no assigned owner, who owns the resulting client record?
**Resolution: The staff member performing the conversion is set as `ownerId` on the client record.**

This is consistent with the existing `createLead` pattern where `actorId` is derived from `UserManagementService.currentPrincipalId()`. The pre-populated dialog shows the acting user as the owner and staff may change it before confirming.

---

## 3. Field Mapping Reference

```
Lead entity                      →    Client entity (new)
─────────────────────────────────────────────────────────────
lead.id                          →    client.sourceLeadId          (FK, bidirectional link)
lead.fullName                    →    client.fullName
lead.contactMethods[]            →    client.contactMethods[]
lead.notes                       →    client.notes (prefixed with dated header)
lead.ownerId ?? actorId          →    client.ownerId
[not transferred] lead.source    →    — (stays on lead)
[generated]                      →    client.id (new UUID)
[generated]                      →    client.createdAt / updatedAt / createdBy
[set by service]                 →    lead.status = CONVERTED
[set by service]                 →    lead.convertedClientId = client.id
```

---

## 4. Epics

### Epic 1: Backend Conversion API
Implement the atomic `POST /leads/{id}/convert` endpoint, FSM guard, client entity, bidirectional persistence, and audit logging.

### Epic 2: Frontend Conversion UX
Add the "Convert to Client" action to the lead detail view, implement the two-step review/edit confirmation dialog, handle all error states, and redirect to the new client profile on success.

---

## 5. User Stories

---

### Story 1-1: Convert a Qualified Lead (Happy Path)

**As a** reception/admin staff member,
**I want** to trigger "Convert to Client" on a lead with status `QUALIFIED` and confirm via a review dialog,
**So that** a fully-populated client record is created without re-entering any data.

**Story Points:** 8 — atomic transaction, new `Client` entity + migration, field mapping, audit log entry, bidirectional FK, redirect, and moderate dialog complexity.

#### Acceptance Criteria

**Behavioral (Gherkin):**

```gherkin
Scenario: Successful lead-to-client conversion with default values
  Given a lead with status QUALIFIED exists with fullName "Anna Kovalenko",
        two contact methods (EMAIL primary, PHONE secondary), and notes "Initial intake"
  And the lead has an assigned ownerId
  When I click "Convert to Client" on the lead detail page
  Then a review dialog opens pre-populated with:
       fullName = "Anna Kovalenko",
       contactMethods = [EMAIL (primary), PHONE (secondary)],
       notes = "Anna's notes",
       ownerId = lead's ownerId
  When I confirm the dialog without changing any fields
  Then POST /leads/{id}/convert is called with the pre-populated values
  And the server returns HTTP 201 with the new client's id
  And the lead's status is CONVERTED
  And the lead record stores convertedClientId = <new client UUID>
  And the client record stores sourceLeadId = <lead UUID>
  And an audit log entry of type LEAD_CONVERTED is created with actorId, leadId, and clientId
  And I am redirected to /clients/<new client UUID>
  And the client profile shows all transferred fields populated

Scenario: Staff edits fields in review dialog before confirming
  Given a lead with status QUALIFIED has notes "Old note"
  When I open the conversion dialog
  And I change notes to "Updated note" and add a second owner
  And I confirm
  Then the client record is created with notes = "Updated note"
  And the lead record transitions to CONVERTED
  And the audit log records the conversion with actorId and both IDs
```

**Non-Functional:**
- [ ] The entire conversion (lead status update + client insert + audit log) is wrapped in a single `@Transactional` database transaction; either all three succeed or all roll back.
- [ ] The `POST /leads/{id}/convert` endpoint responds within 3 seconds under normal load (single-node, < 50 concurrent users).
- [ ] Only principals with the `MANAGE_LEADS` authority may call the endpoint; `READ_LEADS` principals receive HTTP 403.
- [ ] The endpoint is idempotent with respect to the duplicate-conversion guard (see Story 1-3).

#### Edge Cases
- Lead `ownerId` is null: `ownerId` on the client record defaults to `actorId`; dialog pre-populates with the acting user's display name.
- Lead has only a PHONE contact method and no email: client record is created with the PHONE method; no email field is required on the client entity.
- `notes` field on lead is null: `client.notes` is also null (no header is prepended for blank notes).
- All contact method `isPrimary` flags are false: the first contact method in the list is treated as primary; the service normalises this.
- The `actorId` cannot be resolved (deleted user): the service throws a 500; the transaction rolls back.

#### Dependencies
- Lead FSM (PA-20, already implemented — `QUALIFIED → CONVERTED` transition is valid per `LeadStatus.java`)
- New `Client` entity and `V7__clients.sql` migration (created in this ticket)
- `MANAGE_LEADS` permission already assigned to `RECEPTION_ADMIN_STAFF` and `SYSTEM_ADMINISTRATOR`

---

### Story 1-2: Activity History Preserved on Client Record

**As a** therapist,
**I want** the full pre-conversion activity history from the lead to be visible in the client's timeline,
**So that** I have complete context from day one without asking reception staff to summarise it.

**Story Points:** 5 — audit log query scoped to lead, timeline rendering on client page, section label.

#### Acceptance Criteria

**Behavioral (Gherkin):**

```gherkin
Scenario: All pre-conversion audit entries appear on the client timeline
  Given a lead has three audit log entries (LEAD_CREATED, LEAD_UPDATED, LEAD_STATUS_CHANGED)
  When the lead is converted to a client
  Then GET /clients/{clientId}/timeline returns all three entries
  And each entry is labelled with section heading "Pre-conversion history"
  And entries are ordered oldest-first within that section
  And a fourth entry LEAD_CONVERTED appears at the top of the timeline with the conversion timestamp

Scenario: Client created without any prior lead activity
  Given a lead has only a LEAD_CREATED audit entry
  When the lead is converted
  Then the client timeline shows one pre-conversion entry (LEAD_CREATED)
  And one LEAD_CONVERTED entry
```

**Non-Functional:**
- [ ] `GET /clients/{clientId}/timeline` returns within 500 ms for leads with up to 200 audit entries.
- [ ] The timeline endpoint requires `MANAGE_LEADS` or `READ_LEADS` authority.

#### Edge Cases
- Lead audit log has 0 entries (e.g., seed/test data): client timeline shows only the `LEAD_CONVERTED` entry; "Pre-conversion history" section is omitted from the UI if empty.
- Very large history (200+ entries): pagination must be supported on the timeline endpoint; default page size 50.

#### Dependencies
- Story 1-1 (client record must exist with `sourceLeadId`)
- `AuditLog` table already stores `leadId` inside the `detail` JSON column; the timeline query filters by this

---

### Story 1-3: Duplicate Conversion Guard

**As a** system,
**I want** to reject any attempt to convert a lead that is already in status `CONVERTED`,
**So that** a second client record is never accidentally created for the same lead.

**Story Points:** 2 — single guard check, error response, UI link to existing client.

#### Acceptance Criteria

**Behavioral (Gherkin):**

```gherkin
Scenario: Attempt to convert an already-converted lead
  Given a lead has status CONVERTED and convertedClientId = "abc-123"
  When POST /leads/{id}/convert is called
  Then the server returns HTTP 409 Conflict
  And the response body contains:
       { "code": "LEAD_ALREADY_CONVERTED", "existingClientId": "abc-123" }

Scenario: UI surfaces link to existing client
  Given a lead with status CONVERTED is viewed in the lead detail page
  Then the "Convert to Client" button is absent or disabled
  And a read-only notice reads "Converted — view client record" with a link to /clients/{convertedClientId}
```

**Non-Functional:**
- [ ] The guard check is performed inside the same transaction as the conversion attempt; no race condition can produce two client records for the same lead (use a unique constraint on `client.source_lead_id`).

#### Edge Cases
- Lead status is `CONVERTED` but `convertedClientId` is null (corrupt state): the server returns HTTP 409 with `existingClientId: null`; the UI shows "Converted — client record unavailable" with no link.
- Concurrent requests: the unique constraint on `client.source_lead_id` is the last line of defence; one request will get a 409 from the DB constraint, which the service layer catches and re-throws as `LeadAlreadyConvertedException`.

#### Dependencies
- Story 1-1 (`convertedClientId` column on `leads` table)
- Unique constraint on `clients.source_lead_id` (V7 migration)

---

### Story 1-4: Status Guard for Non-Qualified Leads

**As a** system,
**I want** to reject conversion attempts on leads not in `QUALIFIED` status,
**So that** only properly assessed leads can become client records.

**Story Points:** 2 — reuses existing FSM guard pattern from `transitionStatus`.

#### Acceptance Criteria

**Behavioral (Gherkin):**

```gherkin
Scenario: Conversion blocked for NEW lead
  Given a lead has status NEW
  When POST /leads/{id}/convert is called
  Then the server returns HTTP 422 Unprocessable Entity
  And the response body contains { "code": "INVALID_STATUS_TRANSITION" }

Scenario: Conversion blocked for CONTACTED lead
  Given a lead has status CONTACTED
  When POST /leads/{id}/convert is called
  Then the server returns HTTP 422
  And the response body contains { "code": "INVALID_STATUS_TRANSITION" }

Scenario: Conversion blocked for INACTIVE lead
  Given a lead has status INACTIVE
  When POST /leads/{id}/convert is called
  Then the server returns HTTP 422
  And the response body contains { "code": "INVALID_STATUS_TRANSITION" }

Scenario: Convert button is hidden for non-qualifying statuses
  Given a lead has status NEW, CONTACTED, or INACTIVE
  When I view the lead detail page
  Then the "Convert to Client" button is absent from the action menu
```

**Non-Functional:**
- [ ] The status guard reuses the existing `LeadStatus.canTransitionTo(CONVERTED)` FSM method; no duplicate guard logic is introduced.

#### Edge Cases
- Client sends a manually crafted request bypassing the UI: server-side guard is the authoritative check.

#### Dependencies
- `LeadStatus.QUALIFIED.canTransitionTo(CONVERTED)` already returns `true` (confirmed in `LeadStatus.java`)

---

### Story 2-1: Conversion Review Dialog (Frontend)

**As a** reception/admin staff member,
**I want** a review dialog that pre-populates all transferable fields before I confirm conversion,
**So that** I can verify and correct data before the client record is created.

**Story Points:** 5 — new Angular dialog component, form pre-population, submission wiring, error states, redirect.

#### Acceptance Criteria

**Behavioral (Gherkin):**

```gherkin
Scenario: Dialog opens with pre-populated fields
  Given I am on the lead detail page for a QUALIFIED lead
  When I click "Convert to Client"
  Then a modal dialog opens with title "Convert to Client"
  And the form shows:
       - Full name field (pre-filled, required)
       - Contact methods (pre-filled, at least one required)
       - Notes field (pre-filled, optional)
       - Owner field (pre-filled with lead's owner or current user if no owner)
  And a "Confirm Conversion" primary button and a "Cancel" secondary button are visible

Scenario: Successful submission redirects to new client profile
  Given the conversion dialog is open with valid data
  When I click "Confirm Conversion"
  Then the button shows a loading spinner and is disabled
  And on success I am navigated to /clients/<new client UUID>
  And a success toast is displayed: "Lead successfully converted to client"

Scenario: Server error during conversion
  Given the conversion dialog is open
  When the server returns HTTP 422 (status guard) or HTTP 409 (duplicate guard)
  Then the dialog remains open
  And an inline error message is displayed using the server's code:
       - INVALID_STATUS_TRANSITION → "This lead cannot be converted in its current status."
       - LEAD_ALREADY_CONVERTED    → "This lead has already been converted. [View client record]"
  And the form fields remain editable

Scenario: Cancel dismisses dialog without any changes
  Given the conversion dialog is open
  When I click "Cancel"
  Then the dialog closes
  And no API call is made
  And the lead remains in QUALIFIED status
```

**Non-Functional:**
- [ ] The dialog is keyboard-navigable: Tab cycles through fields; Enter submits; Escape cancels.
- [ ] `aria-modal="true"`, `aria-labelledby` on the dialog heading, and `role="alert"` on error messages.
- [ ] The "Confirm Conversion" button shows a spinner and is `[disabled]` during the in-flight request to prevent double-submission.
- [ ] The dialog follows the existing design system: same overlay, border-radius, button styles, and colour tokens as `EditLeadDialogComponent`.

#### Edge Cases
- Network timeout (> 30 s): the spinner stops and a generic "Conversion failed — please try again" message is shown.
- User navigates away while dialog is open: the dialog closes without submitting.
- Lead is edited by another user between "Convert to Client" click and dialog confirm: the server returns 422 or 409 as appropriate; the dialog surfaces the error.

#### Dependencies
- Story 1-1 (`POST /leads/{id}/convert` endpoint)
- Story 1-3 (error response shape for duplicate guard)
- Angular `Router` for redirect to `/clients/:id`
- `LeadService.convertLead(id, payload)` method added to frontend service

---

### Story 2-2: Convert Button Visibility in Lead List and Detail

**As a** reception/admin staff member,
**I want** the "Convert to Client" action to appear only for leads in `QUALIFIED` status,
**So that** the UI matches the server-side rules and I am not prompted to perform invalid conversions.

**Story Points:** 2 — conditional rendering in two existing components, no new API calls.

#### Acceptance Criteria

**Behavioral (Gherkin):**

```gherkin
Scenario: Convert button appears in lead list row for QUALIFIED leads
  Given the lead list shows a lead with status QUALIFIED
  Then the action menu for that row includes a "Convert to Client" button
  And the Archive button is absent (QUALIFIED leads cannot be archived to prevent data loss)

  [ASSUMPTION] Archive is blocked for QUALIFIED leads in the UI; the server-side FSM
  already permits QUALIFIED → INACTIVE, so this is a UI-only guard to prevent accidental
  archival of conversion-ready leads. See Open Questions.

Scenario: Convert button absent for non-QUALIFIED leads in lead list
  Given the lead list shows leads with statuses NEW, CONTACTED, CONVERTED, or INACTIVE
  Then none of those rows show a "Convert to Client" button

Scenario: Convert button visible on QUALIFIED lead detail page
  Given I navigate to the detail page of a QUALIFIED lead
  Then a "Convert to Client" button is visible in the actions area
  And the Archive button is not present

Scenario: Converted lead detail page shows client link
  Given I navigate to the detail page of a CONVERTED lead
  Then the action area shows "Converted — view client record" as a link to /clients/{convertedClientId}
  And neither "Convert to Client" nor "Archive" buttons are shown
```

**Non-Functional:**
- [ ] Visibility logic is driven solely by `lead.status` from the API response; no client-side role checks are duplicated (server enforces RBAC).
- [ ] The "Convert to Client" button uses the existing `btn-primary` CSS class for visual prominence.

#### Edge Cases
- Lead transitions from `QUALIFIED` to `CONVERTED` in a background tab: on returning to the tab and refreshing, the button updates to the client link.

#### Dependencies
- `LeadListComponent` and `LeadDetailComponent` (lead detail page must exist or be created)
- Story 1-3 (`convertedClientId` field in `LeadDetailDto`)

---

## 6. API Contract

### POST /api/v1/leads/{id}/convert

**Authority required:** `MANAGE_LEADS`

**Request body:**
```json
{
  "fullName": "Anna Kovalenko",
  "contactMethods": [
    { "type": "EMAIL", "value": "anna@example.com", "isPrimary": true },
    { "type": "PHONE", "value": "+380501234567",    "isPrimary": false }
  ],
  "notes": "Initial intake — transferred from lead",
  "ownerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

**Success — HTTP 201:**
```json
{
  "clientId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "leadId":   "8b1e3d4a-9c2f-4b5e-a1d7-6f8e2c0b4a3d"
}
```

**Error responses:**

| HTTP | `code` | Condition |
|---|---|---|
| 404 | `NOT_FOUND` | Lead ID does not exist |
| 409 | `LEAD_ALREADY_CONVERTED` | Lead status is already `CONVERTED`; body includes `existingClientId` |
| 422 | `INVALID_STATUS_TRANSITION` | Lead status is not `QUALIFIED` |
| 403 | `ACCESS_DENIED` | Principal lacks `MANAGE_LEADS` |
| 400 | `VALIDATION_ERROR` | `fullName` blank or `contactMethods` empty |

**Note on 409 body:**
```json
{
  "timestamp": "...",
  "status": 409,
  "message": "Lead abc-123 is already converted",
  "code": "LEAD_ALREADY_CONVERTED",
  "existingClientId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "path": "/api/v1/leads/{id}/convert"
}
```
`ErrorResponse` must be extended to carry the optional `existingClientId` field, or a new `ConversionErrorResponse` record created. [ASSUMPTION: A new `ConversionErrorResponse` record is the cleaner approach to avoid polluting the generic `ErrorResponse`.]

---

## 7. Database Migration — V7

**File:** `V7__clients.sql`

```sql
-- Clients table (initial schema — minimal fields required for conversion)
CREATE TABLE IF NOT EXISTS clients (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name        VARCHAR(255) NOT NULL,
    owner_id         UUID         REFERENCES users(id) ON DELETE SET NULL,
    notes            TEXT,
    source_lead_id   UUID         UNIQUE REFERENCES leads(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255)
);

-- Contact methods for clients (mirrors lead_contact_methods)
CREATE TABLE IF NOT EXISTS client_contact_methods (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id  UUID         NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    type       VARCHAR(20)  NOT NULL CHECK (type IN ('EMAIL', 'PHONE')),
    value      VARCHAR(255) NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Bidirectional link: lead → client
ALTER TABLE leads ADD COLUMN IF NOT EXISTS converted_client_id UUID
    REFERENCES clients(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_clients_source_lead_id ON clients(source_lead_id);
CREATE INDEX IF NOT EXISTS idx_clients_owner_id       ON clients(owner_id);
CREATE INDEX IF NOT EXISTS idx_ccm_client_id          ON client_contact_methods(client_id);
```

**UNIQUE constraint on `clients.source_lead_id`** is the DB-level duplicate-conversion guard. It prevents two concurrent conversion requests from creating two client rows for the same lead even if both pass the application-layer status check.

---

## 8. Implementation Checklist

### Backend

#### Domain / Data Layer
- [ ] Create `com.psyassistant.clients.Client` entity extending `BaseEntity`; fields: `fullName`, `ownerId`, `notes`, `sourceLeadId`
- [ ] Create `com.psyassistant.clients.ClientContactMethod` entity (mirrors `LeadContactMethod`)
- [ ] Add `convertedClientId` (`UUID`, nullable) field to `Lead` entity
- [ ] Create `V7__clients.sql` migration per schema above
- [ ] Create `ClientRepository extends JpaRepository<Client, UUID>`
- [ ] Add unique constraint validation: `source_lead_id` is `UNIQUE` in migration; no additional application check needed beyond catching `DataIntegrityViolationException`

#### Exception Classes
- [ ] Create `LeadAlreadyConvertedException` (extends `RuntimeException`); carries `leadId` and `existingClientId`
- [ ] Create `ConversionErrorResponse` record extending `ErrorResponse` shape with an additional `existingClientId` field
- [ ] Register `LeadAlreadyConvertedException` handler in `GlobalExceptionHandler` → HTTP 409

#### Service Layer
- [ ] Add `convertLead(UUID leadId, ConvertLeadRequest request, UUID actorId)` to `LeadService`
  - Load lead or throw `EntityNotFoundException`
  - Guard: `lead.getStatus() != QUALIFIED` → throw `InvalidStatusTransitionException(from, CONVERTED)`
  - Guard: `lead.getStatus() == CONVERTED` → throw `LeadAlreadyConvertedException(leadId, lead.getConvertedClientId())`
  - Default `ownerId`: if `request.ownerId()` is null, use `lead.getOwnerId()` if not null, else use `actorId`
  - Prepend notes header if `request.notes()` is not blank: `"[Transferred from lead on <ISO date>]\n" + request.notes()`
  - Create and save `Client` (with contact methods)
  - Update `lead.setStatus(CONVERTED)` and `lead.setConvertedClientId(client.getId())`
  - Save lead
  - Write `LEAD_CONVERTED` audit log entry: `{ "event": "LEAD_CONVERTED", "leadId": "...", "clientId": "..." }`
  - Return `ConvertLeadResponseDto { clientId, leadId }`
  - Entire method annotated `@Transactional`

#### DTO Classes
- [ ] Create `ConvertLeadRequest` record: `fullName` (required), `contactMethods` (min size 1), `notes` (nullable), `ownerId` (nullable)
- [ ] Create `ConvertLeadResponseDto` record: `clientId`, `leadId`
- [ ] Update `LeadDetailDto` to include `convertedClientId` (nullable `UUID`)

#### Controller
- [ ] Add `POST /api/v1/leads/{id}/convert` endpoint to `LeadController`
  - `@PreAuthorize("hasAuthority('MANAGE_LEADS')")`
  - Returns `ResponseEntity<ConvertLeadResponseDto>` with status `201 CREATED`
  - OpenAPI `@Operation`, `@ApiResponses` for 201, 400, 403, 404, 409, 422

#### Client Read Endpoint (minimal, required for redirect)
- [ ] Create `com.psyassistant.clients.ClientController` with `GET /api/v1/clients/{id}` returning `ClientDetailDto`
  - Requires `MANAGE_LEADS` or `READ_LEADS` authority
  - Returns 404 if not found

#### Tests
- [ ] `LeadServiceTest`: `convertLeadSucceedsForQualifiedLead` — verifies client saved, lead CONVERTED, audit written
- [ ] `LeadServiceTest`: `convertLeadThrows422ForNewLead`
- [ ] `LeadServiceTest`: `convertLeadThrows422ForContactedLead`
- [ ] `LeadServiceTest`: `convertLeadThrows422ForInactiveLead`
- [ ] `LeadServiceTest`: `convertLeadThrows409WhenAlreadyConverted`
- [ ] `LeadServiceTest`: `convertLeadDefaultsOwnerToActorIdWhenLeadHasNoOwner`
- [ ] `LeadServiceTest`: `convertLeadDefaultsOwnerToLeadOwnerWhenSet`
- [ ] `LeadControllerTest`: `postConvertReturns201ForManageLeadsAuthority`
- [ ] `LeadControllerTest`: `postConvertReturns403ForReadLeadsAuthority`
- [ ] `LeadControllerTest`: `postConvertReturns422ForInvalidStatus`
- [ ] `LeadControllerTest`: `postConvertReturns409WhenAlreadyConverted`
- [ ] `LeadControllerTest`: `postConvertReturns404WhenLeadNotFound`
- [ ] `LeadControllerTest`: `postConvertReturns400WhenFullNameMissing`
- [ ] Integration test: concurrent conversion of the same lead → exactly one succeeds with 201, the other receives 409

---

### Frontend

#### Service
- [ ] Add `convertLead(id: string, payload: ConvertLeadPayload): Observable<ConvertLeadResponse>` to `LeadService`
  - `POST /api/v1/leads/{id}/convert`
- [ ] Add `ConvertLeadPayload` interface to `lead.model.ts`: `{ fullName, contactMethods, notes?, ownerId? }`
- [ ] Add `ConvertLeadResponse` interface: `{ clientId: string, leadId: string }`
- [ ] Update `LeadDetail` interface to include `convertedClientId: string | null`

#### Component: ConvertLeadDialogComponent
- [ ] Create `frontend/src/app/features/leads/components/convert-lead-dialog/convert-lead-dialog.component.ts`
- [ ] `@Input() lead: LeadDetail`
- [ ] `@Output() converted = new EventEmitter<ConvertLeadResponse>()`
- [ ] `@Output() cancelled = new EventEmitter<void>()`
- [ ] Form fields: `fullName` (required), `contactMethods` FormArray (min 1, reuse pattern from `EditLeadDialogComponent`), `notes`, `ownerId`
- [ ] On init: pre-populate from `lead`; `ownerId` defaults to current user if `lead.ownerId` is null [ASSUMPTION: current user is readable from an auth service/store]
- [ ] Submit: call `leadService.convertLead()`; show spinner; disable button
- [ ] On success: emit `converted`
- [ ] On 409 `LEAD_ALREADY_CONVERTED`: show inline error with "View client record" link using `existingClientId` from response
- [ ] On 422 `INVALID_STATUS_TRANSITION`: show inline error "This lead cannot be converted in its current status."
- [ ] On other errors: generic retry message
- [ ] Cancel: emit `cancelled`
- [ ] Keyboard: Escape closes; Tab cycles; aria-modal, aria-labelledby, role="alert" on errors
- [ ] Styling: reuse design tokens from `EditLeadDialogComponent`

#### Component: LeadListComponent (modify existing)
- [ ] Add "Convert to Client" button in the actions cell for rows where `lead.status === 'QUALIFIED'`
- [ ] Clicking opens `ConvertLeadDialogComponent` inline (same pattern as `EditLeadDialogComponent`)
- [ ] On `converted` event: reload page and navigate to `/clients/{clientId}`
- [ ] Remove Archive button visibility for `QUALIFIED` leads (see Story 2-2 edge case note)
- [ ] For `CONVERTED` rows: replace Archive button with "View client" link

#### Component: LeadDetailComponent (create or modify)
[ASSUMPTION: A `LeadDetailComponent` at route `/leads/:id` does not yet exist; only the list and dialogs are implemented. This story requires it to be created as a minimal read-only detail page or the dialog must be enhanced. The simpler path is to add a "Convert to Client" button on the edit dialog for `QUALIFIED` leads and remove it for others.]

- [ ] Add "Convert to Client" button to `EditLeadDialogComponent` when `lead.status === 'QUALIFIED'` (renders `ConvertLeadDialogComponent` on click)
- [ ] For `CONVERTED` leads in edit dialog: show read-only notice "Converted — [view client record]" linking to `/clients/{convertedClientId}`

#### Routing
- [ ] Add `/clients/:id` route stub in `app.routes.ts` pointing to a minimal `ClientDetailComponent`
- [ ] `ClientDetailComponent` calls `GET /api/v1/clients/{id}` and renders the client's `fullName` and pre-conversion history section

#### Tests
- [ ] Unit test: `ConvertLeadDialogComponent` pre-populates fields from lead input
- [ ] Unit test: submit calls `leadService.convertLead()` with correct payload
- [ ] Unit test: 409 response surfaces `LEAD_ALREADY_CONVERTED` error with client link
- [ ] Unit test: 422 response surfaces status guard message
- [ ] Unit test: cancel emits `cancelled` without API call
- [ ] Unit test: `LeadListComponent` shows Convert button only for QUALIFIED rows

---

## 9. Out of Scope

The following are explicitly excluded from this ticket to prevent scope creep:

| Item | Reason |
|---|---|
| Automatically scheduling a first appointment at conversion | Scheduling module not yet built |
| Merging two lead records before conversion | Separate duplicate-management feature |
| Automatic email/push notification to therapist or supervisor on conversion | Notification infrastructure and preferences not yet defined |
| Bi-directional navigation from the client record back to the lead record (deep link in client UI) | Client UI is not built yet; a `sourceLeadId` field is persisted for future use |
| Editing the client record after creation (client profile CRUD) | Separate epic |
| Undoing or reversing a conversion (de-converting) | `CONVERTED` is a terminal FSM state; reversal is out of scope for MVP |
| Converting a lead to an existing client record (merge paths) | Out of scope per original ticket |
| Bulk conversion of multiple leads in one action | Out of scope for MVP |
| Consent form collection at conversion time | Separate consent management feature |
| CSV export of converted leads | Separate reporting feature |

---

## 10. Assumptions

| # | Assumption | Impact if Wrong |
|---|---|---|
| A1 | A `LeadDetailComponent` (route `/leads/:id`) does not yet exist; the "Convert to Client" entry point will be added to `EditLeadDialogComponent` as an additional action | If a detail page already exists or is added in parallel, the Convert button placement should move there |
| A2 | The `Client` entity requires only the fields listed in the field-mapping table for this MVP; additional client fields (date of birth, address, consent, insurance) are added in a later sprint | If the client intake form requires more mandatory fields, the `ConvertLeadRequest` must be extended |
| A3 | The `ConversionErrorResponse` extends or mirrors the existing `ErrorResponse` shape with an added `existingClientId` field; this does not break existing error-handling code | If `ErrorResponse` is used in typed deserialization on the frontend, all callers must be updated |
| A4 | `currentPrincipalId()` is available in the Angular frontend via an auth service or NgRx store for defaulting `ownerId` in the dialog | If not available, the dialog must fetch the current user profile, adding a network call |
| A5 | The notes prefix format `"[Transferred from lead on <ISO date>]\n"` is acceptable; no richer formatting is required | If the clinical team requires a different format, the `convertLead` service method must be updated |
| A6 | Archiving a `QUALIFIED` lead (FSM-valid: `QUALIFIED → INACTIVE`) is blocked in the UI only, not at the API level; a future business rule may change this | If archive must be blocked at the API level, a new guard must be added to `archiveLead()` |

---

## 11. Open Questions (Remaining — Require PO Confirmation Before Sprint Start)

| # | Question | Stakeholder | Impact if Unresolved | Proposed Default |
|---|---|---|---|---|
| OQ-5 | Should the "Convert to Client" button also appear in the lead list row (table view), or only in the detail/edit dialog? | Product Owner / UX | Determines `LeadListComponent` changes scope | Include in list row for QUALIFIED leads (as specced above) |
| OQ-6 | Should archiving a `QUALIFIED` lead be blocked at the API level (HTTP 422), or only suppressed in the UI? | Clinical lead / Product Owner | If a therapist calls the archive API directly, a QUALIFIED lead could be lost | UI-only for MVP; add API guard in a follow-up |
| OQ-7 | What is the exact format and content of the "Pre-conversion history" section heading in the client timeline? Should it include the original lead ID or lead name? | Clinical lead / UX | UI rendering of timeline | Show "Pre-conversion history (Lead: {fullName})" |
| OQ-8 | Is there a maximum notes length on the `Client` entity, and must it match the `TEXT` (unlimited) column used on `Lead.notes`? | Developer / Product Owner | Schema constraint; potential truncation risk | Use `TEXT` (unlimited) consistent with leads |
| OQ-9 | Should the `SYSTEM_ADMINISTRATOR` role be able to convert leads in addition to `RECEPTION_ADMIN_STAFF`? | System Administrator | `SYSTEM_ADMINISTRATOR` already has `MANAGE_LEADS` per `RolePermissions.java`; this is already resolved | Yes — both roles can convert |

> OQ-9 is already resolved by the existing `RolePermissions` matrix and is included here for documentation clarity only.

---

## 12. Definition of Done

- [ ] `POST /api/v1/leads/{id}/convert` is implemented and returns 201 with `{ clientId, leadId }`
- [ ] Entire conversion is wrapped in a single `@Transactional` block; partial failure rolls back all changes
- [ ] `V7__clients.sql` migration runs cleanly on a clean schema and on an existing schema with data
- [ ] All field mappings from OQ-2 are implemented and covered by unit tests asserting each field individually
- [ ] `LEAD_CONVERTED` audit log entry is written with `actorId`, `leadId`, and `clientId`
- [ ] Status guard (`QUALIFIED` only) returns HTTP 422 with `INVALID_STATUS_TRANSITION`
- [ ] Duplicate guard (already-converted) returns HTTP 409 with `LEAD_ALREADY_CONVERTED` and `existingClientId`
- [ ] Bidirectional link (`lead.convertedClientId` and `client.sourceLeadId`) is persisted and queryable
- [ ] Unique constraint on `clients.source_lead_id` prevents concurrent duplicate conversions
- [ ] Pre-conversion activity history is returned by `GET /clients/{id}/timeline`
- [ ] Frontend conversion dialog opens pre-populated, validates required fields, shows loading state, and handles all three error codes
- [ ] "Convert to Client" button shown only for `QUALIFIED` leads; replaced by client link for `CONVERTED` leads
- [ ] User is redirected to `/clients/{clientId}` on successful conversion
- [ ] All backend unit and controller tests pass (see checklist above)
- [ ] All frontend unit tests pass
- [ ] Code reviewed and approved by at least one peer
- [ ] Product Owner has confirmed OQ-5 through OQ-8 before sprint start
