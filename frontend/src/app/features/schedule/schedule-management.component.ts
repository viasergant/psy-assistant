import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScheduleCalendarComponent } from './components/schedule-calendar/schedule-calendar.component';
import { ScheduleApiService } from './services/schedule-api.service';
import { AuthService } from '../../core/auth/auth.service';
import {
  getCurrentUserRole,
  getCurrentTherapistProfileId,
  isSystemAdmin,
  canEditSchedule
} from './guards/schedule.guard';
import { ScheduleSummary } from './models/schedule.model';

@Component({
  selector: 'app-schedule-management',
  standalone: true,
  imports: [CommonModule, ScheduleCalendarComponent],
  template: `
    <div class="schedule-management">
      <header class="page-header">
        <div class="header-content">
          <h1>Schedule Management</h1>
          <p class="subtitle" *ngIf="therapistName">{{ therapistName }}'s Schedule</p>
        </div>
        <div class="header-actions">
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
  therapistProfileId: string | null = null;
  schedule: ScheduleSummary | null = null;
  therapistName = '';
  canEdit = false;
  loading = false;
  error: string | null = null;

  constructor(
    private scheduleApiService: ScheduleApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
  }

  private initializeComponent(): void {
    const role = getCurrentUserRole(this.authService);
    const currentProfileId = getCurrentTherapistProfileId(this.authService);

    if (isSystemAdmin(this.authService)) {
      // Admin can select any therapist (for now show their own if they have one)
      this.therapistProfileId = currentProfileId;
      this.canEdit = true;
    } else if (role === 'THERAPIST' || role === 'RECEPTION_ADMIN_STAFF') {
      // Therapist views their own schedule
      this.therapistProfileId = currentProfileId;
      this.canEdit = canEditSchedule(this.authService, currentProfileId ?? undefined);
    }

    if (this.therapistProfileId) {
      this.loadSchedule();
    } else {
      this.error = 'Unable to determine therapist profile';
    }
  }

  loadSchedule(): void {
    if (!this.therapistProfileId) return;

    this.loading = true;
    this.error = null;

    this.scheduleApiService.getMySchedule().subscribe({
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
    // TODO: Open configuration side panel
    console.log('Open config panel');
  }

  openLeaveRequestModal(): void {
    // TODO: Open leave request modal
    console.log('Open leave request modal');
  }
}
