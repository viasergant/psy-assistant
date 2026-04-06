/**
 * Calendar view models for day/week/month appointment display.
 * Maps to backend CalendarWeekViewResponse DTO.
 */

export interface CalendarAppointmentBlock {
  id: string;
  therapistProfileId: string;
  therapistName: string;
  clientId: string;
  clientName: string;
  sessionTypeCode: string;
  sessionTypeName: string;
  startTime: string; // ISO 8601 string
  endTime: string; // ISO 8601 string
  durationMinutes: number;
  status: AppointmentStatus;
  isModified: boolean;
  notes: string | null;
}

export interface CalendarWeekViewResponse {
  weekStart: string; // 'YYYY-MM-DD'
  weekEnd: string; // 'YYYY-MM-DD'
  therapists: Record<string, TherapistInfo>;
  appointments: CalendarAppointmentBlock[];
}

export interface TherapistInfo {
  id: string;
  name: string;
  specialization: string;
}

export type AppointmentStatus =
  | 'SCHEDULED'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW';

export type CalendarViewMode = 'day' | 'week' | 'month';

export interface CalendarFilters {
  therapistIds: string[];
  sessionTypes: string[];
  statuses: AppointmentStatus[];
}

// Configuration for color coding by session type (PA-32 requirement)
export const SESSION_TYPE_COLORS: Record<string, string> = {
  INDIVIDUAL: '#4CAF50', // Green
  GROUP: '#2196F3', // Blue
  INTAKE: '#FF9800', // Orange
  FOLLOW_UP: '#9C27B0', // Purple
  ONLINE: '#00BCD4', // Cyan
  IN_PERSON: '#795548' // Brown
};

// Status color mapping for legend
export const STATUS_COLORS: Record<AppointmentStatus, string> = {
  SCHEDULED: '#FFC107',   // Amber
  CONFIRMED: '#4CAF50',   // Green
  IN_PROGRESS: '#2196F3', // Blue
  COMPLETED: '#9E9E9E',   // Grey
  CANCELLED: '#F44336',   // Red
  NO_SHOW: '#FF5722'      // Deep Orange
};
