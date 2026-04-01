import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CompleteSessionRequest,
  SessionFilters,
  SessionRecord,
} from '../models/session.model';

/**
 * HTTP client for session management endpoints.
 *
 * Authentication tokens are appended by the global JwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly base = '/api/sessions';

  constructor(private http: HttpClient) {}

  /**
   * Retrieves session records with optional filters.
   *
   * @param filters Optional query filters (clientId, date range, status)
   * @returns Observable of session records array
   */
  getSessions(filters?: SessionFilters): Observable<SessionRecord[]> {
    let params = new HttpParams();

    if (filters) {
      if (filters.clientId !== undefined) {
        params = params.set('clientId', filters.clientId.toString());
      }
      if (filters.startDate) {
        params = params.set('startDate', filters.startDate);
      }
      if (filters.endDate) {
        params = params.set('endDate', filters.endDate);
      }
      if (filters.status && filters.status.length > 0) {
        params = params.set('status', filters.status.join(','));
      }
    }

    return this.http.get<SessionRecord[]>(this.base, { params });
  }

  /**
   * Starts a pending session, transitioning it to IN_PROGRESS status.
   *
   * @param sessionId Session record ID
   * @returns Observable of updated session record
   */
  startSession(sessionId: number): Observable<SessionRecord> {
    return this.http.post<SessionRecord>(`${this.base}/${sessionId}/start`, {});
  }

  /**
   * Completes an in-progress session with notes and optional actual end time.
   *
   * @param sessionId Session record ID
   * @param request Session notes and optional actual end time
   * @returns Observable of updated session record
   */
  completeSession(
    sessionId: number,
    request: CompleteSessionRequest
  ): Observable<SessionRecord> {
    return this.http.post<SessionRecord>(`${this.base}/${sessionId}/complete`, request);
  }

  /**
   * Cancels a session with a required cancellation reason.
   *
   * @param sessionId Session record ID
   * @param reason Cancellation reason
   * @returns Observable of updated session record
   */
  cancelSession(sessionId: number, reason: string): Observable<SessionRecord> {
    return this.http.post<SessionRecord>(`${this.base}/${sessionId}/cancel`, { reason });
  }
}
