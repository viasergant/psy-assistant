import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CarePlanAuditEntry,
  CarePlanDetail,
  CarePlanStatus,
  CarePlanSummary,
  CreateCarePlanRequest,
  CreateGoalRequest,
  CreateInterventionRequest,
  CreateMilestoneRequest,
  GoalResponse,
  InterventionResponse,
  MilestoneResponse,
  UpdateCarePlanRequest,
  UpdateGoalStatusRequest,
} from '../models/care-plan.model';

@Injectable({ providedIn: 'root' })
export class CarePlanService {
  private readonly base = '/api/v1';

  constructor(private http: HttpClient) {}

  listByClient(clientId: string, status?: CarePlanStatus): Observable<CarePlanSummary[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<CarePlanSummary[]>(`${this.base}/clients/${clientId}/care-plans`, { params });
  }

  create(clientId: string, request: CreateCarePlanRequest): Observable<CarePlanDetail> {
    return this.http.post<CarePlanDetail>(`${this.base}/clients/${clientId}/care-plans`, request);
  }

  getDetail(planId: string): Observable<CarePlanDetail> {
    return this.http.get<CarePlanDetail>(`${this.base}/care-plans/${planId}`);
  }

  update(planId: string, request: UpdateCarePlanRequest): Observable<CarePlanDetail> {
    return this.http.put<CarePlanDetail>(`${this.base}/care-plans/${planId}`, request);
  }

  close(planId: string): Observable<CarePlanDetail> {
    return this.http.post<CarePlanDetail>(`${this.base}/care-plans/${planId}/close`, {});
  }

  archive(planId: string): Observable<CarePlanDetail> {
    return this.http.post<CarePlanDetail>(`${this.base}/care-plans/${planId}/archive`, {});
  }

  getAudit(planId: string): Observable<CarePlanAuditEntry[]> {
    return this.http.get<CarePlanAuditEntry[]>(`${this.base}/care-plans/${planId}/audit`);
  }

  addGoal(planId: string, request: CreateGoalRequest): Observable<GoalResponse> {
    return this.http.post<GoalResponse>(`${this.base}/care-plans/${planId}/goals`, request);
  }

  updateGoalStatus(planId: string, goalId: string, request: UpdateGoalStatusRequest): Observable<GoalResponse> {
    return this.http.patch<GoalResponse>(`${this.base}/care-plans/${planId}/goals/${goalId}/status`, request);
  }

  removeGoal(planId: string, goalId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/care-plans/${planId}/goals/${goalId}`);
  }

  addIntervention(planId: string, goalId: string, request: CreateInterventionRequest): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.base}/care-plans/${planId}/goals/${goalId}/interventions`, request);
  }

  removeIntervention(planId: string, goalId: string, interventionId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/care-plans/${planId}/goals/${goalId}/interventions/${interventionId}`);
  }

  addMilestone(planId: string, goalId: string, request: CreateMilestoneRequest): Observable<MilestoneResponse> {
    return this.http.post<MilestoneResponse>(`${this.base}/care-plans/${planId}/goals/${goalId}/milestones`, request);
  }

  achieveMilestone(planId: string, goalId: string, milestoneId: string): Observable<MilestoneResponse> {
    return this.http.post<MilestoneResponse>(
      `${this.base}/care-plans/${planId}/goals/${goalId}/milestones/${milestoneId}/achieve`, {}
    );
  }

  removeMilestone(planId: string, goalId: string, milestoneId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/care-plans/${planId}/goals/${goalId}/milestones/${milestoneId}`);
  }

  getInterventionTypes(): Observable<string[]> {
    return this.http.get<string[]>('/api/v1/config/care-plan-intervention-types');
  }
}
