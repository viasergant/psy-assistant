import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ClientDetail } from '../models/client.model';

/**
 * HTTP client for the clients CRM endpoints.
 *
 * Authentication tokens are appended by the global JwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly base = '/api/v1/clients';

  constructor(private http: HttpClient) {}

  /** Returns the full detail for a single client. */
  getClient(id: string): Observable<ClientDetail> {
    return this.http.get<ClientDetail>(`${this.base}/${id}`);
  }
}
