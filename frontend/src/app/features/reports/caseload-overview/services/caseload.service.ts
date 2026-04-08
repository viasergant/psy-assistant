import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CaseloadFilters, CaseloadPage, TherapistClientPage } from '../models/caseload.model';

@Injectable({ providedIn: 'root' })
export class CaseloadService {
  private readonly base = '/api/v1/caseload';

  constructor(private http: HttpClient) {}

  listCaseload(filters: CaseloadFilters): Observable<CaseloadPage> {
    let params = new HttpParams();

    if (filters.specializations && filters.specializations.length > 0) {
      params = params.set('specializations', filters.specializations.join(','));
    }
    if (filters.snapshotDate) {
      params = params.set('snapshotDate', filters.snapshotDate);
    }
    if (filters.page !== undefined) {
      params = params.set('page', filters.page.toString());
    }
    if (filters.size !== undefined) {
      params = params.set('size', filters.size.toString());
    }
    if (filters.sort) {
      params = params.set('sort', filters.sort);
    }

    return this.http.get<CaseloadPage>(this.base, { params });
  }

  getTherapistClients(therapistProfileId: string, page: number, size: number): Observable<TherapistClientPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<TherapistClientPage>(`${this.base}/${therapistProfileId}/clients`, { params });
  }
}
