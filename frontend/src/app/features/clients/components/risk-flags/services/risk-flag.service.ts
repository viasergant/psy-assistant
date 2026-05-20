import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateRiskFlagPayload,
  ResolveRiskFlagPayload,
  RiskFlag,
  RiskFlagType,
} from '../models/risk-flag.model';

/**
 * HTTP client for the risk flags endpoints.
 *
 * Authentication tokens are appended by the global JwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class RiskFlagService {
  private readonly clientsBase = '/api/v1/clients';
  private readonly typesBase = '/api/v1/risk-flag-types';

  constructor(private http: HttpClient) {}

  /**
   * Returns all ACTIVE risk flags for a given client.
   * Requires READ_RISK_FLAGS permission.
   * The clinicalNote field is null when the caller lacks READ_RISK_FLAG_NOTES.
   */
  listActive(clientId: string): Observable<RiskFlag[]> {
    return this.http.get<RiskFlag[]>(`${this.clientsBase}/${clientId}/risk-flags`);
  }

  /**
   * Returns the full flag history (ACTIVE and RESOLVED) for a given client.
   * Requires READ_RISK_FLAG_NOTES permission (supervisor / system admin only).
   */
  listAll(clientId: string): Observable<RiskFlag[]> {
    return this.http.get<RiskFlag[]>(`${this.clientsBase}/${clientId}/risk-flags/history`);
  }

  /**
   * Creates a new ACTIVE risk flag for the given client.
   * Requires MANAGE_RISK_FLAGS permission.
   */
  create(clientId: string, payload: CreateRiskFlagPayload): Observable<RiskFlag> {
    return this.http.post<RiskFlag>(`${this.clientsBase}/${clientId}/risk-flags`, payload);
  }

  /**
   * Resolves an existing risk flag.
   * Requires MANAGE_RISK_FLAGS permission.
   */
  resolve(clientId: string, flagId: string, payload: ResolveRiskFlagPayload): Observable<RiskFlag> {
    return this.http.patch<RiskFlag>(
      `${this.clientsBase}/${clientId}/risk-flags/${flagId}/resolve`,
      payload
    );
  }

  /**
   * Returns all active risk flag types available for selection in the create-flag form.
   * Available to all authenticated users.
   */
  listTypes(): Observable<RiskFlagType[]> {
    return this.http.get<RiskFlagType[]>(this.typesBase);
  }
}
