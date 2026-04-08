import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  OutcomeMeasureChartData,
  OutcomeMeasureDefinition,
  OutcomeMeasureEntry,
  RecordOutcomeMeasureRequest,
} from '../models/care-plan.model';

@Injectable({ providedIn: 'root' })
export class OutcomeMeasureService {
  private readonly base = '/api/v1';

  constructor(private http: HttpClient) {}

  getDefinitions(): Observable<OutcomeMeasureDefinition[]> {
    return this.http.get<OutcomeMeasureDefinition[]>(`${this.base}/outcome-measure-definitions`);
  }

  recordEntry(
    planId: string,
    request: RecordOutcomeMeasureRequest
  ): Observable<OutcomeMeasureEntry> {
    return this.http.post<OutcomeMeasureEntry>(
      `${this.base}/care-plans/${planId}/outcome-measures`,
      request
    );
  }

  getEntries(planId: string, measureCode?: string): Observable<OutcomeMeasureEntry[]> {
    let params = new HttpParams();
    if (measureCode) {
      params = params.set('measureCode', measureCode);
    }
    return this.http.get<OutcomeMeasureEntry[]>(
      `${this.base}/care-plans/${planId}/outcome-measures`,
      { params }
    );
  }

  getChartData(planId: string, measureCode: string): Observable<OutcomeMeasureChartData> {
    const params = new HttpParams().set('measureCode', measureCode);
    return this.http.get<OutcomeMeasureChartData>(
      `${this.base}/care-plans/${planId}/outcome-measures/chart-data`,
      { params }
    );
  }
}
