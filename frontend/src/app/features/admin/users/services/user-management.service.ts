import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateUserPayload,
  PatchUserPayload,
  UserListParams,
  UserPage,
  UserSummary,
} from '../models/user.model';

/**
 * HTTP client for the admin user management endpoints.
 *
 * All paths are relative to /api/v1/admin/users; authentication tokens
 * are appended by the global JwtInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly base = '/api/v1/admin/users';

  constructor(private http: HttpClient) {}

  /**
   * Returns a paginated, optionally-filtered list of users.
   *
   * @param params page, size, sort, and optional role/status filters
   */
  listUsers(params: UserListParams): Observable<UserPage> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString())
      .set('sort', params.sort);

    if (params.role !== undefined) {
      httpParams = httpParams.set('role', params.role);
    }
    if (params.active !== undefined) {
      httpParams = httpParams.set('active', params.active.toString());
    }

    return this.http.get<UserPage>(this.base, { params: httpParams });
  }

  /**
   * Creates a new user account.  No initial password is set; a reset link is
   * issued server-side automatically.
   *
   * @param payload email, fullName, role
   */
  createUser(payload: CreateUserPayload): Observable<UserSummary> {
    return this.http.post<UserSummary>(this.base, payload);
  }

  /**
   * Applies a partial update to a user (role, full name, or active status).
   *
   * @param id     target user UUID
   * @param patch  fields to update; all optional
   */
  updateUser(id: string, patch: PatchUserPayload): Observable<UserSummary> {
    return this.http.patch<UserSummary>(`${this.base}/${id}`, patch);
  }

  /**
   * Initiates an admin password reset for the target user.
   * A 24-hour single-use reset link is generated server-side and sent by email.
   *
   * @param id target user UUID
   */
  initiatePasswordReset(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/password-reset`, {});
  }
}
