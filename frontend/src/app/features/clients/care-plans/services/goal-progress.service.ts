import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateProgressNoteRequest,
  GoalProgressHistory,
  GoalProgressNote,
} from '../models/care-plan.model';

@Injectable({ providedIn: 'root' })
export class GoalProgressService {
  private readonly base = '/api/v1';

  constructor(private http: HttpClient) {}

  addProgressNote(
    planId: string,
    goalId: string,
    request: CreateProgressNoteRequest
  ): Observable<GoalProgressNote> {
    return this.http.post<GoalProgressNote>(
      `${this.base}/care-plans/${planId}/goals/${goalId}/progress-notes`,
      request
    );
  }

  getProgressNotes(planId: string, goalId: string): Observable<GoalProgressNote[]> {
    return this.http.get<GoalProgressNote[]>(
      `${this.base}/care-plans/${planId}/goals/${goalId}/progress-notes`
    );
  }

  getProgressHistory(planId: string, goalId: string): Observable<GoalProgressHistory> {
    return this.http.get<GoalProgressHistory>(
      `${this.base}/care-plans/${planId}/goals/${goalId}/progress-history`
    );
  }
}
