import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ClientRetentionRow,
  LeadConversionRow,
  NoShowCancellationRow,
  PagedReportResponse,
  ReportFilter,
  ReportType,
  RevenueRow,
  SessionTypeOption,
  TherapistOption,
  TherapistUtilizationRow,
} from '../models/report.models';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly base = '/api/v1/reports';
  private readonly therapistsBase = '/api/v1/therapists';
  private readonly catalogBase = '/api/v1/billing/catalog';

  constructor(private http: HttpClient) {}

  getLeadConversion(
    filter: ReportFilter
  ): Observable<PagedReportResponse<LeadConversionRow>> {
    return this.http.get<PagedReportResponse<LeadConversionRow>>(
      `${this.base}/lead-conversion/data`,
      { params: this.buildParams(filter) }
    );
  }

  getTherapistUtilization(
    filter: ReportFilter
  ): Observable<PagedReportResponse<TherapistUtilizationRow>> {
    return this.http.get<PagedReportResponse<TherapistUtilizationRow>>(
      `${this.base}/therapist-utilization/data`,
      { params: this.buildParams(filter) }
    );
  }

  getRevenue(
    filter: ReportFilter
  ): Observable<PagedReportResponse<RevenueRow>> {
    return this.http.get<PagedReportResponse<RevenueRow>>(
      `${this.base}/revenue/data`,
      { params: this.buildParams(filter) }
    );
  }

  getClientRetention(
    filter: ReportFilter
  ): Observable<PagedReportResponse<ClientRetentionRow>> {
    return this.http.get<PagedReportResponse<ClientRetentionRow>>(
      `${this.base}/client-retention/data`,
      { params: this.buildParams(filter) }
    );
  }

  getNoShowCancellation(
    filter: ReportFilter
  ): Observable<PagedReportResponse<NoShowCancellationRow>> {
    return this.http.get<PagedReportResponse<NoShowCancellationRow>>(
      `${this.base}/no-show-cancellation/data`,
      { params: this.buildParams(filter) }
    );
  }

  exportReport(type: ReportType, filter: ReportFilter, format: 'csv' | 'xlsx'): void {
    const params = this.buildParams({ ...filter, page: 0 });
    params.append('format', format);
    const url = `${this.base}/${type}/export?${params.toString()}&format=${format}`;
    window.open(url, '_self');
  }

  getTherapists(): Observable<{ content: TherapistOption[] }> {
    const params = new HttpParams().set('size', '200');
    return this.http.get<{ content: TherapistOption[] }>(this.therapistsBase, { params });
  }

  getSessionTypes(): Observable<SessionTypeOption[]> {
    return this.http.get<SessionTypeOption[]>(this.catalogBase);
  }

  private buildParams(filter: ReportFilter): HttpParams {
    let params = new HttpParams()
      .set('dateFrom', filter.dateFrom)
      .set('dateTo', filter.dateTo);

    if (filter.therapistId) params = params.set('therapistId', filter.therapistId);
    if (filter.sessionTypeId) params = params.set('sessionTypeId', filter.sessionTypeId);
    if (filter.leadSource) params = params.set('leadSource', filter.leadSource);
    if (filter.page !== undefined) params = params.set('page', filter.page.toString());
    if (filter.size !== undefined) params = params.set('size', filter.size.toString());

    return params;
  }
}
