import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ScheduleConfigPanelComponent } from './components/schedule-config-panel/schedule-config-panel.component';
import { LeaveRequestDialogComponent } from './components/leave-request-dialog/leave-request-dialog.component';
import { CalendarWeekViewComponent } from './components/calendar/calendar-week-view/calendar-week-view.component';
import { CalendarDayViewComponent } from './components/calendar/calendar-day-view/calendar-day-view.component';
import { CalendarMonthViewComponent } from './components/calendar/calendar-month-view/calendar-month-view.component';
import { CalendarFacadeService } from './services/calendar-facade.service';
import { ScheduleApiService } from './services/schedule-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { TranslocoPipe } from '@jsverse/transloco';
import { TherapistManagementService } from '../admin/therapists/services/therapist-management.service';
import { ClientService } from '../clients/services/client.service';
import {
  getCurrentUserRole,
  getCurrentTherapistProfileId,
  isSystemAdmin,
  canEditSchedule
} from './guards/schedule.guard';
import { ScheduleSummary, Leave } from './models/schedule.model';
import { TherapistProfile } from '../admin/therapists/models/therapist.model';
import { CalendarWeekViewResponse, CalendarAppointmentBlock, CalendarViewMode } from './models/calendar.model';

interface ClientOption {
  id: string;
  name: string;
}

@Component({
  selector: 'app-schedule-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslocoPipe,
    ScheduleConfigPanelComponent,
    LeaveRequestDialogComponent,
    CalendarWeekViewComponent,
    CalendarDayViewComponent,
    CalendarMonthViewComponent
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

      <!-- Calendar Navigation Toolbar -->
      <div class="calendar-nav-toolbar">
        <div class="nav-section-left">
          <div class="date-navigation">
            <button
              type="button"
              class="nav-button"
              (click)="onPreviousPeriod()"
              [attr.aria-label]="'schedule.calendar.previous' | transloco"
            >←</button>

            <button
              type="button"
              class="today-button btn-secondary"
              (click)="onToday()"
            >{{ 'schedule.calendar.today' | transloco }}</button>

            <button
              type="button"
              class="nav-button"
              (click)="onNextPeriod()"
              [attr.aria-label]="'schedule.calendar.next' | transloco"
            >→</button>
          </div>

          <div class="period-display">
            <h2>{{ getCurrentPeriodLabel() }}</h2>
          </div>
        </div>

        <div class="view-mode-selector" role="group" [attr.aria-label]="'schedule.calendar.viewMode' | transloco">
          <button
            type="button"
            class="view-button"
            [class.active]="viewMode === 'day'"
            (click)="setViewMode('day')"
          >{{ 'schedule.calendar.dayView' | transloco }}</button>
          <button
            type="button"
            class="view-button"
            [class.active]="viewMode === 'week'"
            (click)="setViewMode('week')"
          >{{ 'schedule.calendar.weekView' | transloco }}</button>
          <button
            type="button"
            class="view-button"
            [class.active]="viewMode === 'month'"
            (click)="setViewMode('month')"
          >{{ 'schedule.calendar.monthView' | transloco }}</button>
        </div>
      </div>

      <!-- Calendar View Container -->
      <div class="calendar-view-container">
        <div *ngIf="calendarLoading" class="loading-state">
          <div class="spinner"></div>
          <p>{{ 'schedule.calendar.loading' | transloco }}</p>
        </div>

        <div *ngIf="calendarError && !calendarLoading" class="error-state">
          <p class="error-message">{{ calendarError }}</p>
          <button type="button" class="btn-secondary" (click)="onCalendarRetry()">
            {{ 'schedule.calendar.retry' | transloco }}
          </button>
        </div>

        <app-calendar-day-view
          *ngIf="!calendarLoading && !calendarError && viewMode === 'day'"
          [dayDate]="currentDate"
          [appointments]="getAllAppointments()"
          [therapists]="getTherapistsList()"
          (appointmentClicked)="onAppointmentClick($event)"
        ></app-calendar-day-view>

        <app-calendar-week-view
          *ngIf="!calendarLoading && !calendarError && viewMode === 'week'"
          [weekData]="weekData"
          (appointmentClicked)="onAppointmentClick($event)"
        ></app-calendar-week-view>

        <app-calendar-month-view
          *ngIf="!calendarLoading && !calendarError && viewMode === 'month'"
          [monthDate]="currentDate"
          [appointments]="getAllAppointments()"
          (dayClicked)="onMonthDayClick($event)"
        ></app-calendar-month-view>
      </div>

      <!-- Schedule loading/error (management data) -->
      <div *ngIf="loading" class="management-loading">
        <div class="spinner"></div>
        <p>Loading schedule data...</p>
      </div>
      <div *ngIf="error && !loading" class="error-state management-error">
        <p class="error-message">{{ error }}</p>
        <button type="button" class="btn-secondary" (click)="loadSchedule()">
          Retry
        </button>
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

      /* Calendar Navigation Toolbar */
      .calendar-nav-toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 2rem;
        background: var(--color-surface);
        border-bottom: 1px solid var(--color-border);
        gap: 1rem;
      }

      .nav-section-left {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .date-navigation {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }

      .nav-button {
        width: 36px;
        height: 36px;
        border: 1px solid var(--color-border);
        background: var(--color-surface);
        border-radius: 6px;
        cursor: pointer;
        font-size: 1.125rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.15s ease;

        &:hover {
          background: var(--color-bg);
          border-color: var(--color-accent);
        }
      }

      .period-display h2 {
        margin: 0;
        font-size: 1.125rem;
        font-weight: 600;
        color: var(--color-text-primary);
      }

      .view-mode-selector {
        display: flex;
        border: 1px solid var(--color-border);
        border-radius: 6px;
        overflow: hidden;
      }

      .view-button {
        padding: 0.5rem 1rem;
        border: none;
        border-right: 1px solid var(--color-border);
        background: var(--color-surface);
        cursor: pointer;
        font-size: 0.875rem;
        font-weight: 500;
        transition: all 0.15s ease;

        &:last-child {
          border-right: none;
        }

        &:hover {
          background: var(--color-bg);
        }

        &.active {
          background: var(--color-accent);
          color: white;
        }
      }

      /* Calendar View Container */
      .calendar-view-container {
        flex: 1;
        overflow: hidden;
        position: relative;
      }

      .management-loading,
      .management-error {
        padding: 1rem 2rem;
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

      /* Calendar navigation toolbar */
      .calendar-nav-toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 2rem;
        background: var(--color-surface);
        border-bottom: 1px solid var(--color-border);
        gap: 1rem;
      }

      .nav-section-left {
        display: flex;
        align-items: center;
        gap: 1.25rem;
      }

      .date-navigation {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }

      .nav-button {
        width: 36px;
        height: 36px;
        border: 1px solid var(--color-border);
        background: var(--color-surface);
        border-radius: var(--radius-sm, 6px);
        cursor: pointer;
        font-size: 1.125rem;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.15s ease;
        color: var(--color-text-primary);

        &:hover {
          background: var(--color-bg);
          border-color: var(--color-accent);
        }
      }

      .today-button {
        height: 36px;
        padding: 0 0.875rem;
        font-size: 0.875rem;
      }

      .period-display h2 {
        font-size: 1.125rem;
        font-weight: 600;
        color: var(--color-text-primary);
        margin: 0;
      }

      .view-mode-selector {
        display: flex;
        border: 1px solid var(--color-border);
        border-radius: var(--radius-sm, 6px);
        overflow: hidden;
      }

      .view-button {
        padding: 0.5rem 0.875rem;
        border: none;
        border-right: 1px solid var(--color-border);
        background: var(--color-surface);
        cursor: pointer;
        font-size: 0.875rem;
        font-weight: 500;
        color: var(--color-text-primary);
        transition: all 0.15s ease;

        &:last-child {
          border-right: none;
        }

        &:hover {
          background: var(--color-bg);
        }

        &.active {
          background: var(--color-accent);
          color: white;
        }
      }

      .calendar-view-container {
        flex: 1;
        overflow: hidden;
        position: relative;
        display: flex;
        flex-direction: column;
      }

      .management-loading,
      .management-error {
        padding: 1rem 2rem;
      }
    `
  ]
})
export class ScheduleManagementComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  therapistProfileId: string | null = null;
  selectedTherapistId: string | null = null;
  schedule: ScheduleSummary | null = null;
  therapistName = '';
  therapists: TherapistProfile[] = [];
  clients: ClientOption[] = [];
  isAdmin = false;
  canEdit = false;
  loading = false;
  error: string | null = null;
  showConfigPanel = false;
  showLeaveRequestModal = false;

  // Calendar state
  viewMode: CalendarViewMode = 'week';
  currentDate = new Date();
  weekData: CalendarWeekViewResponse | null = null;
  calendarLoading = false;
  calendarError: string | null = null;

  constructor(
    private scheduleApiService: ScheduleApiService,
    private therapistManagementService: TherapistManagementService,
    private clientService: ClientService,
    private authService: AuthService,
    private readonly calendarFacade: CalendarFacadeService
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
    this.loadClients();
    this.subscribeToCalendarState();
    this.loadCalendarData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private subscribeToCalendarState(): void {
    this.calendarFacade.weekData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        this.weekData = data;
        this.calendarLoading = this.calendarFacade.loading$();
        this.calendarError = this.calendarFacade.error$();
      });
  }

  private loadCalendarData(): void {
    const therapistIds = this.therapistProfileId ? [this.therapistProfileId] : [];
    this.calendarFacade.fetchWeekView(this.calendarFacade.currentDate$(), therapistIds).subscribe();
  }

  setViewMode(mode: CalendarViewMode): void {
    this.viewMode = mode;
    this.calendarFacade.setViewMode(mode);
  }

  onPreviousPeriod(): void {
    this.calendarFacade.navigatePrevious();
    this.currentDate = this.calendarFacade.currentDate$();
    this.loadCalendarData();
  }

  onNextPeriod(): void {
    this.calendarFacade.navigateNext();
    this.currentDate = this.calendarFacade.currentDate$();
    this.loadCalendarData();
  }

  onToday(): void {
    this.calendarFacade.goToToday();
    this.currentDate = this.calendarFacade.currentDate$();
    this.loadCalendarData();
  }

  onCalendarRetry(): void {
    this.loadCalendarData();
  }

  getCurrentPeriodLabel(): string {
    if (this.viewMode === 'day') {
      return this.currentDate.toLocaleDateString('en-US', {
        weekday: 'long', month: 'long', day: 'numeric', year: 'numeric'
      });
    }
    if (this.viewMode === 'month') {
      return this.currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    }
    // Week view — use weekData boundaries when available
    if (this.weekData) {
      const start = new Date(this.weekData.weekStart);
      const end = new Date(this.weekData.weekEnd);
      const startMonth = start.toLocaleDateString('en-US', { month: 'short' });
      const endMonth = end.toLocaleDateString('en-US', { month: 'short' });
      const startDay = start.getDate();
      const endDay = end.getDate();
      const year = start.getFullYear();
      return startMonth === endMonth
        ? `${startMonth} ${startDay}–${endDay}, ${year}`
        : `${startMonth} ${startDay} – ${endMonth} ${endDay}, ${year}`;
    }
    return '';
  }

  onAppointmentClick(appointment: CalendarAppointmentBlock): void {
    console.log('Appointment clicked:', appointment);
  }

  onMonthDayClick(date: Date): void {
    this.currentDate = date;
    this.calendarFacade.goToDate(date);
    this.setViewMode('day');
  }

  getAllAppointments(): CalendarAppointmentBlock[] {
    return this.weekData?.appointments || [];
  }

  getTherapistsList(): Array<{ id: string; name: string; specialization: string }> {
    if (!this.weekData?.therapists) return [];
    return Object.values(this.weekData.therapists).map(t => ({
      id: t.id,
      name: t.name,
      specialization: t.specialization
    }));
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

  private loadClients(): void {
    this.clientService.getAllClients().subscribe({
      next: clients => {
        this.clients = clients;
      },
      error: err => {
        console.error('Error loading clients:', err);
        // Non-fatal error - schedule can still be used without booking
      }
    });
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
      this.loadCalendarData();
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
    this.loadSchedule();
    setTimeout(() => this.loadCalendarData(), 500);
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
    this.loadSchedule();
    setTimeout(() => this.loadCalendarData(), 500);
    alert(`Leave request submitted successfully! Your request is now pending approval.`);
  }
}
