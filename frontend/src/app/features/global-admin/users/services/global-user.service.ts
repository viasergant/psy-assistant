import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { GlobalUserListParams, GlobalUserPage } from '../models/global-user.model';

/** HTTP client for the global administrator user list endpoint. */
@Injectable({ providedIn: 'root' })
export class GlobalUserService {
  private readonly base = '/api/v1/global-admin/users';

  constructor(private http: HttpClient) {}

  /**
   * Returns a paginated, optionally filtered list of users across all organizations.
   *
   * @param params page, size, sort and optional role/status/search filters
   */
  listUsers(params: GlobalUserListParams): Observable<GlobalUserPage> {
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
    const search = params.search?.trim();
    if (search) {
      httpParams = httpParams.set('search', search);
    }

    return this.http.get<GlobalUserPage>(this.base, { params: httpParams });
  }
}
