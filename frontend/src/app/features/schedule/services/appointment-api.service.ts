import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Appointment,
  AppointmentSeries,
  CancelAppointmentRequest,
  CancelRecurringOccurrenceRequest,
  CheckConflictsRequest,
  CheckRecurringConflictsRequest,
  ConflictCheckResponse,
  CreateAppointmentRequest,
  CreateRecurringSeriesRequest,
  CreateRecurringSeriesResponse,
  EditRecurringOccurrenceRequest,
  RecurringConflictCheckResponse,
  RescheduleAppointmentRequest,
  SessionType
} from '../models/schedule.model';

/**
 * Service for appointment booking and recurring series operations
 */
@Injectable({ providedIn: 'root' })
export class AppointmentApiService {
  private readonly apiBase = '/api/v1/appointments';
  private readonly seriesBase = '/api/v1/recurring-series';

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

  /**
   * Update appointment status
   *
   * @param appointmentId appointment UUID
   * @param request status update request
   * @returns updated appointment
   * @throws EntityNotFound (404) if appointment doesn't exist
   * @throws IllegalArgument (400) if invalid status transition
   */
  updateAppointmentStatus(
    appointmentId: string,
    request: { status: string; notes?: string }
  ): Observable<Appointment> {
    return this.http.patch<Appointment>(
      `${this.apiBase}/${appointmentId}/status`,
      request
    );
  }

  // ========== Recurring Series Methods (PA-33) ==========

  /**
   * Pre-flight conflict check for all slots in a recurring series
   *
   * @param request conflict check parameters
   * @returns per-slot conflict results with aggregate counts
   */
  checkRecurringConflicts(
    request: CheckRecurringConflictsRequest
  ): Observable<RecurringConflictCheckResponse> {
    return this.http.post<RecurringConflictCheckResponse>(
      `${this.seriesBase}/check-conflicts`,
      request
    );
  }

  /**
   * Create a new recurring appointment series
   *
   * @param request series creation parameters including conflict resolution
   * @returns summary of saved and skipped occurrences
   */
  createRecurringSeries(
    request: CreateRecurringSeriesRequest
  ): Observable<CreateRecurringSeriesResponse> {
    return this.http.post<CreateRecurringSeriesResponse>(this.seriesBase, request);
  }

  /**
   * Get a recurring series with all its appointments
   *
   * @param seriesId series ID
   * @returns series details including all occurrences
   */
  getRecurringSeries(seriesId: number): Observable<AppointmentSeries> {
    return this.http.get<AppointmentSeries>(`${this.seriesBase}/${seriesId}`);
  }

  /**
   * Edit a single occurrence or all future occurrences of a series
   *
   * @param seriesId parent series ID
   * @param appointmentId the appointment being edited
   * @param request edit parameters including scope
   * @returns the edited appointment
   */
  editRecurringOccurrence(
    seriesId: number,
    appointmentId: string,
    request: EditRecurringOccurrenceRequest
  ): Observable<Appointment> {
    return this.http.patch<Appointment>(
      `${this.seriesBase}/${seriesId}/appointments/${appointmentId}`,
      request
    );
  }

  /**
   * Cancel one or more occurrences in a recurring series
   *
   * @param seriesId parent series ID
   * @param appointmentId the appointment being cancelled (anchor)
   * @param request cancellation parameters including scope
   * @returns void (204 No Content)
   */
  cancelRecurringOccurrence(
    seriesId: number,
    appointmentId: string,
    request: CancelRecurringOccurrenceRequest
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.seriesBase}/${seriesId}/appointments/${appointmentId}`,
      { body: request }
    );
  }
}
