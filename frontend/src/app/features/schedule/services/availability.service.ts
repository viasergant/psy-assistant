import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AvailabilitySlot } from '../models/schedule.model';

/**
 * Service for querying therapist availability
 */
@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  private readonly apiBase = '/api/v1/therapists';

  constructor(private http: HttpClient) {}

  /**
   * Query available slots for a therapist in a date range
   */
  getAvailableSlots(
    therapistProfileId: string,
    startDate: string,
    endDate: string
  ): Observable<AvailabilitySlot[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<AvailabilitySlot[]>(
      `${this.apiBase}/${therapistProfileId}/availability`,
      { params }
    );
  }

  /**
   * Query available slots for current user (therapist viewing own availability)
   */
  getMyAvailableSlots(
    startDate: string,
    endDate: string
  ): Observable<AvailabilitySlot[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<AvailabilitySlot[]>(
      `${this.apiBase}/me/availability`,
      { params }
    );
  }
}
