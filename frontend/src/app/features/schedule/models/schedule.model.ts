/**
 * Recurring weekly schedule for a therapist
 * Note: Backend sends dayOfWeek as Integer (1=Monday, 2=Tuesday, ..., 7=Sunday)
 * startTime/endTime can be either string "HH:mm:ss" or array [hour, minute, second]
 */
export interface RecurringSchedule {
  id?: string;
  therapistProfileId: string;
  dayOfWeek: number; // ISO-8601: 1=Monday, 7=Sunday
  startTime: string | any; // HH:mm format or array [h, m, s]
  endTime: string | any;   // HH:mm format or array [h, m, s]
  timezone: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * One-time schedule override for a specific date
 */
export interface ScheduleOverride {
  id?: string;
  therapistProfileId: string;
  date: string; // yyyy-MM-dd format
  available: boolean;
  startTime?: string; // HH:mm format (if available=true)
  endTime?: string;   // HH:mm format (if available=true)
  reason?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Leave period for a therapist
 */
export interface Leave {
  id?: string;
  therapistProfileId: string;
  therapistName?: string; // Optional - only populated in admin views with join
  leaveType: LeaveType;
  startDate: string; // yyyy-MM-dd format
  endDate: string;   // yyyy-MM-dd format
  status: LeaveStatus;
  requestNotes?: string;
  adminNotes?: string; // Changed from responseNotes to match backend
  requestedAt?: string;
  reviewedAt?: string;
  reviewedBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Complete schedule summary for a therapist
 */
export interface ScheduleSummary {
  therapistProfileId: string;
  therapistName: string;
  timezone: string;
  recurringSchedule: RecurringSchedule[];
  overrides: ScheduleOverride[];
  leavePeriods: Leave[];
}

/**
 * Available time slot for booking
 */
export interface AvailabilitySlot {
  date: string;
  startTime: string;
  endTime: string;
  available: boolean;
}

/**
 * Conflict warning when submitting leave
 */
export interface ConflictWarning {
  hasConflicts: boolean;
  affectedAppointments: AppointmentConflict[];
}

export interface AppointmentConflict {
  appointmentId: string;
  clientName: string;
  scheduledAt: string;
  duration: number;
}

/**
 * Request to create/update recurring schedule
 * Note: Backend expects dayOfWeek as Integer (1=Monday, 2=Tuesday, ..., 7=Sunday)
 */
export interface RecurringScheduleRequest {
  dayOfWeek: number | DayOfWeek; // Accept both number and enum for flexibility
  startTime: string;
  endTime: string;
  timezone: string;
}

/**
 * Request to create/update schedule override
 */
export interface ScheduleOverrideRequest {
  date: string;
  available: boolean;
  startTime?: string;
  endTime?: string;
  reason?: string;
}

/**
 * Request to submit leave
 */
export interface LeaveRequest {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  requestNotes?: string;
}

/**
 * Request to approve/reject leave
 * The reviewer user ID is automatically extracted from the authenticated user's JWT token.
 */
export interface LeaveApprovalRequest {
  adminNotes?: string; // Optional notes from admin
}

/**
 * Day of week enum
 */
export enum DayOfWeek {
  MONDAY = 'MONDAY',
  TUESDAY = 'TUESDAY',
  WEDNESDAY = 'WEDNESDAY',
  THURSDAY = 'THURSDAY',
  FRIDAY = 'FRIDAY',
  SATURDAY = 'SATURDAY',
  SUNDAY = 'SUNDAY'
}

/**
 * Leave type enum
 */
export enum LeaveType {
  ANNUAL = 'ANNUAL',
  SICK = 'SICK',
  PUBLIC_HOLIDAY = 'PUBLIC_HOLIDAY',
  OTHER = 'OTHER'
}

/**
 * Leave status enum
 */
export enum LeaveStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED'
}

/**
 * Helper to get display label for day of week
 */
export function getDayLabel(day: DayOfWeek): string {
  const labels: Record<DayOfWeek, string> = {
    [DayOfWeek.MONDAY]: 'Monday',
    [DayOfWeek.TUESDAY]: 'Tuesday',
    [DayOfWeek.WEDNESDAY]: 'Wednesday',
    [DayOfWeek.THURSDAY]: 'Thursday',
    [DayOfWeek.FRIDAY]: 'Friday',
    [DayOfWeek.SATURDAY]: 'Saturday',
    [DayOfWeek.SUNDAY]: 'Sunday'
  };
  return labels[day];
}

/**
 * Helper to get display label for leave type
 */
export function getLeaveTypeLabel(type: LeaveType): string {
  const labels: Record<LeaveType, string> = {
    [LeaveType.ANNUAL]: 'Annual Leave',
    [LeaveType.SICK]: 'Sick Leave',
    [LeaveType.PUBLIC_HOLIDAY]: 'Public Holiday',
    [LeaveType.OTHER]: 'Other'
  };
  return labels[type];
}

/**
 * Helper to get display label for leave status
 */
export function getLeaveStatusLabel(status: LeaveStatus): string {
  const labels: Record<LeaveStatus, string> = {
    [LeaveStatus.PENDING]: 'Pending',
    [LeaveStatus.APPROVED]: 'Approved',
    [LeaveStatus.REJECTED]: 'Rejected',
    [LeaveStatus.CANCELLED]: 'Cancelled'
  };
  return labels[status];
}

// ========== Appointment Booking Models ==========

/**
 * Session type for appointments
 */
export interface SessionType {
  id: string;
  code: string;
  name: string;
  description?: string;
}

/**
 * Appointment status enum
 */
export enum AppointmentStatus {
  SCHEDULED = 'SCHEDULED',
  CONFIRMED = 'CONFIRMED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  NO_SHOW = 'NO_SHOW'
}

/**
 * Cancellation type enum
 */
export enum CancellationType {
  CLIENT_INITIATED = 'CLIENT_INITIATED',
  THERAPIST_INITIATED = 'THERAPIST_INITIATED',
  LATE_CANCELLATION = 'LATE_CANCELLATION'
}

/**
 * Appointment entity
 */
export interface Appointment {
  id: string;
  therapistProfileId: string;
  clientId: string;
  sessionType: SessionType;
  startTime: string; // ISO 8601 with timezone
  endTime: string;   // Calculated field
  durationMinutes: number;
  timezone: string;
  status: AppointmentStatus;
  isConflictOverride: boolean;
  cancellationType?: CancellationType;
  cancellationReason?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  rescheduleReason?: string;
  originalStartTime?: string;
  rescheduledAt?: string;
  rescheduledBy?: string;
  notes?: string;
  version: number; // Optimistic locking version
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  // Recurring series fields (PA-33)
  seriesId?: number;
  recurrenceIndex?: number;
  isModified?: boolean;
}

/**
 * Request to create new appointment
 */
export interface CreateAppointmentRequest {
  therapistProfileId: string;
  clientId: string;
  sessionTypeId: string;
  startTime: string; // ISO 8601 with timezone
  durationMinutes: number;
  timezone: string;
  notes?: string;
  allowConflictOverride?: boolean;
}

/**
 * Request to check for appointment conflicts
 */
export interface CheckConflictsRequest {
  therapistProfileId: string;
  startTime: string; // ISO 8601 with timezone
  durationMinutes: number;
}

/**
 * Conflicting appointment details (minimal)
 */
export interface ConflictingAppointment {
  id: string;
  clientId: string;
  clientName?: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
}

/**
 * Conflict check response
 */
export interface ConflictCheckResponse {
  hasConflicts: boolean;
  conflicts: ConflictingAppointment[];
}

/**
 * Request to reschedule an appointment
 */
export interface RescheduleAppointmentRequest {
  newStartTime: string; // ISO 8601 with timezone
  reason: string; // 10-1000 characters
  allowConflictOverride?: boolean;
}

/**
 * Request to cancel an appointment
 */
export interface CancelAppointmentRequest {
  cancellationType: CancellationType;
  reason: string; // 10-1000 characters
}

// ========== Recurring Series Models (PA-33) ==========

/**
 * How often recurring occurrences repeat
 */
export type RecurrenceType = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY';

/**
 * Status of a recurring appointment series
 */
export type SeriesStatus = 'ACTIVE' | 'PARTIALLY_CANCELLED' | 'CANCELLED';

/**
 * Scope of an edit or cancellation on a recurring series
 */
export type EditScope = 'SINGLE' | 'FUTURE_SERIES' | 'ENTIRE_SERIES';

/**
 * How to handle conflicting slots during series creation/edit
 */
export type ConflictResolution = 'SKIP_CONFLICTS' | 'ABORT';

/**
 * Details of a conflicting appointment for recurring slot display
 */
export interface RecurringConflictDetail {
  appointmentId: string;
  clientName?: string;
  startTime: string;
}

/**
 * Result for a single generated slot in a recurring conflict check
 */
export interface RecurringSlotCheckResult {
  index: number;
  startTime: string;
  hasConflict: boolean;
  conflictDetails?: RecurringConflictDetail;
}

/**
 * Response from the recurring conflict pre-flight check
 */
export interface RecurringConflictCheckResponse {
  generatedSlots: RecurringSlotCheckResult[];
  conflictCount: number;
  cleanSlotCount: number;
}

/**
 * Request for the recurring conflict pre-flight check
 */
export interface CheckRecurringConflictsRequest {
  therapistProfileId: string;
  clientId: string;
  sessionTypeId: string;
  startTime: string;
  durationMinutes: number;
  timezone: string;
  recurrenceType: RecurrenceType;
  occurrences: number;
}

/**
 * Request to create a new recurring appointment series
 */
export interface CreateRecurringSeriesRequest {
  therapistProfileId: string;
  clientId: string;
  sessionTypeId: string;
  startTime: string;
  durationMinutes: number;
  timezone: string;
  recurrenceType: RecurrenceType;
  occurrences: number;
  notes?: string;
  conflictResolution: ConflictResolution;
}

/**
 * Response from creating a recurring series
 */
export interface CreateRecurringSeriesResponse {
  seriesId: number;
  requestedOccurrences: number;
  savedOccurrences: number;
  skippedOccurrences: number;
  appointments: Appointment[];
  waitlistEntryIds: number[];
}

/**
 * Request to edit a single occurrence or all future occurrences
 */
export interface EditRecurringOccurrenceRequest {
  editScope: EditScope;
  startTime?: string;
  durationMinutes?: number;
  notes?: string;
  conflictResolution?: ConflictResolution;
}

/**
 * Request to cancel one or more occurrences in a series
 */
export interface CancelRecurringOccurrenceRequest {
  cancelScope: EditScope;
  cancellationReason: string;
  cancellationType: CancellationType;
}

/**
 * Full recurring series including all occurrences
 */
export interface AppointmentSeries {
  seriesId: number;
  recurrenceType: RecurrenceType;
  startDate: string;
  totalOccurrences: number;
  generatedOccurrences: number;
  therapistProfileId: string;
  clientId: string;
  sessionType: SessionType;
  durationMinutes: number;
  timezone: string;
  status: SeriesStatus;
  createdAt: string;
  updatedAt: string;
  version: number;
  appointments: Appointment[];
}

/**
 * Helper to get display label for day of week from JS Date
 */
export function getDayOfWeek(date: Date): DayOfWeek {
  const day = date.getDay();
  const days = [
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
  ];
  return days[day];
}
