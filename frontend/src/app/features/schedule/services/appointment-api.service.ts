import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Appointment,
  CreateAppointmentRequest,
  CheckConflictsRequest,
  ConflictCheckResponse,
  SessionType,
  RescheduleAppointmentRequest,
  CancelAppointmentRequest
} from '../models/schedule.model';

/**
 * Service for appointment booking operations
 */
@Injectable({ providedIn: 'root' })
export class AppointmentApiService {
  private readonly apiBase = '/api/v1/appointments';

  constructor(private http: HttpClient) {}

  /**
   * Create a new appointment
   *
   * @param request appointment creation request
   * @returns created appointment
   * @throws ConflictError (409) if conflicts exist and override not allowed
   */
  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(this.apiBase, request);
  }

  /**
   * Check for appointment conflicts before creating
   *
   * @param request conflict check request
   * @returns conflict check result with list of conflicting appointments
   */
  checkConflicts(request: CheckConflictsRequest): Observable<ConflictCheckResponse> {
    return this.http.post<ConflictCheckResponse>(
      `${this.apiBase}/check-conflicts`,
      request
    );
  }

  /**
   * Get all active session types for dropdown
   *
   * @returns list of session types (filtered to active only)
   */
  getSessionTypes(): Observable<SessionType[]> {
    return this.http.get<SessionType[]>(`${this.apiBase}/session-types`);
  }

  /**
   * Get appointment by ID
   *
   * @param appointmentId appointment UUID
   * @returns appointment details
   */
  getAppointment(appointmentId: string): Observable<Appointment> {
    return this.http.get<Appointment>(`${this.apiBase}/${appointmentId}`);
  }

  /**
   * Get all appointments for a therapist within a date range
   *
   * @param therapistProfileId therapist UUID
   * @param startDate start date (format: yyyy-MM-dd)
   * @param endDate end date (format: yyyy-MM-dd)
   * @returns list of appointments ordered by start time
   */
  getTherapistAppointments(
    therapistProfileId: string,
    startDate: string,
    endDate: string
  ): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(
      `${this.apiBase}/therapist/${therapistProfileId}`,
      {
        params: { startDate, endDate }
      }
    );
  }

  /**
   * Get all appointments for a client
   *
   * @param clientId client UUID
   * @returns list of appointments (most recent first)
   */
  getClientAppointments(clientId: string): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apiBase}/client/${clientId}`);
  }

  /**
   * Reschedule an existing appointment
   *
   * @param appointmentId appointment UUID
   * @param request reschedule request with new time and reason
   * @returns updated appointment
   * @throws EntityNotFound (404) if appointment doesn't exist
   * @throws IllegalState (400) if appointment is already cancelled
   * @throws ConflictError (409) if conflicts exist and override not allowed
   */
  rescheduleAppointment(
    appointmentId: string,
    request: RescheduleAppointmentRequest
  ): Observable<Appointment> {
    return this.http.put<Appointment>(
      `${this.apiBase}/${appointmentId}/reschedule`,
      request
    );
  }

  /**
   * Cancel an existing appointment
   *
   * @param appointmentId appointment UUID
   * @param request cancellation request with type and reason
   * @returns updated appointment
   * @throws EntityNotFound (404) if appointment doesn't exist
   * @throws IllegalState (400) if appointment is already cancelled
   */
  cancelAppointment(
    appointmentId: string,
    request: CancelAppointmentRequest
  ): Observable<Appointment> {
    return this.http.put<Appointment>(
      `${this.apiBase}/${appointmentId}/cancel`,
      request
    );
  }
}
