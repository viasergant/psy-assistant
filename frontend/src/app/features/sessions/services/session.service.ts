import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
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
        params = params.set('clientId', filters.clientId);
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

    return this.http.get<any[]>(this.base, { params }).pipe(
      map(sessions => sessions.map(s => this.transformSessionRecord(s)))
    );
  }

  /**
   * Starts a pending session, transitioning it to IN_PROGRESS status.
   *
   * @param sessionId Session record UUID
   * @returns Observable of updated session record
   */
  startSession(sessionId: string): Observable<SessionRecord> {
    return this.http.post<any>(`${this.base}/${sessionId}/start`, {}).pipe(
      map(s => this.transformSessionRecord(s))
    );
  }

  /**
   * Completes an in-progress session with notes and optional actual end time.
   *
   * @param sessionId Session record UUID
   * @param request Session notes and optional actual end time
   * @returns Observable of updated session record
   */
  completeSession(
    sessionId: string,
    request: CompleteSessionRequest
  ): Observable<SessionRecord> {
    return this.http.post<any>(`${this.base}/${sessionId}/complete`, request).pipe(
      map(s => this.transformSessionRecord(s))
    );
  }

  /**
   * Cancels a session with a required cancellation reason.
   *
   * @param sessionId Session record UUID
   * @param reason Cancellation reason
   * @returns Observable of updated session record
   */
  cancelSession(sessionId: string, reason: string): Observable<SessionRecord> {
    return this.http.post<any>(`${this.base}/${sessionId}/cancel`, { reason }).pipe(
      map(s => this.transformSessionRecord(s))
    );
  }

  /**
   * Transforms backend session record response to frontend model.
   * Converts ISO 8601 duration (PT1H30M) to minutes.
   */
  private transformSessionRecord(record: any): SessionRecord {
    try {
      const plannedDuration = this.parseDurationToMinutes(record.plannedDuration);
      console.log('Transforming session record:', {
        id: record.id,
        rawDuration: record.plannedDuration,
        parsedDuration: plannedDuration
      });
      return {
        ...record,
        plannedDuration,
      };
    } catch (error) {
      console.error('Error transforming session record:', error, record);
      throw error;
    }
  }

  /**
   * Parses ISO 8601 duration string (e.g., 'PT1H30M', 'PT45M') to minutes.
   */
  private parseDurationToMinutes(duration: string | any): number {
    if (!duration) {
      console.warn('No duration provided');
      return 0;
    }

    // Handle if duration is already a number (backend sends seconds as a plain number)
    if (typeof duration === 'number') {
      return Math.round(duration / 60);
    }

    if (typeof duration !== 'string') {
      console.warn('Duration is not a string:', duration);
      return 0;
    }

    const regex = /PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/;
    const matches = duration.match(regex);

    if (!matches) {
      console.warn('Duration does not match expected format:', duration);
      return 0;
    }

    const hours = parseInt(matches[1] || '0', 10);
    const minutes = parseInt(matches[2] || '0', 10);
    const seconds = parseFloat(matches[3] || '0');

    return hours * 60 + minutes + Math.round(seconds / 60);
  }
}

