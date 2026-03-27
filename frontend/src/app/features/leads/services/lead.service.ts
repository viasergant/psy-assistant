import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ConvertLeadPayload,
  ConvertLeadResponse,
  CreateLeadPayload,
  LeadDetail,
  LeadListParams,
  LeadPage,
  TransitionStatusPayload,
  UpdateLeadPayload,
} from '../models/lead.model';

/**
 * HTTP client for the leads CRM endpoints.
 *
 * All paths are relative to /api/v1/leads; authentication tokens
 * are appended by the global JwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class LeadService {
  private readonly base = '/api/v1/leads';

  constructor(private http: HttpClient) {}

  /** Returns a paginated, optionally-filtered list of leads. */
  listLeads(params: LeadListParams): Observable<LeadPage> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString())
      .set('sort', params.sort);

    if (params.status !== undefined) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.ownerId !== undefined) {
      httpParams = httpParams.set('ownerId', params.ownerId);
    }
    if (params.includeArchived !== undefined) {
      httpParams = httpParams.set('includeArchived', params.includeArchived.toString());
    }

    return this.http.get<LeadPage>(this.base, { params: httpParams });
  }

  /** Returns the full detail for a single lead. */
  getLead(id: string): Observable<LeadDetail> {
    return this.http.get<LeadDetail>(`${this.base}/${id}`);
  }

  /** Creates a new lead. */
  createLead(payload: CreateLeadPayload): Observable<LeadDetail> {
    return this.http.post<LeadDetail>(this.base, payload);
  }

  /** Fully updates a lead's editable fields (replaces contact methods). */
  updateLead(id: string, payload: UpdateLeadPayload): Observable<LeadDetail> {
    return this.http.put<LeadDetail>(`${this.base}/${id}`, payload);
  }

  /** Transitions the lead to the given status. */
  transitionStatus(id: string, payload: TransitionStatusPayload): Observable<LeadDetail> {
    return this.http.patch<LeadDetail>(`${this.base}/${id}/status`, payload);
  }

  /** Archives the lead (sets status to INACTIVE). */
  archiveLead(id: string): Observable<LeadDetail> {
    return this.http.patch<LeadDetail>(`${this.base}/${id}/archive`, {});
  }

  /**
   * Converts a QUALIFIED lead to a client record.
   * Returns 201 ConvertLeadResponse on success.
   * Throws HttpErrorResponse with status 409 if already converted, 422 if not QUALIFIED.
   */
  convertLead(id: string, payload: ConvertLeadPayload): Observable<ConvertLeadResponse> {
    return this.http.post<ConvertLeadResponse>(`${this.base}/${id}/convert`, payload);
  }
}
