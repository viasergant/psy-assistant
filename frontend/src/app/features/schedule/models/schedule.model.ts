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
 */
export interface LeaveApprovalRequest {
  reviewerUserId: string; // UUID of admin/staff approving/rejecting
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
 * Helper to get day of week from JS Date
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
