import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Leave,
  LeaveRequest,
  LeaveApprovalRequest,
  ConflictWarning,
  LeaveStatus
} from '../models/schedule.model';

/**
 * Service for managing therapist leave requests
 */
@Injectable({ providedIn: 'root' })
export class LeaveRequestService {
  private readonly apiBase = '/api/v1/therapists';
  private readonly adminApiBase = '/api/v1/admin/leave';

  constructor(private http: HttpClient) {}

  /**
   * Submit a leave request
   */
  submitLeaveRequest(
    therapistProfileId: string,
    request: LeaveRequest
  ): Observable<Leave> {
    return this.http.post<Leave>(
      `${this.apiBase}/${therapistProfileId}/leave`,
      request
    );
  }

  /**
   * Submit leave request for current user (therapist)
   */
  submitMyLeaveRequest(request: LeaveRequest): Observable<Leave> {
    return this.http.post<Leave>(`${this.apiBase}/me/leave`, request);
  }

  /**
   * Get leave periods for a therapist
   */
  getLeaveRequests(
    therapistProfileId: string,
    status?: LeaveStatus
  ): Observable<Leave[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Leave[]>(
      `${this.apiBase}/${therapistProfileId}/leave`,
      { params }
    );
  }

  /**
   * Get current user's leave requests
   */
  getMyLeaveRequests(status?: LeaveStatus): Observable<Leave[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Leave[]>(`${this.apiBase}/me/leave`, { params });
  }

  /**
   * Approve or reject a leave request
   */
  approveLeaveRequest(
    therapistProfileId: string,
    leaveId: string,
    request: LeaveApprovalRequest
  ): Observable<Leave> {
    return this.http.put<Leave>(
      `${this.apiBase}/${therapistProfileId}/leave/${leaveId}/approve`,
      request
    );
  }

  /**
   * Cancel a leave request
   */
  cancelLeaveRequest(
    therapistProfileId: string,
    leaveId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBase}/${therapistProfileId}/leave/${leaveId}`
    );
  }

  /**
   * Cancel current user's leave request
   */
  cancelMyLeaveRequest(leaveId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/me/leave/${leaveId}`);
  }

  /**
   * Check for appointment conflicts before submitting leave
   */
  checkConflicts(
    therapistProfileId: string,
    startDate: string,
    endDate: string
  ): Observable<ConflictWarning> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<ConflictWarning>(
      `${this.apiBase}/${therapistProfileId}/leave/conflicts`,
      { params }
    );
  }

  /**
   * Check conflicts for current user
   */
  checkMyConflicts(
    startDate: string,
    endDate: string
  ): Observable<ConflictWarning> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<ConflictWarning>(
      `${this.apiBase}/me/leave/conflicts`,
      { params }
    );
  }

  // ========== Admin Operations ==========

  /**
   * Get all pending leave requests across all therapists (admin only)
   */
  getAllPendingLeaveRequests(): Observable<Leave[]> {
    return this.http.get<Leave[]>(`${this.adminApiBase}/pending`);
  }

  /**
   * Approve a leave request (admin only)
   */
  approveLeaveRequestAdmin(
    leaveId: string,
    request: LeaveApprovalRequest
  ): Observable<Leave> {
    return this.http.put<Leave>(
      `${this.adminApiBase}/${leaveId}/approve`,
      request
    );
  }

  /**
   * Reject a leave request (admin only)
   */
  rejectLeaveRequestAdmin(
    leaveId: string,
    request: LeaveApprovalRequest
  ): Observable<Leave> {
    return this.http.put<Leave>(
      `${this.adminApiBase}/${leaveId}/reject`,
      request
    );
  }
}
