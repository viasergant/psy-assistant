import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  RecurringSchedule,
  RecurringScheduleRequest,
  ScheduleOverride,
  ScheduleOverrideRequest,
  ScheduleSummary,
  DayOfWeek
} from '../models/schedule.model';

/**
 * Service for managing therapist schedules (recurring and overrides)
 */
@Injectable({ providedIn: 'root' })
export class ScheduleApiService {
  private readonly apiBase = '/api/v1/therapists';

  constructor(private http: HttpClient) {}

  /**
   * Get complete schedule summary for a therapist
   */
  getScheduleSummary(therapistProfileId: string): Observable<ScheduleSummary> {
    return this.http.get<ScheduleSummary>(
      `${this.apiBase}/${therapistProfileId}/schedule`
    );
  }

  /**
   * Get current user's schedule (therapist viewing own schedule)
   */
  getMySchedule(): Observable<ScheduleSummary> {
    return this.http.get<ScheduleSummary>(`${this.apiBase}/me/schedule`);
  }

  /**
   * Create recurring schedule for a day of week
   */
  createRecurringSchedule(
    therapistProfileId: string,
    request: RecurringScheduleRequest
  ): Observable<RecurringSchedule> {
    return this.http.post<RecurringSchedule>(
      `${this.apiBase}/${therapistProfileId}/schedule/recurring`,
      request
    );
  }

  /**
   * Update recurring schedule entry
   */
  updateRecurringSchedule(
    therapistProfileId: string,
    scheduleId: string,
    request: RecurringScheduleRequest
  ): Observable<RecurringSchedule> {
    return this.http.put<RecurringSchedule>(
      `${this.apiBase}/${therapistProfileId}/schedule/recurring/${scheduleId}`,
      request
    );
  }

  /**
   * Delete recurring schedule entry
   */
  deleteRecurringSchedule(
    therapistProfileId: string,
    scheduleId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBase}/${therapistProfileId}/schedule/recurring/${scheduleId}`
    );
  }

  /**
   * Create schedule override for a specific date
   */
  createScheduleOverride(
    therapistProfileId: string,
    request: ScheduleOverrideRequest
  ): Observable<ScheduleOverride> {
    return this.http.post<ScheduleOverride>(
      `${this.apiBase}/${therapistProfileId}/schedule/overrides`,
      request
    );
  }

  /**
   * Update schedule override
   */
  updateScheduleOverride(
    therapistProfileId: string,
    overrideId: string,
    request: ScheduleOverrideRequest
  ): Observable<ScheduleOverride> {
    return this.http.put<ScheduleOverride>(
      `${this.apiBase}/${therapistProfileId}/schedule/overrides/${overrideId}`,
      request
    );
  }

  /**
   * Delete schedule override
   */
  deleteScheduleOverride(
    therapistProfileId: string,
    overrideId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBase}/${therapistProfileId}/schedule/overrides/${overrideId}`
    );
  }

  /**
   * Get recurring schedule for specific day of week
   */
  getRecurringScheduleForDay(
    therapistProfileId: string,
    dayOfWeek: DayOfWeek
  ): Observable<RecurringSchedule[]> {
    const params = new HttpParams().set('dayOfWeek', dayOfWeek);
    return this.http.get<RecurringSchedule[]>(
      `${this.apiBase}/${therapistProfileId}/schedule/recurring`,
      { params }
    );
  }

  /**
   * Get schedule overrides for date range
   */
  getScheduleOverrides(
    therapistProfileId: string,
    startDate: string,
    endDate: string
  ): Observable<ScheduleOverride[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<ScheduleOverride[]>(
      `${this.apiBase}/${therapistProfileId}/schedule/overrides`,
      { params }
    );
  }
}
