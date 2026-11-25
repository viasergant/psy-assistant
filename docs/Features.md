# Features

Inventory of all functional areas, pages, and capabilities in the psy-assistant frontend.
Availability columns: **SOLO** = available in solo-practitioner mode; **CLINIC** = available in clinic mode.

---

## Deployment Modes

The app boots in one of two modes, determined by `GET /api/v1/meta`:

| Mode | Description |
|------|-------------|
| `SOLO` | Single therapist, no multi-user management |
| `CLINIC` | Multi-user practice with admin, finance, and supervisor roles |

---

## Roles

| Role | Description |
|------|-------------|
| `THERAPIST` | Clinician managing own clients and sessions |
| `RECEPTION_ADMIN_STAFF` | Front-desk staff; manages leads, clients, and scheduling |
| `FINANCE` | Billing and revenue management |
| `SUPERVISOR` | Oversight and reporting; read-only across therapists |
| `SYSTEM_ADMINISTRATOR` | Full access to all clinic features |
| `GLOBAL_ADMIN` | Cross-organisation platform administration (JWT authority) |

---

## Feature Areas

### 1. Authentication

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| Login with username / password | All | ✓ | ✓ |
| Forced password change on first login | All | ✓ | ✓ |
| JWT-based session with role and permission claims | All | ✓ | ✓ |
| Redirect to original URL after login (`returnUrl`) | All | ✓ | ✓ |

---

### 2. Leads

> Route: `/leads` — RECEPTION_ADMIN_STAFF, SYSTEM_ADMINISTRATOR

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| View lead list (table with status, contact info) | ADMIN, RECEPTION | ✓ | ✓ |
| Create new lead | ADMIN, RECEPTION | ✓ | ✓ |
| Edit lead details | ADMIN, RECEPTION | ✓ | ✓ |
| Convert lead to client | ADMIN, RECEPTION | ✓ | ✓ |

---

### 3. Clients

> Route: `/clients` — THERAPIST, RECEPTION_ADMIN_STAFF, SYSTEM_ADMINISTRATOR

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| View client list | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Client detail page (demographics, contact, notes) | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Client activity timeline | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Risk flags — add, resolve, view panel | THERAPIST, ADMIN | ✓ | ✓ |
| Client packages — view purchased packages | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |

#### 3a. Care Plans

> Route: `/clients/:id/care-plans`

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| List care plans for a client | THERAPIST, ADMIN | ✓ | ✓ |
| Create / edit care plan | THERAPIST, ADMIN | ✓ | ✓ |
| Care plan detail — goals, outcomes, progress | THERAPIST, ADMIN | ✓ | ✓ |
| Outcome measure entry (manual score input) | THERAPIST, ADMIN | ✓ | ✓ |
| Outcome measure chart (trend visualisation) | THERAPIST, ADMIN | ✓ | ✓ |
| Goal progress panel | THERAPIST, ADMIN | ✓ | ✓ |

---

### 4. Schedule

> Route: `/schedule` — THERAPIST, RECEPTION_ADMIN_STAFF, SYSTEM_ADMINISTRATOR

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| Calendar — day view | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Calendar — week view | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Calendar — month view | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Book a new appointment (single or recurring) | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Edit appointment | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Reschedule appointment | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Cancel appointment | THERAPIST, ADMIN, RECEPTION | ✓ | ✓ |
| Recurring series conflict review | THERAPIST, ADMIN | ✓ | ✓ |
| Schedule configuration panel (working hours, etc.) | THERAPIST, ADMIN | ✓ | ✓ |
| Submit leave request | THERAPIST | — | ✓ |

---

### 5. Sessions

> Route: `/sessions` — THERAPIST, SYSTEM_ADMINISTRATOR

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| View session list (past and upcoming) | THERAPIST, ADMIN | ✓ | ✓ |
| Session detail dialog | THERAPIST, ADMIN | ✓ | ✓ |
| Mark session as complete | THERAPIST, ADMIN | ✓ | ✓ |
| Cancel session | THERAPIST, ADMIN | ✓ | ✓ |
| Record attendance | THERAPIST, ADMIN | ✓ | ✓ |
| Group session attendance panel | THERAPIST, ADMIN | ✓ | ✓ |
| Group session participant panel | THERAPIST, ADMIN | ✓ | ✓ |
| Session notes — create and edit (encrypted) | THERAPIST, ADMIN | ✓ | ✓ |
| Session note version history | THERAPIST, ADMIN | ✓ | ✓ |

---

### 6. Billing

> Route: `/billing` — FINANCE, SYSTEM_ADMINISTRATOR

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| **Finance dashboard** — revenue KPIs, charts | FINANCE, ADMIN | ✓ | ✓ |
| **Invoices** — list all invoices | FINANCE, ADMIN | ✓ | ✓ |
| Create new invoice | FINANCE, ADMIN | ✓ | ✓ |
| Invoice detail — line items, status, payment history | FINANCE, ADMIN | ✓ | ✓ |
| Register payment against an invoice | FINANCE, ADMIN | ✓ | ✓ |
| Register refund | FINANCE, ADMIN | ✓ | ✓ |
| Override therapist on invoice | FINANCE, ADMIN | ✓ | ✓ |
| Update line-item price | FINANCE, ADMIN | ✓ | ✓ |
| **Service catalog** — list services | FINANCE, ADMIN | ✓ | ✓ |
| Create / edit service catalog item | FINANCE, ADMIN | ✓ | ✓ |
| **Package definitions** — list bundles | FINANCE, ADMIN | ✓ | ✓ |
| Create / edit package definition | FINANCE, ADMIN | ✓ | ✓ |
| Sell a package to a client | FINANCE, ADMIN | ✓ | ✓ |
| **Discount rules** — list rules | FINANCE, ADMIN | ✓ | ✓ |
| Create / edit discount rule | FINANCE, ADMIN | ✓ | ✓ |

---

### 7. Reports

> Route: `/reports` — SUPERVISOR, FINANCE, SYSTEM_ADMINISTRATOR, THERAPIST (varies per report)

| Report | Roles | SOLO | CLINIC |
|--------|-------|------|--------|
| Caseload overview (active clients per therapist) | ADMIN, SUPERVISOR | — | ✓ |
| Caseload drilldown (per-therapist detail) | ADMIN, SUPERVISOR | — | ✓ |
| Lead conversion rate | SUPERVISOR, FINANCE, ADMIN | ✓ | ✓ |
| Therapist utilisation | SUPERVISOR, FINANCE, ADMIN | — | ✓ |
| Revenue summary | SUPERVISOR, FINANCE, ADMIN | ✓ | ✓ |
| Client retention | SUPERVISOR, FINANCE, ADMIN | ✓ | ✓ |
| No-show and cancellation rate | SUPERVISOR, FINANCE, ADMIN | ✓ | ✓ |
| Report filters (date range, therapist, etc.) | (same as report) | ✓ | ✓ |
| Export controls (CSV / PDF) | (same as report) | ✓ | ✓ |

---

### 8. Admin

> Route: `/admin` — SYSTEM_ADMINISTRATOR

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| **Users** — list platform users | ADMIN | — | ✓ |
| Create user | ADMIN | — | ✓ |
| Edit user (name, roles, status) | ADMIN | — | ✓ |
| Reset user password | ADMIN | — | ✓ |
| **Therapists** — list therapist profiles | ADMIN | — | ✓ |
| Therapist detail (specialisations, availability) | ADMIN | — | ✓ |
| Create therapist account | ADMIN | — | ✓ |
| Edit therapist profile | ADMIN | — | ✓ |
| **Leave requests** — review pending requests | ADMIN | — | ✓ |
| Approve / reject leave request | ADMIN | — | ✓ |
| **Service catalog** — same as Billing > Service catalog | ADMIN | ✓ | ✓ |
| **Notification templates** — list templates | ADMIN | ✓ | ✓ |
| Create / edit notification template | ADMIN | ✓ | ✓ |
| Preview notification template | ADMIN | ✓ | ✓ |
| **Risk flag types** — list types | ADMIN | ✓ | ✓ |
| Create / edit risk flag type | ADMIN | ✓ | ✓ |

---

### 9. Global Admin

> Route: `/global-admin` — GLOBAL_ADMIN authority (cross-organisation)

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| Risk flag type list (platform-wide) | GLOBAL_ADMIN | ✓ | ✓ |
| Create / edit risk flag type (platform-wide) | GLOBAL_ADMIN | ✓ | ✓ |
| User list (all organisations, read-only) | GLOBAL_ADMIN | ✓ | ✓ |

---

### 10. Therapist Profile Setup

> Route: `/therapist/profile/complete` — THERAPIST (redirected automatically on incomplete profile)

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| Step-by-step profile completion wizard | THERAPIST | ✓ | ✓ |

---

### 11. Settings

> Route: `/settings` — All roles

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| Language selection — English / Ukrainian | All | ✓ | ✓ |

---

### 12. Shell / Navigation

| Feature | Roles | SOLO | CLINIC |
|---------|-------|------|--------|
| Sidebar navigation with role-filtered menu items | All | ✓ | ✓ |
| Global search | All | ✓ | ✓ |
| Root redirect to first permitted route on login | All | ✓ | ✓ |

---

## Cross-Cutting Concerns

| Concern | Implementation |
|---------|---------------|
| Authentication guard | Redirects to `/auth/login` if no valid token |
| Forced password change | Intercepts all navigation if `mustChangePassword` in JWT |
| Profile completion | Redirects THERAPIST to wizard if profile incomplete |
| Role-based access | `roleGuard` on each feature route; `*appHasPermission` in templates |
| Mode-based access | `modeGuard` on CLINIC-only routes; `*appIfMode` in templates |
| PII protection | No client names or notes logged; opaque IDs only (ADR-001) |
| i18n | All UI strings via Transloco keys; English and Ukrainian supported |
