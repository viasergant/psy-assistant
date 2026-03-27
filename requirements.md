# CRM for Psychological Assistance

## 1. Purpose

This document defines product requirements for a CRM application intended for psychological assistance organizations and private practices. The system must support the full client lifecycle, from first contact through intake, scheduling, session tracking, payments, follow-up, and reporting, while protecting sensitive personal and health-related data.

The product is intended to improve operational efficiency for psychologists, administrators, coordinators, and managers, while providing a structured and compliant way to manage client relationships and care workflows.

## 2. Product Vision

The application will serve as a centralized CRM for psychology and mental health assistance services. It must combine client relationship management, appointment operations, therapist workflow support, and business reporting in a single platform.

The solution should be implemented as:

- Backend: Java-24,  Spring Boot ecosystem
- Frontend: Angular
- Database: relational database, preferably PostgreSQL
- API style: REST as primary integration model

## 3. Business Goals

- Centralize all client and therapist operational data in one system
- Reduce manual administrative work for appointment coordination and follow-up
- Improve client retention and continuity of care
- Provide visibility into therapist utilization, revenue, and service outcomes
- Ensure secure processing of sensitive client data
- Support future scaling to multiple clinics, teams, or branches

## 4. Target Users

### 4.1 Administrators

Responsible for intake, scheduling, payments, client communications, and maintaining records.

### 4.2 Psychologists / Therapists

Responsible for reviewing client history, managing sessions, writing notes, and tracking treatment progress.

### 4.3 Supervisors / Clinical Managers

Responsible for workload oversight, operational monitoring, reporting, and quality control.

### 4.4 Finance / Operations Staff

Responsible for invoicing, payment tracking, refunds, subscription or package management, and financial reporting.

### 4.5 System Administrators

Responsible for user management, role-based access, configuration, audit review, and system settings.

## 5. Scope

### 5.1 In Scope

- Lead and client management
- Therapist management
- Appointment scheduling and calendar views
- Session records and case history
- Notes and treatment tracking
- Group therapy session support
- Payments and invoices
- Notifications and reminders
- Reporting and dashboards
- User roles and permissions
- Audit trail for sensitive actions

### 5.2 Out of Scope for Initial Release

- Video conferencing platform development from scratch
- AI diagnosis or automated treatment recommendations
- Insurance provider claim automation
- Mobile native applications
- Public marketplace for therapist discovery
- Online client self-service booking

## 6. Key Features

### 6.1 Lead and Client Management

The system must allow staff to register and manage leads, convert them into active clients, and maintain a complete history of interactions.

Key capabilities:

- Create and edit lead profiles
- Capture source of acquisition such as website, phone, referral, campaign, or partner
- Track lead status such as new, contacted, qualified, converted, inactive
- Convert lead to client without data re-entry
- Maintain client master profile with demographic and contact information
- Store emergency contact and preferred communication method
- Track client tags, categories, and case type

### 6.2 Intake and Assessment

The system must support structured intake collection before the first session.

Key capabilities:

- Digital intake forms
- Consent form tracking
- Risk flag indicators
- Initial assessment capture
- Assignment to therapist or waiting list
- Document attachment support

### 6.3 Therapist and Staff Management

The system must maintain professional profiles and operational availability for therapists and relevant staff.

Key capabilities:

- Therapist profile with specialization, language, credentials, and availability
- Employment or contractor status
- Session pricing rules per therapist or service type
- Working hours and leave management
- Caseload overview

### 6.4 Scheduling and Calendar

The system must provide multi-user scheduling for appointments and therapy sessions.

Key capabilities:

- Create, reschedule, cancel, and confirm appointments
- Calendar views by day, week, month
- Therapist availability management
- Conflict detection for overlapping appointments
- Waiting list support
- Recurring appointments
- Session type support such as in-person, online, group, intake, follow-up
- Reminder notifications for clients and staff
- Google Calendar synchronization for therapist calendars

### 6.4.1 External Calendar Synchronization

The system should synchronize internal appointments with external calendar platforms, with Google Calendar included in the initial scope.

Key capabilities:

- Connect therapist Google accounts securely using OAuth 2.0
- Sync CRM appointments to Google Calendar
- Detect synchronization failures and surface retry status to administrators
- Allow per-user control over calendar sync enablement
- Use one-way synchronization in MVP from CRM to Google Calendar

### 6.5 Session Management

The system must allow therapists to manage session records linked to each client.

Key capabilities:

- Create session entries from appointments
- Track attendance status such as attended, no-show, cancelled, late cancellation
- Capture structured or free-form session notes
- Record treatment goals and progress observations
- View full case timeline
- Restrict access to confidential notes by role
- Support both individual and group therapy session records

### 6.6 Care Plan and Progress Tracking

The system should support longitudinal client progress.

Key capabilities:

- Define goals, interventions, and milestones
- Track progress per care plan
- Record outcome measures and periodic reviews
- Highlight inactive clients or at-risk follow-up gaps

### 6.7 Billing and Payments

The system must support operational finance workflows.

Key capabilities:

- Configure service catalog and pricing
- Generate invoices for sessions or packages
- Track payment status such as pending, paid, overdue, refunded
- Support manual payment registration and future online payment integration
- Support discounts, prepaid packages, subscriptions, or session bundles
- Produce financial summaries by therapist, service, and period

### 6.8 Communication and Notifications

The system must support controlled client communication.

Key capabilities:

- Email and SMS notification support via external providers
- Appointment reminders
- Follow-up reminders after missed sessions
- Templates for common messages
- Communication history per client

### 6.9 Reporting and Dashboards

The system must provide operational and business visibility.

Key capabilities:

- Dashboard for upcoming sessions, no-shows, new leads, unpaid invoices
- Reports on conversion rates, therapist utilization, revenue, retention, cancellations
- Filter reports by date range, therapist, location, service type, lead source
- Export report data to CSV or Excel

### 6.10 Administration and Security

The system must protect sensitive information and support controlled operations.

Key capabilities:

- Role-based access control
- User and permission management
- Audit logs for create, update, delete, login, permission changes, and note access
- Configurable retention policies for records and logs
- Data export and anonymization support where required by policy

### 6.11 Multilingual User Experience

The system must support multiple interface languages, with Ukrainian and English available in the initial release.

Key capabilities:

- User-selectable language preference
- English as the default interface language for new users
- Localized UI labels, navigation, messages, and validation text
- Localized notification templates for supported communication channels
- Support for future addition of more languages without major rework
- Language switching available from the login page and persisted for subsequent visits

## 7. Functional Requirements

### 7.1 Authentication and Authorization

- The system shall require authentication for all internal users.
- The system shall support role-based authorization with at least the following roles: Admin, Therapist, Reception/Admin Staff, Finance, Supervisor, System Administrator.
- The system shall restrict access to clinical notes based on permissions.
- The system shall support password reset and account lockout rules.
- The system should support single sign-on in future releases.

### 7.2 Lead and Client Lifecycle

- The system shall allow staff to create a lead manually.
- The system shall allow leads to be imported from external sources such as CSV.
- The system shall record lead owner, source, status, and last contact date.
- The system shall allow conversion of a lead into a client profile.
- The system shall preserve the full activity history during lead conversion.
- The system shall allow staff to search clients by name, phone, email, identifier, or tag.

### 7.3 Client Profile

- The system shall maintain a unique client record.
- The system shall store personal information, contact details, referral information, emergency contact, and communication preferences.
- The system shall support document uploads linked to the client profile.
- The system shall display a chronological activity timeline for each client.

### 7.4 Intake Forms and Consents

- The system shall support configurable intake forms.
- The system shall record consent status and consent timestamps.
- The system shall allow authorized users to review submitted forms.
- The system shall flag incomplete intake requirements before the first session.

### 7.5 Scheduling

- The system shall allow authorized users to book appointments for available therapists.
- The system shall prevent double booking unless explicitly overridden by privileged users.
- The system shall support rescheduling and cancellation with reason capture.
- The system shall support recurring appointments.
- The system shall support waitlist placement and promotion when a slot becomes available.
- The system shall generate reminders based on configurable rules.
- The system shall support both individual and group therapy appointments.

### 7.5.1 Google Calendar Synchronization

- The system shall support integration with Google Calendar for connected therapist accounts.
- The system shall create or update Google Calendar events when CRM appointments are created, changed, or cancelled.
- The system shall store synchronization status for each synced appointment.
- The system shall prevent duplicate external calendar events for the same appointment.
- The system shall allow authorized users to reconnect expired Google integrations.
- The system shall use one-way synchronization in MVP from CRM to Google Calendar.
- The system should support configurable sync direction in later releases if two-way sync is introduced.

### 7.6 Session Records

- The system shall create a session record associated with the client, therapist, and appointment.
- The system shall allow therapists to write notes after sessions.
- The system shall support note visibility rules, including private clinical notes.
- The system shall record attendance and no-show outcomes.
- The system shall maintain version history or audit metadata for note changes.
- The system shall support recording group therapy sessions with multiple participating clients.

### 7.7 Billing

- The system shall allow configuration of services and prices.
- The system shall generate invoices from sessions, packages, or manual entries.
- The system shall track payment state and payment date.
- The system shall allow refunds and payment adjustments for authorized roles.
- The system shall produce receivables and revenue reports.

### 7.8 Notifications

- The system shall send appointment reminders using configured channels.
- The system shall log notification delivery attempts and status.
- The system shall allow template configuration for different communication events.

### 7.8.1 Localization

- The system shall provide the user interface in English and Ukrainian in the initial release.
- The system shall allow each internal user to select a preferred language.
- The system shall use English as the default language for new users.
- The system shall display localized static text, validation messages, and system notifications based on the selected language.
- The system shall support localized email and SMS templates where those channels are enabled.
- The system shall ensure new UI features are added using the localization mechanism rather than hard-coded strings.
- The system shall allow language selection on the login page.
- The system shall persist the selected language in cookies until user preferences are available or updated.

### 7.9 Reporting

- The system shall provide dashboards with role-relevant widgets.
- The system shall provide filterable reports for operational and financial metrics.
- The system shall allow export of reporting data.

### 7.10 Audit and Compliance

- The system shall log critical user actions.
- The system shall log access to confidential client data and notes.
- The system shall provide immutable audit records or equivalent tamper-evident controls.
- The system shall support retention and deletion policies aligned with organizational rules.

## 8. Non-Functional Requirements

### 8.1 Security

- All data in transit must be encrypted using TLS.
- Sensitive personal data must be encrypted at rest where appropriate.
- Passwords must be stored using strong hashing algorithms.
- The solution must implement secure session handling and protection against common web vulnerabilities.
- Access to protected health or sensitive information must be minimized by role.

### 8.2 Performance

- The system should support at least 200 concurrent internal users in the initial target deployment.
- Standard page loads should complete within 3 seconds under normal load.
- Search responses for common client lookups should complete within 2 seconds for expected data volumes.

### 8.3 Availability and Reliability

- The system should target 99.5% uptime or better for production deployments.
- The system must include backup and restore procedures.
- Critical operations must be resilient to transient integration failures.

### 8.4 Scalability

- The initial release shall support a single clinic deployment.
- The system architecture should support future expansion to multiple branches or clinics.
- The data model should support future tenant or branch separation if multi-location operation is planned.

### 8.5 Maintainability

- The backend must follow layered or modular architecture with clear domain boundaries.
- The frontend must follow Angular module or standalone component organization with clear feature separation.
- The system must expose documented APIs for future integrations.
- Logging and monitoring must be included for production diagnostics.
- Internationalization and localization resources must be managed in a way that supports additional languages without code duplication.

### 8.6 Compliance and Privacy

- The application must support compliance with applicable privacy regulations depending on deployment region, such as GDPR or similar health-data requirements.
- The product must include consent handling, auditability, and controlled data access.
- Data export and deletion workflows must be supported according to organizational and legal policy.

## 9. Suggested Roles and Permissions Model

- Reception/Admin Staff: manage leads, clients, appointments, reminders, basic billing
- Therapist: view assigned clients, manage sessions, write notes, review care plans
- Supervisor: view team workload, full clinical notes, selected summaries, reports
- Finance: manage invoices, payments, refunds, financial reporting
- System Administrator: manage users, roles, configuration, audit access

## 10. Suggested MVP Release Scope

The minimum viable product should include:

- User authentication and role management
- Lead and client management
- Therapist directory and availability
- Appointment scheduling and reminders
- Group therapy scheduling and session tracking
- Session tracking with notes
- Invoicing and payment status tracking
- One-way Google Calendar synchronization from CRM to therapist calendars
- English and Ukrainian localization with English default selection
- Basic dashboards and exports
- Audit logging for sensitive actions

## 11. Java and Angular Implementation Notes

### 11.1 Backend Architecture

Recommended backend stack:

- Java 21 or current LTS version
- Spring Boot
- Spring Security for authentication and authorization
- Spring Data JPA or Hibernate for persistence
- PostgreSQL for primary storage
- Flyway or Liquibase for database migrations
- Bean Validation for request validation
- OpenAPI/Swagger for API documentation

Backend implementation expectations:

- Use a modular domain structure such as clients, scheduling, billing, reporting, users, notifications
- Expose REST APIs for Angular consumption
- Enforce role-based securi ty at both endpoint and business-service levels
- Enforce role-based security at both endpoint and business-service levels
- Implement DTO boundaries instead of exposing persistence entities directly
- Store audit events for critical operations
- Integrate external messaging providers through dedicated adapter services
- Implement Google Calendar integration through a dedicated adapter or integration module with OAuth token lifecycle handling

### 11.2 Frontend Architecture

Recommended frontend stack:

- Angular current LTS-compatible version
- Angular Material or another enterprise UI library if design system is needed
- RxJS for asynchronous data handling
- Angular Router with role-guarded routes
- Reactive Forms for intake, profile, and billing forms

Frontend implementation expectations:

- Build feature-based modules or standalone feature areas such as leads, clients, schedule, sessions, billing, reports, admin
- Implement route guards and permission-based UI visibility
- Provide responsive calendar, dashboard, and profile management screens
- Use reusable form and table components for CRM workflows
- Ensure accessible UX for back-office users
- Use Angular internationalization support or an equivalent translation framework to deliver English and Ukrainian UI variants

### 11.3 Integration Considerations

- Email provider integration for reminders and notifications
- SMS provider integration where required
- Payment gateway integration as a future-ready extension point
- Google Calendar synchronization in initial scope using one-way CRM-to-Google event publishing
- Other calendar providers may be added later through the same integration pattern

## 12. Suggested Core Entities

- User
- Role
- Therapist
- Lead
- Client
- ContactHistory
- IntakeForm
- Consent
- Appointment
- Session
- SessionNote
- CarePlan
- Goal
- Invoice
- Payment
- Notification
- AuditLog
- CalendarSyncAccount
- CalendarSyncEvent

## 13. Success Metrics

- Reduced manual appointment coordination time
- Increased lead-to-client conversion rate
- Reduced no-show rate through reminders
- Improved therapist utilization visibility
- Faster invoice collection and reduced overdue balances
- Full traceability of access to sensitive data

## 14. Product Decisions

- Initial release scope is for a single clinic deployment.
- No additional country-specific compliance requirements are defined beyond general privacy and security controls.
- Online client self-service booking is planned for a later phase and is not part of MVP.
- Group therapy sessions are required in MVP.
- Insurance claim management is planned for a later phase.
- Supervisors are allowed to access full clinical notes according to role permissions.
- Google Calendar synchronization in MVP is one-way from the CRM application to Google Calendar.
- English is the default system language for new users, with language switching available on the login page and persisted in cookies.

## 15. Summary

This CRM for psychological assistance should combine client management, care operations, scheduling, billing, and reporting in a secure platform designed for sensitive data handling. The preferred implementation approach is a Java backend, ideally with Spring Boot, and an Angular frontend structured into feature-based modules. The MVP should prioritize operational efficiency, confidentiality, and clean extensibility for future clinical and financial workflows.
###