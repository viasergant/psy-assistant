---
goal: Track client advance payment as a deposit payment on the invoice, not a discount
version: 1.0
date_created: 2026-05-29
last_updated: 2026-05-29
owner: Serhiy_Piddubchak
status: 'Planned'
tags: [feature, billing, refactor]
---

# Introduction

![Status: Planned](https://img.shields.io/badge/status-Planned-blue)

When a client pays money before a session (a deposit/advance), the system currently records it as a **discount** on the auto-generated invoice, reducing the invoice total. This distorts revenue statistics: a 100 UAH session with a 30 UAH deposit shows as a 70 UAH invoice instead of showing full 100 UAH earned with 30 UAH already received.

This plan renames the field from `manualDiscount` → `depositAmount` across the full stack and changes the billing logic so the advance payment is recorded as a `Payment` record against the invoice rather than as a discount deduction. The invoice total reflects full revenue; `paidAmount` reflects money already received; `outstandingBalance` reflects what the client still owes.

---

## 1. Requirements & Constraints

- **REQ-001**: When a session invoice is auto-generated and `depositAmount > 0`, the invoice `discount` field must remain `0`. The full service price must appear as `total`.
- **REQ-002**: A `Payment` record must be created alongside the invoice with `amount = depositAmount`, `paymentMethod = CASH`, `paymentDate = event.sessionDate()`, `notes = "Advance payment received at booking"`, `createdBy = "system"`.
- **REQ-003**: The `Invoice.paidAmount` must be updated to reflect the deposit, and `Invoice.status` must remain `DRAFT` (the deposit does NOT trigger a status transition away from DRAFT).
- **REQ-004**: The `Invoice` domain model must expose an `applyDeposit(BigDecimal)` method that updates `paidAmount` without changing `status` — distinct from `applyPayment()` which enforces ISSUED/OVERDUE state and transitions status.
- **REQ-005**: All occurrences of the field name `manualDiscount` in backend Java code, frontend TypeScript, and i18n JSON files must be renamed to `depositAmount`.
- **REQ-006**: A Flyway migration must rename the DB columns `manual_discount` → `deposit_amount` in both the `appointment` and `session_record` tables.
- **CON-001**: `Invoice.applyPayment()` (used for user-triggered payments on ISSUED/OVERDUE invoices) must remain unchanged — its state guard and status transition logic is not affected.
- **CON-002**: `InvoiceService.handleSessionCompleted()` is the only place where the discount was applied automatically; no other call site applies `manualDiscount` to billing.
- **CON-003**: Flyway migrations must be sequential and non-destructive. The rename is `ALTER TABLE ... RENAME COLUMN`, not a drop/add (data preservation required).
- **GUD-001**: `InvoiceService` must inject `PaymentRepository` (not `PaymentService`) to avoid circular dependency concerns and keep the deposit registration a low-level atomic operation inside the same transaction.
- **GUD-002**: Frontend i18n hint text must be updated to accurately say the amount will appear as a pre-payment on the invoice, not as a deduction from the total.

---

## 2. Implementation Steps

### Implementation Phase 1 — Database migration

- GOAL-001: Rename `manual_discount` → `deposit_amount` columns in DB with a non-destructive Flyway migration.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-001 | Create `backend/src/main/resources/db/migration/V75__rename_manual_discount_to_deposit_amount.sql` with: `ALTER TABLE appointment RENAME COLUMN manual_discount TO deposit_amount;` and `ALTER TABLE session_record RENAME COLUMN manual_discount TO deposit_amount;` | | |

### Implementation Phase 2 — Backend Java rename (field + event)

- GOAL-002: Rename `manualDiscount` → `depositAmount` across all backend Java classes. These tasks are independent and can be executed in parallel.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-002 | `scheduling/domain/Appointment.java` line 163–164: rename `@Column(name = "manual_discount")` → `deposit_amount`, field `manualDiscount` → `depositAmount`, getter `getManualDiscount()` → `getDepositAmount()`, setter `setManualDiscount()` → `setDepositAmount()` | | |
| TASK-003 | `scheduling/dto/CreateAppointmentRequest.java` line 46: rename record component `manualDiscount` → `depositAmount`; update Javadoc comment to say "advance payment" not "prepaid amount" | | |
| TASK-004 | `scheduling/service/AppointmentService.java` lines 109, 143–144: rename param `manualDiscount` → `depositAmount`; update calls `appointment.setManualDiscount()` → `appointment.setDepositAmount()` | | |
| TASK-005 | `scheduling/rest/AppointmentController.java` line 143: update call from `request.manualDiscount()` → `request.depositAmount()` | | |
| TASK-006 | `sessions/domain/SessionRecord.java` lines 155–156, 370–375: rename `@Column(name = "manual_discount")` → `deposit_amount`, field + getter + setter (`manualDiscount` → `depositAmount`) | | |
| TASK-007 | `sessions/service/SessionRecordService.java` lines 139, 215, 327: rename all calls `setManualDiscount` → `setDepositAmount` and `getManualDiscount` → `getDepositAmount` | | |
| TASK-008 | `sessions/event/SessionCompletedEvent.java` line 22: rename record component `manualDiscount` → `depositAmount`; update Javadoc comment to "advance payment" | | |

### Implementation Phase 3 — Billing logic change

- GOAL-003: Replace the discount application with deposit payment registration in `InvoiceService`.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-009 | `billing/invoice/Invoice.java`: add method `applyDeposit(BigDecimal depositAmount)` after `applyPayment()`. Method validates `depositAmount` does not exceed `total`, increments `paidAmount`, does NOT change `status`. Throws `PaymentValidationException` if amount exceeds outstanding. | | |
| TASK-010 | `billing/invoice/InvoiceService.java`: inject `PaymentRepository` via constructor (add field + constructor param). | | |
| TASK-011 | `billing/invoice/InvoiceService.java` — `handleSessionCompleted()` lines 142–146: remove `invoice.setDiscount(event.manualDiscount())` block. After `invoiceRepository.save(invoice)`, add: if `event.depositAmount() != null && > 0`, call `saved.applyDeposit(event.depositAmount())`, save a `new Payment(saved.getId(), event.depositAmount(), PaymentMethod.CASH, event.sessionDate(), null, "Advance payment received at booking", "system")` via `paymentRepository.save()`, then `invoiceRepository.save(saved)`. Update log message from "manual prepaid discount" to "deposit payment". | | |

### Implementation Phase 4 — Frontend rename and copy update

- GOAL-004: Rename `manualDiscount` → `depositAmount` in frontend TypeScript and update i18n hint text. Tasks in this phase are independent.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-012 | `frontend/src/app/features/schedule/models/schedule.model.ts` line 287: rename `manualDiscount?: number` → `depositAmount?: number` | | |
| TASK-013 | `frontend/src/app/features/schedule/components/appointment-booking-dialog/appointment-booking-dialog.component.ts` lines 174–191, 1141, 1551: rename all occurrences of `manualDiscount` (form control name, id, formControlName, isInvalid check, Reactive Forms declaration, form value read) to `depositAmount`; rename all i18n keys from `manualDiscountLabel/Hint/Placeholder/Invalid` to `depositAmountLabel/Hint/Placeholder/Invalid` | | |
| TASK-014 | `frontend/src/assets/i18n/en.json` lines 599–603: rename keys `manualDiscountLabel` → `depositAmountLabel`, `manualDiscountPlaceholder` → `depositAmountPlaceholder`, `manualDiscountHint` → `depositAmountHint`, `manualDiscountInvalid` → `depositAmountInvalid`; update hint value from "Amount already paid by client — will be deducted from the invoice total." → "Amount already paid by client — will be recorded as a pre-payment on the invoice." | | |
| TASK-015 | `frontend/src/assets/i18n/uk.json` lines 599–603: same key renames as TASK-014; update hint value from "Сума, вже сплачена клієнтом — буде відрахована від загальної суми рахунку." → "Сума, вже сплачена клієнтом — буде зарахована як передоплата у рахунку." | | |

### Implementation Phase 5 — Tests

- GOAL-005: Add unit test coverage for the new deposit payment behaviour; update existing tests to use renamed field.

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-016 | `billing/invoice/InvoiceServiceTest.java`: inject mock `PaymentRepository`; add it to `InvoiceService` constructor call in `setUp()`. | | |
| TASK-017 | `billing/invoice/InvoiceServiceTest.java`: add test `handleSessionCompleted_withDeposit_registersPaymentNotDiscount()` — set up event with `depositAmount = 30`, unit price = 100; assert invoice `discount == 0`, `total == 100`, `paidAmount == 30`; verify `paymentRepository.save()` called once with amount 30, method CASH, notes containing "Advance". | | |
| TASK-018 | `billing/invoice/InvoiceServiceTest.java`: add test `handleSessionCompleted_withZeroDeposit_noPaymentRegistered()` — event with `depositAmount = null`; verify `paymentRepository.save()` never called, invoice `discount == 0`, `paidAmount == 0`. | | |
| TASK-019 | `billing/invoice/InvoiceServiceTest.java`: if an existing test for `handleSessionCompleted` with `manualDiscount` asserting `discount > 0` exists, update it to assert `discount == 0` and a Payment was registered instead. | | |

---

## 4. Dependencies

- **DEP-001**: Flyway `V75` migration must run before any JPA entity reads the renamed column (`deposit_amount`). No code in Phase 2–4 can be deployed before this migration runs.
- **DEP-002**: `PaymentRepository` is already an existing Spring Data repository (`billing/payment/PaymentRepository.java`). No new repository needs to be created.
- **DEP-003**: `Payment` constructor signature is `(invoiceId, amount, paymentMethod, paymentDate, reference, notes, createdBy)` — all positional, no builder. Callers must pass `null` for `reference` when not applicable.

---

## 5. Files

- **FILE-001**: `backend/src/main/resources/db/migration/V75__rename_manual_discount_to_deposit_amount.sql` — NEW migration
- **FILE-002**: `backend/src/main/java/com/psyassistant/scheduling/domain/Appointment.java` — field rename + column name update
- **FILE-003**: `backend/src/main/java/com/psyassistant/scheduling/dto/CreateAppointmentRequest.java` — record component rename
- **FILE-004**: `backend/src/main/java/com/psyassistant/scheduling/service/AppointmentService.java` — param + call site rename
- **FILE-005**: `backend/src/main/java/com/psyassistant/scheduling/rest/AppointmentController.java` — call site rename
- **FILE-006**: `backend/src/main/java/com/psyassistant/sessions/domain/SessionRecord.java` — field rename + column name update
- **FILE-007**: `backend/src/main/java/com/psyassistant/sessions/service/SessionRecordService.java` — call site renames
- **FILE-008**: `backend/src/main/java/com/psyassistant/sessions/event/SessionCompletedEvent.java` — record component rename
- **FILE-009**: `backend/src/main/java/com/psyassistant/billing/invoice/Invoice.java` — new `applyDeposit()` method
- **FILE-010**: `backend/src/main/java/com/psyassistant/billing/invoice/InvoiceService.java` — inject `PaymentRepository`, replace discount logic with deposit payment registration
- **FILE-011**: `backend/src/test/java/com/psyassistant/billing/invoice/InvoiceServiceTest.java` — new deposit tests + mock wiring
- **FILE-012**: `frontend/src/app/features/schedule/models/schedule.model.ts` — field rename
- **FILE-013**: `frontend/src/app/features/schedule/components/appointment-booking-dialog/appointment-booking-dialog.component.ts` — form control rename + i18n key rename
- **FILE-014**: `frontend/src/assets/i18n/en.json` — key renames + hint text update
- **FILE-015**: `frontend/src/assets/i18n/uk.json` — key renames + hint text update (Ukrainian)

---

## 6. Testing

- **TEST-001**: Unit — `handleSessionCompleted_withDeposit_registersPaymentNotDiscount()`: deposit creates `Payment`, invoice `discount = 0`, `paidAmount = depositAmount`, `total = fullPrice`, `status = DRAFT`.
- **TEST-002**: Unit — `handleSessionCompleted_withZeroDeposit_noPaymentRegistered()`: no deposit → `paymentRepository.save()` never called, `paidAmount = 0`.
- **TEST-003**: Unit — `Invoice.applyDeposit()` happy path: `paidAmount` increases, `status` stays `DRAFT`.
- **TEST-004**: Unit — `Invoice.applyDeposit()` exceeds total: throws `PaymentValidationException`.
- **TEST-005**: Manual smoke — create an appointment with a deposit amount, complete the session, open the auto-generated invoice; verify `total = full price`, `paidAmount = deposit`, `outstandingBalance = total - deposit`, one payment record visible in payment history.
- **TEST-006**: Manual regression — create an appointment without a deposit, complete the session; invoice has `discount = 0`, `paidAmount = 0`, no payment records auto-created.

---

## 7. Risks & Assumptions

- **RISK-001**: Existing appointments in the DB with `manual_discount > 0` that have already completed sessions will have generated invoices with that discount already applied. The migration renames the column but does NOT retroactively fix those invoices. Existing DRAFT invoices with `discount > 0` from the old behaviour will need to be identified and manually corrected (set `discount = 0`, add a corresponding `Payment` record) or left as-is and documented as a known pre-migration artefact.
- **RISK-002**: If a test environment has no existing `manual_discount` data, RISK-001 is moot. Confirm with the user whether any production data needs a backfill.
- **ASSUMPTION-001**: `PaymentMethod.CASH` is an acceptable default for auto-registered deposits. The actual method used by the client at booking time is not currently captured in the system. A future enhancement could add a `depositPaymentMethod` field to the appointment.
- **ASSUMPTION-002**: The deposit `paymentDate` is set to `event.sessionDate()` (the date the session was completed, which triggers invoice generation) — not the original booking date. The booking date is not in `SessionCompletedEvent`. This is acceptable for the first iteration.
- **ASSUMPTION-003**: Frontend form control and API field name change from `manualDiscount` → `depositAmount` is a breaking API change. This is acceptable because the API is internal (no external consumers).

---

## 8. Related Specifications / Further Reading

- V74 migration: `backend/src/main/resources/db/migration/V74__add_manual_discount_to_appointments.sql` — original column that this plan renames
- `billing/wallet/ClientWallet.java` — future Option B (wallet-based deposits) builds on this entity
- `billing/payment/Payment.java` — immutable ledger entry; constructor signature required for TASK-011
- `billing/invoice/Invoice.java` lines 182–192 — `applyPayment()` is the reference implementation for `applyDeposit()` (same pattern, minus state guard and status transition)
