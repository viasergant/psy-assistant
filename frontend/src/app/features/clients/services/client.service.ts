import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ClientDetail,
  ClientPageResponse,
  ClientSearchResult,
  TimelineEvent,
  UpdateClientProfilePayload,
  UpdateClientTagsPayload,
} from '../models/client.model';

interface ClientSummary {
  id: string;
  name: string;
}

/**
 * HTTP client for the clients CRM endpoints.
 *
 * Authentication tokens are appended by the global JwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly base = '/api/v1/clients';

  constructor(private http: HttpClient) {}

  /** Returns a list of all clients as lightweight summaries for dropdowns. */
  getAllClients(): Observable<ClientSummary[]> {
    return this.http.get<ClientSummary[]>(this.base);
  }

  /** Returns the full detail for a single client. */
  getClient(id: string): Observable<ClientDetail> {
    return this.http.get<ClientDetail>(`${this.base}/${id}`);
  }

  /** Replaces slice-one profile fields for a single client. */
  updateClient(id: string, payload: UpdateClientProfilePayload): Observable<ClientDetail> {
    return this.http.put<ClientDetail>(`${this.base}/${id}`, payload);
  }

  /** Replaces all tags for a single client. */
  updateTags(id: string, payload: UpdateClientTagsPayload): Observable<ClientDetail> {
    return this.http.patch<ClientDetail>(`${this.base}/${id}/tags`, payload);
  }

  /** Uploads or replaces the client profile photo. */
  uploadPhoto(id: string, version: number, file: File): Observable<ClientDetail> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('version', String(version));
    return this.http.post<ClientDetail>(`${this.base}/${id}/photo`, formData);
  }

  /** Downloads a client profile photo as Blob through authenticated HttpClient flow. */
  getPhoto(id: string): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/photo`, { responseType: 'blob' });
  }

  /**
   * Searches clients by name, email, phone, code, or tags.
   * Results are ordered by relevance.
   *
   * @param query Search term (case-insensitive)
   * @param limit Maximum number of results (default 10, max 50)
   */
  searchClients(query: string, limit: number = 10): Observable<ClientSearchResult[]> {
    const params = new HttpParams()
      .set('q', query)
      .set('limit', limit.toString());
    return this.http.get<ClientSearchResult[]>(`${this.base}/search`, { params });
  }

  /**
   * Returns the activity timeline for a specific client.
   * Aggregates appointments, profile changes, and conversion history.
   *
   * @param clientId Client UUID
   * @param eventTypes Optional event type filter
   * @param page Page number (0-based)
   * @param size Page size (default 50, max 100)
   */
  getTimeline(
    clientId: string,
    eventTypes?: string[],
    page: number = 0,
    size: number = 50
  ): Observable<TimelineEvent[]> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (eventTypes && eventTypes.length > 0) {
      eventTypes.forEach(type => {
        params = params.append('eventTypes', type);
      });
    }

    return this.http.get<TimelineEvent[]>(`${this.base}/${clientId}/timeline`, { params });
  }

  /**
   * Returns a paginated, filterable client list for the /clients list page.
   *
   * @param page         zero-based page index
   * @param size         page size (1–100)
   * @param sort         sort field: fullName | createdAt
   * @param dir          sort direction: asc | desc
   * @param q            optional text filter
   * @param tags         optional tag filter (OR semantics)
   * @param therapistId  optional assigned therapist UUID
   */
  listClients(
    page: number = 0,
    size: number = 20,
    sort: string = 'fullName',
    dir: string = 'asc',
    q?: string,
    tags?: string[],
    therapistId?: string
  ): Observable<ClientPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort)
      .set('dir', dir);

    if (q) {
      params = params.set('q', q);
    }
    if (tags && tags.length > 0) {
      tags.forEach(tag => {
        params = params.append('tags', tag);
      });
    }
    if (therapistId) {
      params = params.set('therapistId', therapistId);
    }

    return this.http.get<ClientPageResponse>(`${this.base}/list`, { params });
  }

  /** Returns all distinct tag values across all clients, sorted alphabetically. */
  getAllTags(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/tags`);
  }
}
