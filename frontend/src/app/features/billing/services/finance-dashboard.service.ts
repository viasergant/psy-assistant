import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FinanceDashboard } from '../models/finance-dashboard.model';

@Injectable({ providedIn: 'root' })
export class FinanceDashboardService {
  private readonly base = '/api/v1/finance';

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<FinanceDashboard> {
    return this.http.get<FinanceDashboard>(`${this.base}/dashboard`);
  }
}
