import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ClientDetail,
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
}
