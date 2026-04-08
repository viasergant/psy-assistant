export interface CaseloadRow {
  therapistProfileId: string;
  therapistName: string;
  activeClientCount: number;
  sessionsThisWeek: number;
  sessionsThisMonth: number;
  scheduledHoursThisWeek: number | null;
  contractedHoursPerWeek: number | null;
  utilizationRate: number | null;
}

export interface CaseloadPage {
  content: CaseloadRow[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface TherapistClientRow {
  clientId: string;
  clientName: string;
  completedSessionCount: number;
  nextScheduledSession: string | null;
  clientStatus: string;
}

export interface TherapistClientPage {
  content: TherapistClientRow[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CaseloadFilters {
  specializations?: string[];
  snapshotDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}
