/**
 * Session record kind discriminator
 */
export enum RecordKind {
  INDIVIDUAL = 'INDIVIDUAL',
  GROUP = 'GROUP'
}

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
 * A single participant in a group session record
 */
export interface GroupSessionParticipant {
  participantId: string; // UUID of session_participant row
  clientId: string; // UUID
  clientName: string;
  joinedAt: string; // ISO instant
  removedAt?: string; // ISO instant — null means still active
  attendanceOutcome?: AttendanceOutcome;
}

/**
 * Session record entity
 */
export interface SessionRecord {
  id: string; // UUID
  appointmentId: string; // UUID
  recordKind: RecordKind;
  clientId?: string; // UUID — null for GROUP sessions
  clientName?: string; // null for GROUP sessions
  therapistId: string; // UUID
  sessionDate: string; // ISO date (YYYY-MM-DD)
  scheduledStartTime: string; // ISO time (HH:mm:ss)
  sessionType: SessionTypeInfo;
  plannedDuration: number; // minutes (converted from ISO 8601 duration)
  status: SessionStatus;
  cancellationReason?: string;
  sessionNotes?: string;
  actualEndTime?: string; // ISO time (HH:mm:ss)
  attendanceOutcome?: AttendanceOutcome; // INDIVIDUAL sessions only
  cancelledAt?: string; // ISO instant
  cancellationInitiatorId?: string; // UUID
  createdAt: string;
  updatedAt: string;
  participants: GroupSessionParticipant[]; // empty for INDIVIDUAL, populated for GROUP
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
 * Request payload for recording per-client attendance within a group session
 */
export interface RecordGroupAttendanceRequest {
  outcome: AttendanceOutcome;
  cancelledAt?: string; // ISO instant
  note?: string;
}

/**
 * Filters for querying session records
 */
export interface SessionFilters {
  clientId?: string; // UUID
  startDate?: string; // ISO date
  endDate?: string; // ISO date
  status?: SessionStatus[];
  recordKind?: RecordKind;
}
