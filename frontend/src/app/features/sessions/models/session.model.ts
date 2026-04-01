/**
 * Session record entity
 */
export interface SessionRecord {
  id: number;
  appointmentId: number;
  clientId: number;
  clientName: string;
  therapistId: number;
  sessionDate: string; // ISO date (YYYY-MM-DD)
  scheduledStartTime: string; // ISO time (HH:mm:ss)
  sessionType: SessionType;
  plannedDuration: number; // minutes
  status: SessionStatus;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Session type enumeration
 */
export enum SessionType {
  INITIAL_CONSULTATION = 'INITIAL_CONSULTATION',
  FOLLOW_UP = 'FOLLOW_UP',
  CHECK_IN = 'CHECK_IN',
  CRISIS_INTERVENTION = 'CRISIS_INTERVENTION'
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
 * Filters for querying session records
 */
export interface SessionFilters {
  clientId?: number;
  startDate?: string; // ISO date
  endDate?: string; // ISO date
  status?: SessionStatus[];
}
