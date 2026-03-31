---
goal: 'PA-29: Working hours, availability, and leave management implementation'
version: 1.0
date_created: 2026-03-31
last_updated: 2026-03-31
owner: Development Team
status: 'In progress'
tags: ['feature', 'scheduling', 'backend', 'frontend', 'PA-29']
---

# Introduction

![Status: In progress](https://img.shields.io/badge/status-In%20progress-yellow)

Implementation plan for PA-29: Working hours, availability, and leave management. This feature provides administrators and therapists with tools to define and maintain therapist working schedules through recurring weekly patterns, one-off overrides, and leave period management. The scheduling engine will consult this data to prevent double-booking and ensure only genuinely available slots are offered for appointments.

## 1. Requirements & Constraints

### Functional Requirements

- **REQ-001**: System Administrator can define and modify a therapist's recurring weekly working hours (day of week + time range)
- **REQ-002**: System Administrator can add one-off availability overrides for specific dates (custom hours or mark as unavailable)
- **REQ-003**: System Administrator can record leave periods with start date, end date, and leave type
- **REQ-004**: Therapist can view their own schedule including recurring hours, overrides, and leave periods
- **REQ-005**: Therapist can submit leave requests (status: Pending, Approved, Rejected)
- **REQ-006**: System Administrator can approve/reject leave requests
- **REQ-007**: Scheduling engine must respect all schedule rules when offering available slots
- **REQ-008**: Therapist with RECEPTION_ADMIN_STAFF role can edit their own schedule without approval
- **REQ-009**: Therapist with only THERAPIST role has read-only access and can only submit leave requests
- **REQ-010**: 30-minute slot granularity for all time-based operations
- **REQ-011**: Per-therapist timezone support for schedule definition

### Technical Requirements

- **TECH-001**: Availability query response time < 500ms for 4-week window under normal load
- **TECH-002**: Use BaseEntity for audit columns (createdAt, updatedAt, createdBy)
- **TECH-003**: Implement field-level audit trail (AuditEntry + AuditChange pattern)
- **TECH-004**: Real-time conflict detection when creating leave requests
- **TECH-005**: Follow Spring Boot best practices (constructor injection, DTOs, ControllerAdvice error handling)
- **TECH-006**: Angular standalone component architecture
- **TECH-007**: PrimeNG components for UI elements
- **TECH-008**: Transloco for i18n strings

### Security Requirements

- **SEC-001**: Role-based access control using Spring Security
  - SYSTEM_ADMINISTRATOR: full CRUD on all schedules
  - THERAPIST + RECEPTION_ADMIN_STAFF: CRUD on own schedule
  - THERAPIST only: Read own schedule + submit leave requests
- **SEC-002**: Therapists can only view their own schedule data
- **SEC-003**: All schedule mutations must be audited with actor identity

### Accessibility & UI Requirements

- **UI-001**: WCAG 2.1 AA accessibility standards compliance
- **UI-002**: Intuitive time-range entry with 30-minute increments
- **UI-003**: Visual calendar with color-coded slot states (available, booked, leave, override, unavailable)
- **UI-004**: Conflict warning with detailed appointment breakdown

### Data Consistency Requirements

- **CON-001**: Override takes precedence over recurring schedule for specific dates
- **CON-002**: Leave period marks all dates in range as unavailable
- **CON-003**: Approved leave immediately blocks affected date range in scheduling engine
- **CON-004**: Schedule changes must be atomic (transaction isolation)

### Design Guidelines

- **GUD-001**: Follow UI design spec at docs/specs/PA-29-scheduling-calendar-ui-design.md
- **GUD-002**: Use domain-driven package structure (scheduling/domain, dto, repository, rest, service)
- **GUD-003**: Database schema changes via Flyway migration scripts

### Implementation Patterns

- **PAT-001**: Use Spring Data JPA repositories for data access
- **PAT-002**: Use @Transactional on service methods
- **PAT-003**: Use @Valid for DTO validation
- **PAT-004**: Use @ControllerAdvice for global exception handling
- **PAT-005**: Use Angular route guards for role-based navigation

## 2. Implementation Steps

### Phase 1: Database Schema & Domain Models

**GOAL-001**: Create database schema for schedule management entities

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-001 | Create V19__therapist_schedule_recurring.sql migration for recurring weekly schedule table | | |
| TASK-002 | Create V20__therapist_schedule_override.sql migration for schedule override table | | |
| TASK-003 | Create V21__therapist_leave.sql migration for leave period table | | |
| TASK-004 | Create V22__therapist_schedule_audit.sql migration for audit tables (entry + changes) | | |
| TASK-005 | Create TherapistRecurringSchedule entity extending BaseEntity with day_of_week, start_time, end_time, timezone | | |
| TASK-006 | Create TherapistScheduleOverride entity for one-off date overrides | | |
| TASK-007 | Create TherapistLeave entity with start_date, end_date, leave_type, status, request_notes | | |
| TASK-008 | Create TherapistScheduleAuditEntry and TherapistScheduleAuditChange entities | | |
| TASK-009 | Create LeaveType enum (ANNUAL, SICK, PUBLIC_HOLIDAY, OTHER) | | |
| TASK-010 | Create LeaveStatus enum (PENDING, APPROVED, REJECTED, CANCELLED) | | |

### Phase 2: Backend Repositories

**GOAL-002**: Implement Spring Data JPA repositories for schedule data access

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-011 | Create TherapistRecurringScheduleRepository with findByTherapistProfileId method | | |
| TASK-012 | Create TherapistScheduleOverrideRepository with findByTherapistProfileIdAndDateBetween | | |
| TASK-013 | Create TherapistLeaveRepository with findByTherapistProfileIdAndStatus | | |
| TASK-014 | Create TherapistScheduleAuditEntryRepository with findByTherapistProfileIdOrderByCreatedAtDesc | | |
| TASK-015 | Add custom queries for date range overlap detection | | |

### Phase 3: Backend Service Layer

**GOAL-003**: Implement business logic for schedule management

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-016 | Create TherapistScheduleService with CRUD operations for recurring schedules | | |
| TASK-017 | Implement schedule override service methods (create, update, delete with date validation) | | |
| TASK-018 | Implement leave management service methods (submit request, approve, reject, cancel) | | |
| TASK-019 | Create AvailabilityQueryService with method to compute available slots for date range | | |
| TASK-020 | Implement conflict detection logic for leave requests (check existing appointments) | | |
| TASK-021 | Implement schedule change audit logging service | | |
| TASK-022 | Add @Transactional annotations on all mutating service methods | | |

### Phase 4: Backend DTOs

**GOAL-004**: Create request and response DTOs for REST API

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-023 | Create RecurringScheduleRequest DTO with @Valid annotations | | |
| TASK-024 | Create ScheduleOverrideRequest DTO with date and time validation | | |
| TASK-025 | Create LeaveRequestSubmission DTO with date range validation | | |
| TASK-026 | Create LeaveApprovalRequest DTO | | |
| TASK-027 | Create ScheduleSummaryResponse DTO (recurring + overrides + leave) | | |
| TASK-028 | Create AvailabilitySlotResponse DTO for availability queries | | |
| TASK-029 | Create ConflictWarningResponse DTO with appointment details | | |

### Phase 5: Backend REST Controllers

**GOAL-005**: Expose REST endpoints for schedule management

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-030 | Create TherapistScheduleController with endpoints for recurring schedule CRUD | | |
| TASK-031 | Add schedule override endpoints (POST, PUT, DELETE) | | |
| TASK-032 | Create TherapistLeaveController with submit, approve, reject, list endpoints | | |
| TASK-033 | Create AvailabilityController with GET endpoint for slot queries | | |
| TASK-034 | Add role-based @PreAuthorize annotations (SYSTEM_ADMINISTRATOR, RECEPTION_ADMIN_STAFF, THERAPIST) | | |
| TASK-035 | Implement therapist-owned data access control (check principal matches therapist or has admin role) | | |
| TASK-036 | Add conflict detection endpoint for leave request preview | | |

### Phase 6: Backend Security & Validation

**GOAL-006**: Ensure proper security and data validation

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-037 | Add method security configuration for schedule endpoints | | |
| TASK-038 | Implement custom validator for recurring schedule time ranges (start < end, 30-min aligned) | | |
| TASK-039 | Implement date range validator (end date >= start date) | | |
| TASK-040 | Add @ControllerAdvice exception handlers for schedule conflicts | | |
| TASK-041 | Add timezone validation for therapist timezone field | | |

### Phase 7: Backend Unit Tests

**GOAL-007**: Write comprehensive unit tests for backend services

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-042 | Write tests for TherapistScheduleService recurring schedule CRUD | | |
| TASK-043 | Write tests for override precedence logic | | |
| TASK-044 | Write tests for leave approval workflow state transitions | | |
| TASK-045 | Write tests for AvailabilityQueryService slot computation | | |
| TASK-046 | Write tests for conflict detection logic | | |
| TASK-047 | Write tests for audit logging on schedule changes | | |

### Phase 8: Frontend Domain Models & Services

**GOAL-008**: Create Angular services and TypeScript models

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-048 | Create schedule.model.ts with RecurringSchedule, ScheduleOverride, Leave interfaces | | |
| TASK-049 | Create schedule-api.service.ts with HTTP methods for all schedule endpoints | | |
| TASK-050 | Create availability.service.ts for slot queries | | |
| TASK-051 | Create leave-request.service.ts for leave management | | |
| TASK-052 | Add timezone handling utilities (convert therapist timezone to browser local time) | | |

### Phase 9: Frontend Calendar Components

**GOAL-009**: Implement calendar UI components following design spec

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-053 | Create schedule-calendar.component.ts (week view with time slots) | | |
| TASK-054 | Implement calendar grid rendering with 30-minute slot granularity | | |
| TASK-055 | Add slot state styling (available, unavailable, leave, override, booked) | | |
| TASK-056 | Implement week navigation (previous/next week buttons) | | |
| TASK-057 | Create therapist-roster-sidebar.component.ts (admin view) | | |
| TASK-058 | Add calendar legend component | | |

### Phase 10: Frontend Configuration Panel

**GOAL-010**: Build schedule configuration UI (admin & therapist self-service)

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-059 | Create recurring-schedule-config.component.ts with day/time pickers | | |
| TASK-060 | Implement schedule-override-list.component.ts with add/edit/delete | | |
| TASK-061 | Create leave-period-list.component.ts with status indicators | | |
| TASK-062 | Implement role-based edit permissions (show/hide configure button) | | |
| TASK-063 | Add form validation for time ranges and date inputs | | |

### Phase 11: Frontend Leave Request Modal

**GOAL-011**: Implement leave request submission modal for therapists

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-064 | Create leave-request-modal.component.ts with date range picker | | |
| TASK-065 | Add leave type dropdown (ANNUAL, SICK, PUBLIC_HOLIDAY, OTHER) | | |
| TASK-066 | Implement real-time conflict detection on date change | | |
| TASK-067 | Display conflict warning with appointment details | | |
| TASK-068 | Add submit/cancel actions | | |

### Phase 12: Frontend Routes & Permissions

**GOAL-012**: Configure routing and role-based access

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-069 | Add schedule routes to app.routes.ts (admin and therapist views) | | |
| TASK-070 | Create schedule route guard checking for SYSTEM_ADMINISTRATOR or THERAPIST roles | | |
| TASK-071 | Add navigation menu entries with role-based visibility | | |
| TASK-072 | Implement data scoping (therapist sees only own schedule unless admin) | | |

### Phase 13: Frontend i18n Strings

**GOAL-013**: Add internationalization support

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-073 | Add schedule-related strings to frontend/src/assets/i18n/en.json | | |
| TASK-074 | Add leave type labels and status labels | | |
| TASK-075 | Add conflict warning messages | | |
| TASK-076 | Add validation error messages | | |

### Phase 14: Frontend Unit Tests

**GOAL-014**: Write frontend component and service tests

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-077 | Write tests for schedule-calendar.component (week rendering, slot states) | | |
| TASK-078 | Write tests for recurring-schedule-config.component (form validation) | | |
| TASK-079 | Write tests for leave-request-modal.component (conflict detection) | | |
| TASK-080 | Write tests for schedule-api.service HTTP calls | | |
| TASK-081 | Write tests for role-based permission logic | | |

### Phase 15: Integration Testing & Documentation

**GOAL-015**: Validate end-to-end workflows and update documentation

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-082 | Test recurring schedule CRUD workflow (admin creates, edits, deletes) | | |
| TASK-083 | Test schedule override workflow (admin adds override, system respects it) | | |
| TASK-084 | Test leave request workflow (therapist submits, admin approves, schedule updates) | | |
| TASK-085 | Test availability query performance (measure response time for 4-week window) | | |
| TASK-086 | Test role-based access control (therapist can't edit other schedules) | | |
| TASK-087 | Test conflict detection accuracy (leave request shows existing appointments) | | |
| TASK-088 | Update backend README with schedule endpoints | | |
| TASK-089 | Update frontend README with schedule components | | |
| TASK-090 | Create API documentation in Swagger UI | | |

## 3. Alternatives

- **ALT-001**: Use full calendar library (FullCalendar, etc.) instead of custom calendar component
  - **Decision**: Build custom component to match exact design spec requirements and keep dependencies minimal
- **ALT-002**: Store availability as calendar events instead of rules + overrides
  - **Decision**: Use rule-based approach (recurring + overrides) for better storage efficiency and query performance
- **ALT-003**: Implement soft deletes for schedule records
  - **Decision**: Use hard deletes but rely on comprehensive audit trail (AuditEntry/AuditChange) for historical tracking
- **ALT-004**: Use read replicas for availability queries
  - **Decision**: Defer to v2; optimize queries first with proper indexing

## 4. Dependencies

- **DEP-001**: TherapistProfile entity must exist (already implemented in V11__therapist_profile_base.sql)
- **DEP-002**: BaseEntity and audit infrastructure (already implemented in common/audit/)
- **DEP-003**: User roles SYSTEM_ADMINISTRATOR, THERAPIST, RECEPTION_ADMIN_STAFF (already defined in V5__rbac_roles.sql)
- **DEP-004**: Spring Security configuration for method-level security (already implemented)
- **DEP-005**: PrimeNG components for Angular UI (already in use)
- **DEP-006**: Transloco for i18n (already configured)

## 5. Files

### Backend Files (New)

- **FILE-001**: backend/src/main/resources/db/migration/V19__therapist_schedule_recurring.sql
- **FILE-002**: backend/src/main/resources/db/migration/V20__therapist_schedule_override.sql
- **FILE-003**: backend/src/main/resources/db/migration/V21__therapist_leave.sql
- **FILE-004**: backend/src/main/resources/db/migration/V22__therapist_schedule_audit.sql
- **FILE-005**: backend/src/main/java/com/psyassistant/scheduling/domain/TherapistRecurringSchedule.java
- **FILE-006**: backend/src/main/java/com/psyassistant/scheduling/domain/TherapistScheduleOverride.java
- **FILE-007**: backend/src/main/java/com/psyassistant/scheduling/domain/TherapistLeave.java
- **FILE-008**: backend/src/main/java/com/psyassistant/scheduling/domain/LeaveType.java (enum)
- **FILE-009**: backend/src/main/java/com/psyassistant/scheduling/domain/LeaveStatus.java (enum)
- **FILE-010**: backend/src/main/java/com/psyassistant/scheduling/domain/TherapistScheduleAuditEntry.java
- **FILE-011**: backend/src/main/java/com/psyassistant/scheduling/domain/TherapistScheduleAuditChange.java
- **FILE-012**: backend/src/main/java/com/psyassistant/scheduling/repository/TherapistRecurringScheduleRepository.java
- **FILE-013**: backend/src/main/java/com/psyassistant/scheduling/repository/TherapistScheduleOverrideRepository.java
- **FILE-014**: backend/src/main/java/com/psyassistant/scheduling/repository/TherapistLeaveRepository.java
- **FILE-015**: backend/src/main/java/com/psyassistant/scheduling/repository/TherapistScheduleAuditEntryRepository.java
- **FILE-016**: backend/src/main/java/com/psyassistant/scheduling/service/TherapistScheduleService.java
- **FILE-017**: backend/src/main/java/com/psyassistant/scheduling/service/TherapistLeaveService.java
- **FILE-018**: backend/src/main/java/com/psyassistant/scheduling/service/AvailabilityQueryService.java
- **FILE-019**: backend/src/main/java/com/psyassistant/scheduling/service/TherapistScheduleAuditService.java
- **FILE-020**: backend/src/main/java/com/psyassistant/scheduling/dto/RecurringScheduleRequest.java
- **FILE-021**: backend/src/main/java/com/psyassistant/scheduling/dto/ScheduleOverrideRequest.java
- **FILE-022**: backend/src/main/java/com/psyassistant/scheduling/dto/LeaveRequestSubmission.java
- **FILE-023**: backend/src/main/java/com/psyassistant/scheduling/dto/LeaveApprovalRequest.java
- **FILE-024**: backend/src/main/java/com/psyassistant/scheduling/dto/ScheduleSummaryResponse.java
- **FILE-025**: backend/src/main/java/com/psyassistant/scheduling/dto/AvailabilitySlotResponse.java
- **FILE-026**: backend/src/main/java/com/psyassistant/scheduling/dto/ConflictWarningResponse.java
- **FILE-027**: backend/src/main/java/com/psyassistant/scheduling/rest/TherapistScheduleController.java
- **FILE-028**: backend/src/main/java/com/psyassistant/scheduling/rest/TherapistLeaveController.java
- **FILE-029**: backend/src/main/java/com/psyassistant/scheduling/rest/AvailabilityController.java

### Frontend Files (New)

- **FILE-030**: frontend/src/app/features/schedule/models/schedule.model.ts
- **FILE-031**: frontend/src/app/features/schedule/services/schedule-api.service.ts
- **FILE-032**: frontend/src/app/features/schedule/services/availability.service.ts
- **FILE-033**: frontend/src/app/features/schedule/services/leave-request.service.ts
- **FILE-034**: frontend/src/app/features/schedule/components/schedule-calendar/schedule-calendar.component.ts
- **FILE-035**: frontend/src/app/features/schedule/components/schedule-calendar/schedule-calendar.component.html
- **FILE-036**: frontend/src/app/features/schedule/components/schedule-calendar/schedule-calendar.component.scss
- **FILE-037**: frontend/src/app/features/schedule/components/therapist-roster-sidebar/therapist-roster-sidebar.component.ts
- **FILE-038**: frontend/src/app/features/schedule/components/recurring-schedule-config/recurring-schedule-config.component.ts
- **FILE-039**: frontend/src/app/features/schedule/components/schedule-override-list/schedule-override-list.component.ts
- **FILE-040**: frontend/src/app/features/schedule/components/leave-period-list/leave-period-list.component.ts
- **FILE-041**: frontend/src/app/features/schedule/components/leave-request-modal/leave-request-modal.component.ts
- **FILE-042**: frontend/src/app/features/schedule/guards/schedule.guard.ts

### Test Files (New)

- **FILE-043**: backend/src/test/java/com/psyassistant/scheduling/service/TherapistScheduleServiceTest.java
- **FILE-044**: backend/src/test/java/com/psyassistant/scheduling/service/AvailabilityQueryServiceTest.java
- **FILE-045**: backend/src/test/java/com/psyassistant/scheduling/service/TherapistLeaveServiceTest.java
- **FILE-046**: frontend/src/app/features/schedule/components/schedule-calendar/schedule-calendar.component.spec.ts
- **FILE-047**: frontend/src/app/features/schedule/components/leave-request-modal/leave-request-modal.component.spec.ts
- **FILE-048**: frontend/src/app/features/schedule/services/schedule-api.service.spec.ts

## 6. Testing

### Backend Unit Tests

- **TEST-001**: TherapistScheduleServiceTest.createRecurringSchedule_success
- **TEST-002**: TherapistScheduleServiceTest.updateRecurringSchedule_withConflict_fails
- **TEST-003**: TherapistScheduleServiceTest.deleteRecurringSchedule_removesAllSlots
- **TEST-004**: TherapistScheduleOverrideServiceTest.addOverride_overridesRecurringSchedule
- **TEST-005**: TherapistLeaveServiceTest.submitLeaveRequest_createsPendingStatus
- **TEST-006**: TherapistLeaveServiceTest.approveLeave_blocksSchedule
- **TEST-007**: TherapistLeaveServiceTest.rejectLeave_doesNotBlockSchedule
- **TEST-008**: AvailabilityQueryServiceTest.querySlots_respectsRecurringSchedule
- **TEST-009**: AvailabilityQueryServiceTest.querySlots_respectsOverride
- **TEST-010**: AvailabilityQueryServiceTest.querySlots_respectsApprovedLeave
- **TEST-011**: AvailabilityQueryServiceTest.querySlots_performanceUnder500ms
- **TEST-012**: ConflictDetectionServiceTest.detectConflicts_findsExistingAppointments
- **TEST-013**: AuditServiceTest.scheduleChange_createsAuditEntry

### Backend Integration Tests

- **TEST-014**: TherapistScheduleControllerIT.POST_recurringSchedule_returns201
- **TEST-015**: TherapistScheduleControllerIT.PUT_override_returns200
- **TEST-016**: TherapistLeaveControllerIT.POST_leaveRequest_returns201
- **TEST-017**: TherapistLeaveControllerIT.PUT_approveLeave_returns200
- **TEST-018**: AvailabilityControllerIT.GET_availableSlots_returns200
- **TEST-019**: SecurityIT.therapist_cannotEditOtherSchedule_returns403
- **TEST-020**: SecurityIT.receptionistTherapist_canEditOwnSchedule_returns200

### Frontend Component Tests

- **TEST-021**: ScheduleCalendarComponent.week_rendering_displaysSevenDays
- **TEST-022**: ScheduleCalendarComponent.slot_states_applyCorrectCssClasses
- **TEST-023**: RecurringScheduleConfigComponent.form_validation_requiresStartBeforeEnd
- **TEST-024**: LeaveRequestModalComponent.conflict_detection_showsWarning
- **TEST-025**: LeaveRequestModalComponent.submit_callsApiService
- **TEST-026**: ScheduleApiService.getScheduleSummary_callsCorrectEndpoint
- **TEST-027**: RoleGuard.admin_canAccessAllSchedules
- **TEST-028**: RoleGuard.therapist_canAccessOwnScheduleOnly

### End-to-End Tests

- **TEST-029**: E2E: Admin creates recurring schedule → therapist sees it in calendar
- **TEST-030**: E2E: Admin adds override → availability query excludes that time
- **TEST-031**: E2E: Therapist submits leave → admin sees pending request → approves → schedule blocks dates
- **TEST-032**: E2E: Leave request with conflicts → system shows warning with appointment details
- **TEST-033**: E2E: Receptionist-therapist edits own schedule → changes appear immediately
- **TEST-034**: E2E: Therapist-only user attempts edit → UI hides configure button

## 7. Risks & Assumptions

### Risks

- **RISK-001**: Performance degradation for availability queries with complex schedule rules
  - **Mitigation**: Add database indexes on therapist_profile_id + date columns; implement query result caching
- **RISK-002**: Timezone handling complexity across backend and frontend
  - **Mitigation**: Store all times in UTC; convert to therapist timezone only for display
- **RISK-003**: Concurrent schedule modifications causing data inconsistency
  - **Mitigation**: Use optimistic locking (@Version) on schedule entities; implement retry logic
- **RISK-004**: UI complexity for calendar rendering with multiple slot states
  - **Mitigation**: Iterate on design with UX feedback; use PrimeNG calendar as foundation
- **RISK-005**: Audit table growth impacting query performance
  - **Mitigation**: Add table partitioning strategy in v2; monitor table size

### Assumptions

- **ASSUMPTION-001**: Therapists work within a single timezone (stored in therapist profile)
- **ASSUMPTION-002**: 30-minute slot granularity is sufficient for all use cases
- **ASSUMPTION-003**: Leave approval workflow always requires admin intervention (no auto-approval rules)
- **ASSUMPTION-004**: Existing appointments are managed in a separate system/module (conflict detection reads from external table)
- **ASSUMPTION-005**: Notification system for leave approval is implemented separately (marked TBD in acceptance criteria)

## 8. Related Specifications / Further Reading

- [PA-29 Jira Ticket](https://alphanetvin.atlassian.net/browse/PA-29)
- [PA-29 Scheduling Calendar UI Design Spec](../docs/specs/PA-29-scheduling-calendar-ui-design.md)
- [Backend README](../backend/README.md)
- [Frontend README](../frontend/README.md)
- [ADR-001: Observability and Logging](../docs/adr/001-observability.md)
