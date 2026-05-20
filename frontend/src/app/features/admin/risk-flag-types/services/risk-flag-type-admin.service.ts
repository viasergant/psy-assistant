import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RiskFlagType } from '../models/risk-flag-type-admin.model';

/**
 * HTTP client for the admin risk flag type endpoints.
 * Requires MANAGE_RISK_FLAG_TYPES authority (SYSTEM_ADMINISTRATOR only).
 *
 * Authentication tokens are appended by the global JwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class RiskFlagTypeAdminService {
  private readonly baseUrl = '/api/v1/admin/risk-flag-types';

  constructor(private http: HttpClient) {}

  /**
   * Returns all risk flag types (active and inactive), ordered by displayOrder.
   * GET /api/v1/admin/risk-flag-types
   */
  listAll(): Observable<RiskFlagType[]> {
    return this.http.get<RiskFlagType[]>(this.baseUrl);
  }

  /**
   * Creates a new risk flag type.
   * POST /api/v1/admin/risk-flag-types
   */
  create(name: string, displayOrder: number): Observable<RiskFlagType> {
    return this.http.post<RiskFlagType>(this.baseUrl, { name, displayOrder });
  }

  /**
   * Deactivates an existing risk flag type by ID.
   * PATCH /api/v1/admin/risk-flag-types/{id}/deactivate
   */
  deactivate(id: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
