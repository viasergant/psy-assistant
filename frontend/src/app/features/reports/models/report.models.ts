export type ReportType =
  | 'lead-conversion'
  | 'therapist-utilization'
  | 'revenue'
  | 'client-retention'
  | 'no-show-cancellation';

export interface ReportFilter {
  dateFrom: string;
  dateTo: string;
  therapistId?: string;
  sessionTypeId?: string;
  leadSource?: string;
  page?: number;
  size?: number;
}

export interface PagedReportResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

export interface LeadConversionRow {
  leadSource: string;
  totalLeads: number;
  convertedLeads: number;
  conversionRate: number;
}

export interface TherapistUtilizationRow {
  therapistName: string;
  therapistProfileId: string;
  bookedMinutes: number;
  availableMinutes: number;
  utilizationPct: number;
}

export interface RevenueRow {
  month: string;
  therapistName: string;
  therapistId: string;
  invoicedTotal: number;
  paidTotal: number;
  outstandingAmount: number;
}

export interface ClientRetentionRow {
  activeCount: number;
  newCount: number;
  churnedCount: number;
  retentionRate: number;
}

export interface NoShowCancellationRow {
  therapistName: string;
  therapistId: string;
  totalScheduled: number;
  noShowCount: number;
  noShowRate: number;
  cancellationCount: number;
  cancellationRate: number;
}

export interface TherapistOption {
  id: string;
  fullName: string;
}

export interface SessionTypeOption {
  id: string;
  name: string;
}
