# E2E Test Flows

End-to-end test cases derived from [Features.md](Features.md). Each case covers one user-observable behaviour from start to finish via the browser UI.

**ID scheme:** `TC-<AREA>-<NNN>`  
**Mode column:** `SOLO` | `CLINIC` | `BOTH`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Leads](#2-leads)
3. [Clients](#3-clients)
4. [Care Plans](#4-care-plans)
5. [Schedule](#5-schedule)
6. [Sessions](#6-sessions)
7. [Billing](#7-billing)
8. [Reports](#8-reports)
9. [Admin — Users](#9-admin--users)
10. [Admin — Therapists](#10-admin--therapists)
11. [Admin — Leave Requests](#11-admin--leave-requests)
12. [Admin — Notification Templates](#12-admin--notification-templates)
13. [Admin — Risk Flag Types](#13-admin--risk-flag-types)
14. [Global Admin](#14-global-admin)
15. [Therapist Profile Setup](#15-therapist-profile-setup)
16. [Settings](#16-settings)
17. [Navigation & Shell](#17-navigation--shell)
18. [Role-Based Access Control](#18-role-based-access-control)
19. [Mode-Based Access Control](#19-mode-based-access-control)

---

## 1. Authentication

### TC-AUTH-001 — Successful login
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | Any |
| Preconditions | Valid account exists |

**Steps:**
1. Navigate to `/auth/login`.
2. Enter valid username and password.
3. Click **Login**.

**Expected:** User is redirected to the first route their role permits; sidebar is visible.

---

### TC-AUTH-002 — Failed login with wrong password
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | Any |
| Preconditions | Valid account exists |

**Steps:**
1. Navigate to `/auth/login`.
2. Enter valid username and an incorrect password.
3. Click **Login**.

**Expected:** An error message is shown; the user remains on the login page.

---

### TC-AUTH-003 — Redirect to login when unauthenticated
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | — |
| Preconditions | No active session |

**Steps:**
1. Without logging in, navigate directly to `/clients`.

**Expected:** Browser redirects to `/auth/login?returnUrl=%2Fclients`.

---

### TC-AUTH-004 — Return URL is honoured after login
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | No active session |

**Steps:**
1. Navigate directly to `/sessions`.
2. Complete login on the redirected page.

**Expected:** After login, the browser lands on `/sessions`, not the default home route.

---

### TC-AUTH-005 — Forced password change on first login
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | Any |
| Preconditions | Account has `mustChangePassword: true` in JWT |

**Steps:**
1. Log in with a first-login account.

**Expected:** App redirects to `/auth/first-login-password-change`; the user cannot access any other page until password is changed.

---

### TC-AUTH-006 — Successful first-login password change
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | Any |
| Preconditions | User is on `/auth/first-login-password-change` |

**Steps:**
1. Enter a new password meeting complexity requirements.
2. Confirm the new password.
3. Submit the form.

**Expected:** Password is updated; user is redirected to their home route.

---

### TC-AUTH-007 — Logout clears session
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | Any |
| Preconditions | User is logged in |

**Steps:**
1. Click the logout control in the shell.
2. After logout, attempt to navigate to `/clients`.

**Expected:** User is redirected to `/auth/login`; no previous session data is accessible.

---

## 2. Leads

### TC-LEADS-001 — View lead list
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | At least one lead exists |

**Steps:**
1. Log in as RECEPTION_ADMIN_STAFF.
2. Click **Leads** in the sidebar.

**Expected:** Lead list is displayed with name, status, and contact columns.

---

### TC-LEADS-002 — Create a new lead
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | User is on `/leads` |

**Steps:**
1. Click **New Lead** / create button.
2. Fill in first name, last name, phone, and referral source.
3. Submit the dialog.

**Expected:** New lead appears at the top of the list; a success toast is shown.

---

### TC-LEADS-003 — Edit an existing lead
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | At least one lead exists |

**Steps:**
1. Open the edit dialog for a lead.
2. Change the phone number.
3. Save.

**Expected:** Updated phone number is visible in the list row; no page reload required.

---

### TC-LEADS-004 — Convert lead to client
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | A lead in eligible status exists |

**Steps:**
1. Open the convert dialog for a lead.
2. Confirm conversion.

**Expected:** Lead status changes to Converted; a corresponding client record appears in `/clients`.

---

## 3. Clients

### TC-CLIENT-001 — View client list
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | At least one active client exists |

**Steps:**
1. Log in as THERAPIST.
2. Click **Clients** in the sidebar.

**Expected:** Client list is displayed with name, status, and therapist columns.

---

### TC-CLIENT-002 — Open client detail page
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A client record exists |

**Steps:**
1. Click on a client name in the list.

**Expected:** Client detail page loads at `/clients/:id` showing demographics, contact info, and panels.

---

### TC-CLIENT-003 — View client activity timeline
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | Client has at least one session or note |

**Steps:**
1. Open a client detail page.
2. Navigate to the Timeline section.

**Expected:** Timeline displays events in reverse-chronological order.

---

### TC-CLIENT-004 — Add a risk flag
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | Client detail page is open |

**Steps:**
1. Open the Risk Flags panel.
2. Click **Add Flag**.
3. Select a risk flag type, enter severity notes, and save.

**Expected:** New flag appears in the risk flags panel with correct type and status Active.

---

### TC-CLIENT-005 — Resolve a risk flag
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | Client has at least one active risk flag |

**Steps:**
1. Open the Risk Flags panel.
2. Click **Resolve** on an active flag.
3. Enter a resolution note and confirm.

**Expected:** Flag status changes to Resolved; it moves out of the active flags section.

---

### TC-CLIENT-006 — View client packages
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A package has been sold to the client |

**Steps:**
1. Open a client detail page.
2. Navigate to the Packages section.

**Expected:** Purchased packages are listed with remaining session count and expiry.

---

## 4. Care Plans

### TC-CAREPLAN-001 — View care plans for a client
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | Client has at least one care plan |

**Steps:**
1. Open a client detail page.
2. Navigate to `/clients/:id/care-plans`.

**Expected:** Care plan list is displayed with plan name, status, and date range.

---

### TC-CAREPLAN-002 — Create a care plan
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | Client detail is open |

**Steps:**
1. Click **New Care Plan**.
2. Enter plan name, start date, and goals.
3. Save.

**Expected:** New plan appears in the list with status Active.

---

### TC-CAREPLAN-003 — View care plan detail with goals and outcomes
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A care plan with at least one goal exists |

**Steps:**
1. Click a care plan in the list.

**Expected:** Detail page shows goal progress panel and outcome measures section.

---

### TC-CAREPLAN-004 — Add outcome measure entry
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A care plan with an outcome measure configured exists |

**Steps:**
1. Open a care plan detail.
2. Click **Add Entry** in the outcome measures section.
3. Enter a numeric score and date.
4. Save.

**Expected:** New entry is reflected in the list and the trend chart updates.

---

### TC-CAREPLAN-005 — Outcome measure chart displays trend
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A care plan has ≥ 2 outcome measure entries |

**Steps:**
1. Open the care plan detail.
2. View the outcome measure chart.

**Expected:** Chart shows a line/bar graph across the recorded dates with correct values.

---

### TC-CAREPLAN-006 — Goal progress panel reflects updates
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | Care plan has at least one goal with a progress record |

**Steps:**
1. Open a care plan detail.
2. View the Goal Progress panel.

**Expected:** Each goal shows current progress percentage and last-updated date.

---

## 5. Schedule

### TC-SCHED-001 — View calendar in week view
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | User is logged in |

**Steps:**
1. Click **Schedule** in the sidebar.

**Expected:** Calendar loads in the default week view showing current week; existing appointments are rendered as blocks.

---

### TC-SCHED-002 — Switch between calendar views
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | User is on `/schedule` |

**Steps:**
1. Click the **Day** view toggle.
2. Click the **Month** view toggle.
3. Click **Week** to return.

**Expected:** Calendar re-renders for each view without errors; appointment blocks are visible in day and week views.

---

### TC-SCHED-003 — Book a single appointment
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | At least one client and one therapist exist |

**Steps:**
1. Click a free time slot or the **Book** button.
2. Select client, therapist, service, date, and time.
3. Confirm booking.

**Expected:** Appointment block appears on the calendar at the selected slot; success toast shown.

---

### TC-SCHED-004 — Book a recurring appointment series
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | At least one client and therapist exist |

**Steps:**
1. Open the booking dialog.
2. Enable recurrence (e.g. weekly, 8 occurrences).
3. Confirm booking.

**Expected:** Multiple appointment blocks appear on the calendar across subsequent weeks.

---

### TC-SCHED-005 — Recurring series with conflicts triggers review dialog
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | A recurring slot overlaps an existing appointment |

**Steps:**
1. Book a recurring series that conflicts with an existing appointment.

**Expected:** Conflict review dialog appears listing the conflicting occurrences; user can skip or reschedule individual slots.

---

### TC-SCHED-006 — Edit an appointment
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A future appointment exists |

**Steps:**
1. Click on an appointment block.
2. Select **Edit**.
3. Change the service and save.

**Expected:** Appointment block reflects the updated service; no duplicate is created.

---

### TC-SCHED-007 — Reschedule an appointment
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | A future appointment exists |

**Steps:**
1. Open an appointment block.
2. Select **Reschedule**.
3. Pick a new date and time and confirm.

**Expected:** Appointment moves to the new slot on the calendar; original slot is free.

---

### TC-SCHED-008 — Cancel an appointment
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A future appointment exists |

**Steps:**
1. Open an appointment block.
2. Select **Cancel** and confirm.

**Expected:** Appointment block is removed from the calendar.

---

### TC-SCHED-009 — Submit a leave request
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | THERAPIST |
| Preconditions | User is on `/schedule` |

**Steps:**
1. Click the **Leave Request** button.
2. Select start date, end date, and reason.
3. Submit.

**Expected:** Success toast shown; leave request appears in the Admin leave-request list as Pending.

---

### TC-SCHED-010 — Schedule configuration panel saves working hours
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | User is on `/schedule` |

**Steps:**
1. Open the Schedule Config panel.
2. Change working hours for Monday.
3. Save.

**Expected:** Calendar reflects the new working-hours boundary on the next reload.

---

## 6. Sessions

### TC-SESSION-001 — View session list
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | At least one session exists |

**Steps:**
1. Click **Sessions** in the sidebar.

**Expected:** Session list loads with columns for client, therapist, date, and status.

---

### TC-SESSION-002 — Open session detail dialog
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | At least one session exists in the list |

**Steps:**
1. Click on a session row.

**Expected:** Session detail dialog opens showing client name, service, date, duration, and status.

---

### TC-SESSION-003 — Complete a session
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A session in Scheduled or In-Progress status exists |

**Steps:**
1. Open the session detail dialog.
2. Click **Complete**.
3. Confirm.

**Expected:** Session status changes to Completed; row updates in the list.

---

### TC-SESSION-004 — Cancel a session
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A future session exists |

**Steps:**
1. Open the session detail dialog.
2. Click **Cancel** and select a cancellation reason.
3. Confirm.

**Expected:** Session status changes to Cancelled; cancellation reason is saved.

---

### TC-SESSION-005 — Record attendance for a session
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A session exists |

**Steps:**
1. Open the session detail dialog.
2. Click **Record Attendance**.
3. Select Attended or No-Show and save.

**Expected:** Attendance status is saved and visible in the detail dialog.

---

### TC-SESSION-006 — Add a session note
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A completed session exists |

**Steps:**
1. Open a session detail.
2. Navigate to the Notes panel.
3. Click **New Note**, type content, and save.

**Expected:** Note is saved and listed in the notes panel; editor closes.

---

### TC-SESSION-007 — Edit a session note
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A session with an existing note exists |

**Steps:**
1. Open the note editor for an existing note.
2. Change the note text and save.

**Expected:** Updated content is shown; a new version entry appears in the version history.

---

### TC-SESSION-008 — View note version history
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A note with at least 2 versions exists |

**Steps:**
1. Open the note version history dialog.

**Expected:** All previous versions are listed with timestamps; selecting a version shows its content.

---

### TC-SESSION-009 — Group session attendance panel
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | A group session with multiple participants exists |

**Steps:**
1. Open a group session detail.
2. Navigate to the Group Attendance panel.

**Expected:** Each participant is listed; attendance can be recorded individually.

---

## 7. Billing

### TC-BILLING-001 — View finance dashboard
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | At least one paid invoice exists |

**Steps:**
1. Log in as FINANCE.
2. Click **Billing** → **Dashboard** tab.

**Expected:** Dashboard shows revenue KPIs and charts for the current period.

---

### TC-BILLING-002 — View invoice list
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | At least one invoice exists |

**Steps:**
1. Navigate to **Billing** → **Invoices**.

**Expected:** Invoice list shows invoice number, client, amount, status, and date columns.

---

### TC-BILLING-003 — Create a new invoice
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | A client and at least one service catalog item exist |

**Steps:**
1. Click **New Invoice**.
2. Select client, session / service, and therapist.
3. Save the invoice.

**Expected:** New invoice appears in the list with status Draft or Issued.

---

### TC-BILLING-004 — View invoice detail
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | An invoice exists |

**Steps:**
1. Click an invoice row.

**Expected:** Detail page shows line items, amounts, status, and payment history panel.

---

### TC-BILLING-005 — Register payment
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | An invoice with outstanding balance exists |

**Steps:**
1. Open an invoice detail.
2. Click **Register Payment**.
3. Enter amount and payment method.
4. Confirm.

**Expected:** Invoice status updates to Paid (or Partial); payment appears in payment history.

---

### TC-BILLING-006 — Register refund
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | An invoice that has been paid exists |

**Steps:**
1. Open an invoice detail.
2. Click **Register Refund**, enter amount, and confirm.

**Expected:** Refund entry appears in payment history; balance is adjusted accordingly.

---

### TC-BILLING-007 — Update line-item price
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | An invoice in editable status exists |

**Steps:**
1. Open an invoice detail.
2. Click **Update Price** on a line item.
3. Enter new price and confirm.

**Expected:** Line-item price is updated; invoice total recalculates.

---

### TC-BILLING-008 — Override therapist on invoice
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | An invoice exists with an assigned therapist |

**Steps:**
1. Open an invoice detail.
2. Click **Override Therapist**, select a different therapist, and save.

**Expected:** Invoice reflects the new therapist; audit trail or tooltip notes the override.

---

### TC-BILLING-009 — View service catalog
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | At least one catalog item exists |

**Steps:**
1. Navigate to **Billing** → **Catalog**.

**Expected:** Service list shows name, duration, and price columns.

---

### TC-BILLING-010 — Create a service catalog item
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | User is on the Catalog page |

**Steps:**
1. Click **New Service**.
2. Enter name, duration (minutes), and price.
3. Save.

**Expected:** New service appears in the catalog list.

---

### TC-BILLING-011 — Create a package definition
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | At least one service catalog item exists |

**Steps:**
1. Navigate to **Billing** → **Packages**.
2. Click **New Package**, enter name, included sessions count, and total price.
3. Save.

**Expected:** Package definition appears in the list.

---

### TC-BILLING-012 — Sell a package to a client
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | A client and a package definition exist |

**Steps:**
1. Open **Sell Package** dialog (from invoice or packages page).
2. Select client and package.
3. Confirm.

**Expected:** Package appears in the client's packages panel with the correct session balance.

---

### TC-BILLING-013 — Create a discount rule
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | User is on the Discounts page |

**Steps:**
1. Click **New Discount Rule**.
2. Enter code, discount type (percentage or fixed), and value.
3. Save.

**Expected:** New discount rule appears in the list.

---

## 8. Reports

### TC-REPORT-001 — Caseload overview (CLINIC only)
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | Multiple therapists have active clients |

**Steps:**
1. Navigate to **Reports** → **Caseload**.

**Expected:** Table shows each therapist with their active client count; totals are correct.

---

### TC-REPORT-002 — Caseload drilldown
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | At least one therapist with clients exists |

**Steps:**
1. Click on a therapist row in the Caseload Overview.

**Expected:** Drilldown view shows the therapist's client list with status and last-session date.

---

### TC-REPORT-003 — Lead conversion report
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SUPERVISOR |
| Preconditions | Leads in various statuses exist |

**Steps:**
1. Navigate to **Reports** → **Lead Conversion**.
2. Apply a date range filter.

**Expected:** Report shows conversion rate percentage and funnel counts for the selected period.

---

### TC-REPORT-004 — Therapist utilisation report (CLINIC only)
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SUPERVISOR |
| Preconditions | Sessions are recorded for multiple therapists |

**Steps:**
1. Navigate to **Reports** → **Therapist Utilisation**.

**Expected:** Chart/table shows booked vs available hours per therapist for the period.

---

### TC-REPORT-005 — Revenue report
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | Paid invoices exist |

**Steps:**
1. Navigate to **Reports** → **Revenue**.
2. Apply a monthly date filter.

**Expected:** Revenue totals and breakdown by service or therapist are displayed.

---

### TC-REPORT-006 — Client retention report
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SUPERVISOR |
| Preconditions | Sessions across multiple months exist |

**Steps:**
1. Navigate to **Reports** → **Client Retention**.

**Expected:** Report shows retention rate and cohort data for the selected period.

---

### TC-REPORT-007 — No-show and cancellation report
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SUPERVISOR |
| Preconditions | Sessions with no-show or cancelled status exist |

**Steps:**
1. Navigate to **Reports** → **No-Show & Cancellation**.

**Expected:** Report shows counts and rates broken down by therapist or month.

---

### TC-REPORT-008 — Report filter restricts data
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SUPERVISOR |
| Preconditions | Data spanning multiple months exists |

**Steps:**
1. Open any report.
2. Set the date range to a single week.

**Expected:** Report data reflects only the selected range; totals change accordingly.

---

### TC-REPORT-009 — Export report
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | Report has data |

**Steps:**
1. Open a report with data.
2. Click the export button and select CSV (or PDF if available).

**Expected:** A file download is triggered containing the report data.

---

## 9. Admin — Users

### TC-ADMIN-USR-001 — View user list (CLINIC)
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | Multiple users exist |

**Steps:**
1. Navigate to **Admin** → **Users**.

**Expected:** User list shows username, roles, and status columns.

---

### TC-ADMIN-USR-002 — Create a user
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | User is on the Users admin page |

**Steps:**
1. Click **New User**.
2. Enter username, email, and assign a role.
3. Save.

**Expected:** New user appears in the list; a temporary password or invite mechanism is triggered.

---

### TC-ADMIN-USR-003 — Edit a user
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A non-admin user exists |

**Steps:**
1. Open the edit dialog for a user.
2. Change their role.
3. Save.

**Expected:** Role change is reflected in the user list immediately.

---

### TC-ADMIN-USR-004 — Reset user password
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A user exists |

**Steps:**
1. Open the Password Reset dialog for a user.
2. Enter and confirm a new password.
3. Save.

**Expected:** Success message shown; the user can log in with the new password.

---

## 10. Admin — Therapists

### TC-ADMIN-TH-001 — View therapist list (CLINIC)
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | At least one therapist exists |

**Steps:**
1. Navigate to **Admin** → **Therapists**.

**Expected:** Therapist list shows name, specialisation, and active status.

---

### TC-ADMIN-TH-002 — Create a therapist account
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | User is on the Therapists admin page |

**Steps:**
1. Click **New Therapist**.
2. Enter full name, email, and specialisations.
3. Save.

**Expected:** `TherapistAccountCreatedModal` appears with generated credentials; therapist appears in the list.

---

### TC-ADMIN-TH-003 — View therapist detail
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | At least one therapist exists |

**Steps:**
1. Click on a therapist in the list.

**Expected:** Detail page shows profile, specialisations, working hours, and assigned clients.

---

### TC-ADMIN-TH-004 — Edit therapist profile
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A therapist detail page is open |

**Steps:**
1. Click **Edit**.
2. Change a specialisation.
3. Save.

**Expected:** Updated specialisation is shown on the detail page.

---

## 11. Admin — Leave Requests

### TC-ADMIN-LV-001 — View pending leave requests (CLINIC)
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A therapist has submitted a leave request |

**Steps:**
1. Navigate to **Admin** → **Leave Requests**.

**Expected:** List shows pending requests with therapist name, dates, and reason.

---

### TC-ADMIN-LV-002 — Approve a leave request
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A pending leave request exists |

**Steps:**
1. Click **Approve** on a pending leave request.

**Expected:** Request status changes to Approved; the therapist's calendar blocks those days.

---

### TC-ADMIN-LV-003 — Reject a leave request
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A pending leave request exists |

**Steps:**
1. Click **Reject** on a pending leave request and optionally add a reason.

**Expected:** Request status changes to Rejected; the therapist's calendar remains unchanged.

---

## 12. Admin — Notification Templates

### TC-ADMIN-NT-001 — View notification template list
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | At least one template exists |

**Steps:**
1. Navigate to **Admin** → **Notification Templates**.

**Expected:** Template list shows name, type, and last-updated columns.

---

### TC-ADMIN-NT-002 — Create a notification template
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | User is on the Templates page |

**Steps:**
1. Click **New Template**.
2. Enter template name, subject, and body with a variable placeholder (e.g. `{{clientName}}`).
3. Save.

**Expected:** New template appears in the list.

---

### TC-ADMIN-NT-003 — Edit a notification template
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A template exists |

**Steps:**
1. Open the edit dialog.
2. Modify the body text.
3. Save.

**Expected:** Updated body is confirmed in the template preview.

---

### TC-ADMIN-NT-004 — Preview a notification template
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A template with variable placeholders exists |

**Steps:**
1. Open the preview dialog for a template.
2. Enter sample variable values.

**Expected:** Preview renders the template with variables replaced by sample values.

---

## 13. Admin — Risk Flag Types

### TC-ADMIN-RFT-001 — View risk flag type list
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | At least one type exists |

**Steps:**
1. Navigate to **Admin** → (Risk Flag Types tab or section).

**Expected:** List shows flag name, severity level, and active status.

---

### TC-ADMIN-RFT-002 — Create a risk flag type
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | User is on the Risk Flag Types page |

**Steps:**
1. Click **New Risk Flag Type**.
2. Enter name, severity, and description.
3. Save.

**Expected:** New type appears in the list and is available when adding a risk flag on a client.

---

### TC-ADMIN-RFT-003 — Edit a risk flag type
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | A risk flag type exists |

**Steps:**
1. Open the edit dialog.
2. Change the severity level.
3. Save.

**Expected:** Updated severity is shown in the list.

---

## 14. Global Admin

### TC-GADMIN-001 — View platform-wide risk flag types
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | GLOBAL_ADMIN |
| Preconditions | GLOBAL_ADMIN authority in JWT |

**Steps:**
1. Log in as a user with GLOBAL_ADMIN authority.
2. Click **Global Admin** in the sidebar.
3. Navigate to **Risk Flag Types**.

**Expected:** List shows platform-wide risk flag types across all organisations.

---

### TC-GADMIN-002 — Create a platform-wide risk flag type
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | GLOBAL_ADMIN |
| Preconditions | User is on the Global Admin Risk Flag Types page |

**Steps:**
1. Click **New Risk Flag Type**.
2. Enter name and severity.
3. Save.

**Expected:** New type appears in the platform-wide list and is available to all organisations.

---

## 15. Therapist Profile Setup

### TC-PROFILE-001 — Redirect to wizard on incomplete profile
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | THERAPIST account with incomplete profile exists |

**Steps:**
1. Log in as a THERAPIST whose profile completion status is INCOMPLETE.

**Expected:** `profileCompletionGuard` redirects to `/therapist/profile/complete`; navigation to any other page is blocked until wizard completes.

---

### TC-PROFILE-002 — Complete the profile wizard
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | User is on `/therapist/profile/complete` |

**Steps:**
1. Fill in all required wizard steps (personal info, specialisations, working hours).
2. Submit the final step.

**Expected:** Profile status changes to COMPLETE; user is redirected to their home route and can navigate freely.

---

### TC-PROFILE-003 — Completed profile does not redirect on login
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | THERAPIST with a completed profile |

**Steps:**
1. Log in.

**Expected:** User lands on their home route directly; no wizard redirect occurs.

---

## 16. Settings

### TC-SETTINGS-001 — Switch language to Ukrainian
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | User is logged in; app is in English |

**Steps:**
1. Click **Settings** in the sidebar.
2. Select **Ukrainian**.

**Expected:** UI strings switch to Ukrainian immediately without page reload; selection persists on next login.

---

### TC-SETTINGS-002 — Switch language back to English
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | UI is in Ukrainian |

**Steps:**
1. Open Settings.
2. Select **English**.

**Expected:** UI strings revert to English.

---

## 17. Navigation & Shell

### TC-NAV-001 — Sidebar shows only role-permitted items
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | — |

**Steps:**
1. Log in as FINANCE.
2. Inspect the sidebar navigation.

**Expected:** Only **Billing**, **Reports**, and **Settings** are visible; Clients, Leads, Schedule, Sessions, Admin are absent.

---

### TC-NAV-002 — Root path redirects to first permitted route
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | User is logged in as FINANCE |

**Steps:**
1. Navigate to `/`.

**Expected:** Browser redirects to `/billing` (first permitted route for the FINANCE role).

---

### TC-NAV-003 — Global search returns relevant results
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | At least one client and one session exist |

**Steps:**
1. Click the global search input in the shell.
2. Type a client's last name.

**Expected:** Search results include the matching client; clicking the result navigates to their detail page.

---

### TC-NAV-004 — Active nav item is highlighted
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | — |

**Steps:**
1. Click each sidebar item in turn.

**Expected:** The currently active route's sidebar item has a visually distinct active state.

---

## 18. Role-Based Access Control

### TC-RBAC-001 — THERAPIST cannot access Billing
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | — |

**Steps:**
1. Log in as THERAPIST.
2. Navigate directly to `/billing`.

**Expected:** Role guard fires; user sees an "Access Denied" toast and is redirected to `/`.

---

### TC-RBAC-002 — FINANCE cannot access Admin
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | FINANCE |
| Preconditions | — |

**Steps:**
1. Log in as FINANCE.
2. Navigate directly to `/admin`.

**Expected:** Redirect to `/` with access-denied notification.

---

### TC-RBAC-003 — RECEPTION_ADMIN_STAFF cannot access Sessions
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | RECEPTION_ADMIN_STAFF |
| Preconditions | — |

**Steps:**
1. Log in as RECEPTION_ADMIN_STAFF.
2. Navigate directly to `/sessions`.

**Expected:** Redirect to `/` with access-denied notification.

---

### TC-RBAC-004 — Non-GLOBAL_ADMIN cannot access Global Admin
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | — |

**Steps:**
1. Log in as SYSTEM_ADMINISTRATOR (no GLOBAL_ADMIN authority).
2. Navigate directly to `/global-admin`.

**Expected:** `globalAdminGuard` fires; redirect to `/` with access-denied notification.

---

### TC-RBAC-005 — `*appHasPermission` hides invoice actions from THERAPIST
| Field | Value |
|-------|-------|
| Mode | BOTH |
| Role | THERAPIST |
| Preconditions | THERAPIST has access to a session detail that includes an invoice link |

**Steps:**
1. Log in as THERAPIST.
2. Open a session detail dialog.

**Expected:** Billing-action buttons (guarded by `VIEW_BILLING_ACTIONS`) are not rendered in the DOM.

---

## 19. Mode-Based Access Control

### TC-MODE-001 — SOLO mode hides Caseload report
| Field | Value |
|-------|-------|
| Mode | SOLO |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | App running in SOLO mode |

**Steps:**
1. Log in as SYSTEM_ADMINISTRATOR in SOLO mode.
2. Navigate to **Reports**.

**Expected:** Caseload Overview tab/link is absent; navigating to `/reports/caseload` redirects to `/`.

---

### TC-MODE-002 — SOLO mode hides Users, Therapists, Leave Requests tabs in Admin
| Field | Value |
|-------|-------|
| Mode | SOLO |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | App running in SOLO mode |

**Steps:**
1. Navigate to **Admin**.

**Expected:** Only **Service Catalog** and **Notification Templates** tabs are visible; Users, Therapists, and Leave Requests are absent.

---

### TC-MODE-003 — CLINIC mode shows all Admin tabs
| Field | Value |
|-------|-------|
| Mode | CLINIC |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | App running in CLINIC mode |

**Steps:**
1. Navigate to **Admin**.

**Expected:** All five tabs are visible: Users, Therapists, Leave Requests, Service Catalog, Notification Templates.

---

### TC-MODE-004 — SOLO mode blocks direct navigation to clinic-only route
| Field | Value |
|-------|-------|
| Mode | SOLO |
| Role | SYSTEM_ADMINISTRATOR |
| Preconditions | App running in SOLO mode |

**Steps:**
1. Navigate directly to `/admin/users`.

**Expected:** `modeGuard` fires; redirect to `/`.

---
