import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScheduleCalendarComponent } from './components/schedule-calendar/schedule-calendar.component';
import { ScheduleConfigPanelComponent } from './components/schedule-config-panel/schedule-config-panel.component';
import { LeaveRequestDialogComponent } from './components/leave-request-dialog/leave-request-dialog.component';
import { ScheduleApiService } from './services/schedule-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { TherapistManagementService } from '../admin/therapists/services/therapist-management.service';
import {
  getCurrentUserRole,
  getCurrentTherapistProfileId,
  isSystemAdmin,
  canEditSchedule
} from './guards/schedule.guard';
import { ScheduleSummary, Leave } from './models/schedule.model';
import { TherapistProfile } from '../admin/therapists/models/therapist.model';

@Component({
  selector: 'app-schedule-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ScheduleCalendarComponent,
    ScheduleConfigPanelComponent,
    LeaveRequestDialogComponent
  ],
  template: `
    <div class="schedule-management">
      <header class="page-header">
        <div class="header-content">
          <h1>Schedule Management</h1>
          <p class="subtitle" *ngIf="therapistName">{{ therapistName }}'s Schedule</p>
        </div>
        <div class="header-actions">
          <!-- Admin therapist selector -->
          <select
            *ngIf="isAdmin"
            [(ngModel)]="selectedTherapistId"
            (ngModelChange)="onTherapistChange()"
            class="therapist-selector"
          >
            <option [ngValue]="null">Select a therapist...</option>
            <option *ngFor="let therapist of therapists" [ngValue]="therapist.id">
              {{ therapist.name }}
            </option>
          </select>

          <button
            *ngIf="canEdit"
            type="button"
            class="btn-primary"
            (click)="openConfigPanel()"
          >
            Configure Schedule
          </button>
          <button
            type="button"
            class="btn-secondary"
            (click)="openLeaveRequestModal()"
          >
            Request Leave
          </button>
        </div>
      </header>

      <div class="schedule-content">
        <app-schedule-calendar
          *ngIf="schedule && therapistProfileId"
          [therapistProfileId]="therapistProfileId"
          [schedule]="schedule"
          [editable]="canEdit"
        ></app-schedule-calendar>

        <div *ngIf="loading" class="loading-state">
          <div class="spinner"></div>
          <p>Loading schedule...</p>
        </div>

        <div *ngIf="error" class="error-state">
          <p class="error-message">{{ error }}</p>
          <button type="button" class="btn-secondary" (click)="loadSchedule()">
            Retry
          </button>
        </div>
      </div>

      <!-- Configuration Panel -->
      <app-schedule-config-panel
        *ngIf="showConfigPanel && therapistProfileId"
        [therapistProfileId]="therapistProfileId"
        [timezone]="schedule?.timezone || 'America/New_York'"
        (close)="closeConfigPanel()"
        (saved)="onConfigSaved()"
      ></app-schedule-config-panel>

      <!-- Leave Request Dialog -->
      <app-leave-request-dialog
        *ngIf="showLeaveRequestModal && therapistProfileId"
        [therapistProfileId]="therapistProfileId"
        (submitted)="onLeaveRequestSubmitted($event)"
        (cancelled)="closeLeaveRequestModal()"
      ></app-leave-request-dialog>
    </div>
  `,
  styles: [
    `
      .schedule-management {
        display: flex;
        flex-direction: column;
        height: 100%;
        background: var(--color-bg);
      }

      .page-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 2rem;
        background: var(--color-surface);
        border-bottom: 1px solid var(--color-border);
      }

      .header-content h1 {
        font-size: 1.75rem;
        font-weight: 700;
        color: var(--color-text-primary);
        margin: 0 0 0.25rem 0;
      }

      .subtitle {
        font-size: 0.875rem;
        color: var(--color-text-secondary);
        margin: 0;
      }

      .header-actions {
        display: flex;
        gap: 1rem;
        align-items: center;
      }

      .therapist-selector {
        padding: 0.625rem 1rem;
        border: 1px solid var(--color-border);
        border-radius: 6px;
        background: var(--color-surface);
        color: var(--color-text-primary);
        font-size: 0.875rem;
        cursor: pointer;
        min-width: 200px;

        &:focus {
          outline: none;
          border-color: var(--color-accent);
        }
      }

      .btn-primary,
      .btn-secondary {
        padding: 0.625rem 1.25rem;
        border-radius: 6px;
        font-size: 0.875rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
        border: none;
      }

      .btn-primary {
        background: var(--color-accent);
        color: white;

        &:hover {
          background: var(--color-accent-hover);
        }
      }

      .btn-secondary {
        background: transparent;
        color: var(--color-text-primary);
        border: 1px solid var(--color-border);

        &:hover {
          background: var(--color-bg);
        }
      }

      .schedule-content {
        flex: 1;
        padding: 2rem;
        overflow: auto;
      }

      .loading-state,
      .error-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 4rem 2rem;
      }

      .spinner {
        width: 2rem;
        height: 2rem;
        border: 3px solid var(--color-border);
        border-top-color: var(--color-accent);
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
        margin-bottom: 1rem;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .error-message {
        color: var(--color-error);
        margin-bottom: 1rem;
      }
    `
  ]
})
export class ScheduleManagementComponent implements OnInit {
  @ViewChild(ScheduleCalendarComponent) calendarComponent?: ScheduleCalendarComponent;
  
  therapistProfileId: string | null = null;
  selectedTherapistId: string | null = null;
  schedule: ScheduleSummary | null = null;
  therapistName = '';
  therapists: TherapistProfile[] = [];
  isAdmin = false;
  canEdit = false;
  loading = false;
  error: string | null = null;
  showConfigPanel = false;
  showLeaveRequestModal = false;

  constructor(
    private scheduleApiService: ScheduleApiService,
    private therapistManagementService: TherapistManagementService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
  }

  private initializeComponent(): void {
    const role = getCurrentUserRole(this.authService);
    const currentProfileId = getCurrentTherapistProfileId(this.authService);

    this.isAdmin = isSystemAdmin(this.authService);

    if (this.isAdmin) {
      // Admin can select any therapist - load therapist list
      this.canEdit = true;
      this.loadTherapists();
    } else if (role === 'THERAPIST' || role === 'RECEPTION_ADMIN_STAFF') {
      // Therapist views their own schedule
      this.therapistProfileId = currentProfileId;
      this.canEdit = canEditSchedule(this.authService, currentProfileId ?? undefined);

      if (this.therapistProfileId) {
        this.loadSchedule();
      } else {
        this.error = 'Unable to determine therapist profile';
      }
    }
  }

  private loadTherapists(): void {
    this.loading = true;
    this.error = null;

    this.therapistManagementService.getTherapists(0, 100).subscribe({
      next: page => {
        this.therapists = page.content;
        this.loading = false;
        
        // Auto-select first therapist if available
        if (this.therapists.length > 0) {
          this.selectedTherapistId = this.therapists[0].id;
          this.onTherapistChange();
        } else {
          this.error = 'No therapists found in the system';
        }
      },
      error: err => {
        console.error('Error loading therapists:', err);
        this.error = 'Failed to load therapist list';
        this.loading = false;
      }
    });
  }

  onTherapistChange(): void {
    if (this.selectedTherapistId && this.selectedTherapistId !== 'null') {
      this.therapistProfileId = this.selectedTherapistId;
      this.loadSchedule();
    } else {
      // User selected "Select a therapist..." - clear schedule
      this.therapistProfileId = null;
      this.schedule = null;
      this.therapistName = '';
    }
  }

  loadSchedule(): void {
    if (!this.therapistProfileId) return;

    this.loading = true;
    this.error = null;

    // If admin, use getScheduleSummary with therapistProfileId
    // If therapist, use getMySchedule
    const scheduleRequest = this.isAdmin
      ? this.scheduleApiService.getScheduleSummary(this.therapistProfileId)
      : this.scheduleApiService.getMySchedule();

    scheduleRequest.subscribe({
      next: schedule => {
        this.schedule = schedule;
        this.therapistName = schedule.therapistName;
        this.loading = false;
      },
      error: err => {
        console.error('Error loading schedule:', err);
        this.error = 'Failed to load schedule. Please try again.';
        this.loading = false;
      }
    });
  }

  openConfigPanel(): void {
    this.showConfigPanel = true;
  }

  closeConfigPanel(): void {
    this.showConfigPanel = false;
  }

  onConfigSaved(): void {
    console.log('Config saved - reloading schedule and calendar');
    // Reload schedule after configuration changes
    this.loadSchedule();
    
    // Wait a moment for DB transaction to commit, then reload calendar
    // This ensures the availability query sees the new recurring schedule
    console.log('Triggering calendar reload with slight delay...');
    setTimeout(() => {
      console.log('Now reloading calendar, calendarComponent exists:', !!this.calendarComponent);
      this.calendarComponent?.loadAvailability();
    }, 500);
  }

  openLeaveRequestModal(): void {
    if (!this.therapistProfileId) {
      console.warn('Cannot open leave request modal: no therapist profile ID');
      return;
    }
    this.showLeaveRequestModal = true;
  }

  closeLeaveRequestModal(): void {
    this.showLeaveRequestModal = false;
  }

  onLeaveRequestSubmitted(leave: Leave): void {
    console.log('Leave request submitted:', leave);
    this.showLeaveRequestModal = false;
    
    // Reload schedule to reflect the new leave request
    this.loadSchedule();
    
    // Reload calendar grid to show the leave request
    setTimeout(() => {
      this.calendarComponent?.loadAvailability();
    }, 500);
    
    // Optionally show a success message
    // You could add a toast notification here in the future
    alert(`Leave request submitted successfully! Your request is now pending approval.`);
  }
}
