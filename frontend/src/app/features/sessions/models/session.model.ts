/**
 * Attendance outcome enumeration
 */
export enum AttendanceOutcome {
  ATTENDED = 'ATTENDED',
  NO_SHOW = 'NO_SHOW',
  LATE_CANCELLATION = 'LATE_CANCELLATION',
  CANCELLED = 'CANCELLED',
  THERAPIST_CANCELLATION = 'THERAPIST_CANCELLATION'
}

/**
 * Session type information from backend
 */
export interface SessionTypeInfo {
  id: string;
  code: string;
  name: string;
  description?: string;
}

/**
 * Session record entity
 */
export interface SessionRecord {
  id: string; // UUID
  appointmentId: string; // UUID
  clientId: string; // UUID
  clientName: string;
  therapistId: string; // UUID
  sessionDate: string; // ISO date (YYYY-MM-DD)
  scheduledStartTime: string; // ISO time (HH:mm:ss)
  sessionType: SessionTypeInfo;
  plannedDuration: number; // minutes (converted from ISO 8601 duration)
  status: SessionStatus;
  cancellationReason?: string;
  sessionNotes?: string;
  actualEndTime?: string; // ISO time (HH:mm:ss)
  attendanceOutcome?: AttendanceOutcome;
  cancelledAt?: string; // ISO instant
  cancellationInitiatorId?: string; // UUID
  createdAt: string;
  updatedAt: string;
}

/**
 * Session status enumeration
 */
export enum SessionStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

/**
 * Request payload for completing a session
 */
export interface CompleteSessionRequest {
  sessionNotes: string;
  actualEndTime?: string; // ISO time (HH:mm:ss)
}

/**
 * Request payload for recording an attendance outcome
 */
export interface RecordAttendanceRequest {
  outcome: AttendanceOutcome;
  cancelledAt?: string; // ISO instant
  note?: string;
}

/**
 * Response from recording an attendance outcome
 */
export interface AttendanceOutcomeResponse {
  sessionId: string;
  effectiveOutcome: AttendanceOutcome;
  requestedOutcome: AttendanceOutcome;
  cancelledAt?: string;
  updatedAt: string;
}

/**
 * Filters for querying session records
 */
export interface SessionFilters {
  clientId?: string; // UUID
  startDate?: string; // ISO date
  endDate?: string; // ISO date
  status?: SessionStatus[];
}
