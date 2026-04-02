import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { LeaveRequestService } from '../../../../schedule/services/leave-request.service';
import { Leave, LeaveApprovalRequest } from '../../../../schedule/models/schedule.model';

/**
 * Component for managing pending leave requests (admin/reception staff only)
 */
@Component({
  selector: 'app-pending-leave-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  templateUrl: './pending-leave-requests.component.html',
  styleUrls: ['./pending-leave-requests.component.scss']
})
export class PendingLeaveRequestsComponent implements OnInit {
  pendingRequests: Leave[] = [];
  isLoading = false;
  selectedRequest: Leave | null = null;
  adminNotes = '';
  isProcessing = false;

  constructor(
    private leaveRequestService: LeaveRequestService
  ) {}

  ngOnInit(): void {
    this.loadPendingRequests();
  }

  /**
   * Load all pending leave requests
   */
  loadPendingRequests(): void {
    this.isLoading = true;
    this.leaveRequestService.getAllPendingLeaveRequests().subscribe({
      next: (requests) => {
        this.pendingRequests = requests;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load pending requests:', error);
        this.isLoading = false;
      }
    });
  }

  /**
   * Select a leave request to review
   */
  selectRequest(request: Leave): void {
    this.selectedRequest = request;
    this.adminNotes = '';
  }

  /**
   * Approve the selected leave request
   */
  approveRequest(): void {
    if (!this.selectedRequest || !this.selectedRequest.id) return;

    this.isProcessing = true;
    const approvalRequest: LeaveApprovalRequest = {
      adminNotes: this.adminNotes || undefined
    };

    this.leaveRequestService
      .approveLeaveRequestAdmin(this.selectedRequest.id, approvalRequest)
      .subscribe({
        next: () => {
          this.isProcessing = false;
          this.selectedRequest = null;
          this.adminNotes = '';
          this.loadPendingRequests(); // Reload the list
        },
        error: (error) => {
          console.error('Failed to approve request:', error);
          this.isProcessing = false;
          alert('Failed to approve leave request. Please try again.');
        }
      });
  }

  /**
   * Reject the selected leave request
   */
  rejectRequest(): void {
    if (!this.selectedRequest || !this.selectedRequest.id) return;
    if (!this.adminNotes.trim()) {
      alert('Please provide a reason for rejection in the notes field.');
      return;
    }

    this.isProcessing = true;
    const rejectionRequest: LeaveApprovalRequest = {
      adminNotes: this.adminNotes
    };

    this.leaveRequestService
      .rejectLeaveRequestAdmin(this.selectedRequest.id, rejectionRequest)
      .subscribe({
        next: () => {
          this.isProcessing = false;
          this.selectedRequest = null;
          this.adminNotes = '';
          this.loadPendingRequests(); // Reload the list
        },
        error: (error) => {
          console.error('Failed to reject request:', error);
          this.isProcessing = false;
          alert('Failed to reject leave request. Please try again.');
        }
      });
  }

  /**
   * Cancel review selection
   */
  cancelReview(): void {
    this.selectedRequest = null;
    this.adminNotes = '';
  }

  /**
   * Get formatted date range for display
   */
  getDateRange(leave: Leave): string {
    return `${leave.startDate} — ${leave.endDate}`;
  }

  /**
   * Get display name for leave type
   */
  getLeaveTypeDisplay(type: string): string {
    const typeMap: Record<string, string> = {
      'ANNUAL': 'Annual Leave',
      'SICK': 'Sick Leave',
      'PUBLIC_HOLIDAY': 'Public Holiday',
      'OTHER': 'Other'
    };
    return typeMap[type] || type;
  }
}
