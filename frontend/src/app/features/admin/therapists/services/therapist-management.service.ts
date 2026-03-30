import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  TherapistProfile,
  TherapistPage,
  CreateTherapistRequest,
  UpdateTherapistRequest,
  EmploymentStatus
} from '../models/therapist.model';

/**
 * Service for therapist profile management.
 * Communicates with /api/v1/therapists endpoints.
 */
@Injectable({
  providedIn: 'root'
})
export class TherapistManagementService {
  private readonly baseUrl = '/api/v1/therapists';

  constructor(private http: HttpClient) {}

  /**
   * Retrieves all specializations for dropdowns.
   */
  getSpecializations(): Observable<{ id: string; name: string }[]> {
    return this.http.get<{ id: string; name: string }[]>('/api/v1/specializations');
  }

  /**
   * Retrieves all languages for dropdowns.
   */
  getLanguages(): Observable<{ id: string; name: string }[]> {
    return this.http.get<{ id: string; name: string }[]>('/api/v1/languages');
  }

  /**
   * Fetches a paginated list of therapists.
   * @param page zero-based page number
   * @param size page size
   * @param employmentStatus optional filter by employment status
   * @param active optional filter by active status
   */
  getTherapists(
    page: number = 0,
    size: number = 20,
    employmentStatus?: EmploymentStatus,
    active?: boolean
  ): Observable<TherapistPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (employmentStatus) {
      params = params.set('employmentStatus', employmentStatus);
    }
    if (active !== undefined) {
      params = params.set('active', active.toString());
    }

    return this.http.get<TherapistPage>(this.baseUrl, { params });
  }

  /**
   * Retrieves a single therapist profile by ID.
   */
  getTherapist(id: string): Observable<TherapistProfile> {
    return this.http.get<TherapistProfile>(`${this.baseUrl}/${id}`);
  }

  /**
   * Retrieves a single therapist profile by email address.
   */
  getTherapistByEmail(email: string): Observable<TherapistProfile> {
    return this.http.get<TherapistProfile>(`${this.baseUrl}/by-email/${encodeURIComponent(email)}`);
  }

  /**
   * Creates a new therapist profile.
   */
  createTherapist(request: CreateTherapistRequest): Observable<TherapistProfile> {
    return this.http.post<TherapistProfile>(this.baseUrl, request);
  }

  /**
   * Updates an existing therapist profile.
   */
  updateTherapist(id: string, request: UpdateTherapistRequest): Observable<TherapistProfile> {
    return this.http.patch<TherapistProfile>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Toggles the active status of a therapist.
   */
  toggleActive(id: string, currentVersion: number): Observable<TherapistProfile> {
    // Note: Backend would need a dedicated endpoint for this.
    // For now, we'll use the update endpoint.
    // This is a simplified implementation - adjust based on backend.
    return this.http.patch<TherapistProfile>(`${this.baseUrl}/${id}/toggle-active`, {
      version: currentVersion
    });
  }
}
