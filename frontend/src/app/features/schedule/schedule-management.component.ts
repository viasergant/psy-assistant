import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ScheduleConfigPanelComponent } from './components/schedule-config-panel/schedule-config-panel.component';
import { LeaveRequestDialogComponent } from './components/leave-request-dialog/leave-request-dialog.component';
import { AppointmentBookingDialogComponent } from './components/appointment-booking-dialog/appointment-booking-dialog.component';
import { AppointmentRescheduleDialogComponent } from './components/appointment-reschedule-dialog/appointment-reschedule-dialog.component';
import { AppointmentCancelDialogComponent } from './components/appointment-cancel-dialog/appointment-cancel-dialog.component';
import { AppointmentEditDialogComponent } from './components/appointment-edit-dialog/appointment-edit-dialog.component';
import { CalendarWeekViewComponent } from './components/calendar/calendar-week-view/calendar-week-view.component';
import { CalendarDayViewComponent } from './components/calendar/calendar-day-view/calendar-day-view.component';
import { CalendarMonthViewComponent } from './components/calendar/calendar-month-view/calendar-month-view.component';
import { CalendarFacadeService } from './services/calendar-facade.service';
import { ScheduleApiService } from './services/schedule-api.service';
import { AppointmentApiService } from './services/appointment-api.service';
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
import { ScheduleSummary, Leave, Appointment, CreateRecurringSeriesResponse } from './models/schedule.model';
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
    AppointmentBookingDialogComponent,
    AppointmentRescheduleDialogComponent,
    AppointmentCancelDialogComponent,
    AppointmentEditDialogComponent,
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
            *ngIf="canEdit && therapistProfileId"
            type="button"
            class="btn-primary"
            (click)="openBookingDialog()"
          >
            + {{ 'schedule.appointment.booking.title' | transloco }}
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
      <div class="calendar-view-container" (click)="captureClickPosition($event)">
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
          [leavePeriods]="schedule?.leavePeriods || []"
          (appointmentClicked)="onAppointmentClick($event)"
        ></app-calendar-day-view>

        <app-calendar-week-view
          *ngIf="!calendarLoading && !calendarError && viewMode === 'week'"
          [weekData]="weekData"
          [leavePeriods]="schedule?.leavePeriods || []"
          (appointmentClicked)="onAppointmentClick($event)"
        ></app-calendar-week-view>

        <app-calendar-month-view
          *ngIf="!calendarLoading && !calendarError && viewMode === 'month'"
          [monthDate]="currentDate"
          [appointments]="getAllAppointments()"
          [leavePeriods]="schedule?.leavePeriods || []"
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

      <!-- Appointment Context Menu -->
      <div *ngIf="showAppointmentMenu" class="appointment-menu-overlay" (click)="closeAppointmentMenu()">
        <div
          class="appointment-menu"
          [style.left.px]="appointmentMenuPosition.x"
          [style.top.px]="appointmentMenuPosition.y"
          (click)="$event.stopPropagation()"
        >
          <div *ngIf="appointmentActionLoading" class="menu-loading">
            <div class="spinner-sm"></div>
          </div>
          <ng-container *ngIf="!appointmentActionLoading">
            <button type="button" class="menu-item" (click)="onEditMenuAction()">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11.333 2A2.121 2.121 0 0 1 14 4.667L5.333 13.333H2v-3.333L10.667 1.333a.5.5 0 0 1 .666.667z" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span>{{ 'schedule.appointment.actions.edit' | transloco }}</span>
            </button>
            <button type="button" class="menu-item" (click)="onRescheduleMenuAction()">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M8 1v6l3 3" stroke-linecap="round" />
                <circle cx="8" cy="8" r="6" />
              </svg>
              <span>{{ 'schedule.appointment.actions.reschedule' | transloco }}</span>
            </button>
            <button type="button" class="menu-item menu-item-danger" (click)="onCancelMenuAction()">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 4L4 12M4 4l8 8" stroke-linecap="round" />
              </svg>
              <span>{{ 'schedule.appointment.actions.cancel' | transloco }}</span>
            </button>
          </ng-container>
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

      <!-- Appointment Booking Dialog -->
      <app-appointment-booking-dialog
        *ngIf="showBookingDialog && therapistProfileId"
        [therapistProfileId]="therapistProfileId"
        [clients]="clients"
        [initialDateTime]="selectedDateTime || undefined"
        [schedule]="schedule || undefined"
        (submitted)="onBookingSubmitted($event)"
        (seriesCreated)="onSeriesCreated($event)"
        (cancelled)="onBookingCancelled()"
      ></app-appointment-booking-dialog>

      <!-- Appointment Reschedule Dialog -->
      <app-appointment-reschedule-dialog
        *ngIf="showRescheduleDialog && selectedAppointment"
        [appointment]="selectedAppointment"
        [schedule]="schedule || undefined"
        (submitted)="onRescheduleSubmitted($event)"
        (cancelled)="onRescheduleCancelled()"
      ></app-appointment-reschedule-dialog>

      <!-- Appointment Cancel Dialog -->
      <app-appointment-cancel-dialog
        *ngIf="showCancelDialog && selectedAppointment"
        [appointment]="selectedAppointment"
        (submitted)="onCancelSubmitted($event)"
        (cancelled)="onCancelDialogCancelled()"
      ></app-appointment-cancel-dialog>

      <!-- Appointment Edit Dialog -->
      <app-appointment-edit-dialog
        *ngIf="showEditDialog && selectedAppointment"
        [appointment]="selectedAppointment"
        (submitted)="onEditSubmitted($event)"
        (cancelled)="onEditCancelled()"
      ></app-appointment-edit-dialog>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
        height: 100%;
        min-height: 0;
      }

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
        overflow: hidden; /* clips vertical; horizontal handled by each view */
        position: relative;
        display: flex;
        flex-direction: column;
        min-height: 0;
      }

      .management-loading,
      .management-error {
        padding: 1rem 2rem;
      }

      /* Appointment Context Menu */
      .appointment-menu-overlay {
        position: fixed;
        inset: 0;
        z-index: 1000;
      }

      .appointment-menu {
        position: fixed;
        background: var(--color-surface);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md, 8px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
        min-width: 180px;
        z-index: 1001;
        overflow: hidden;
      }

      .menu-item {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        width: 100%;
        padding: 0.75rem 1rem;
        border: none;
        background: transparent;
        cursor: pointer;
        font-size: 0.875rem;
        color: var(--color-text-primary);
        text-align: left;
        transition: background 0.15s ease;

        &:hover {
          background: var(--color-bg);
        }
      }

      .menu-item-danger {
        color: var(--color-error, #dc3545);
      }

      .menu-loading {
        display: flex;
        justify-content: center;
        padding: 1rem;
      }

      .spinner-sm {
        width: 20px;
        height: 20px;
        border: 2px solid var(--color-border);
        border-top-color: var(--color-accent);
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
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

  // Appointment dialog state
  showBookingDialog = false;
  showRescheduleDialog = false;
  showEditDialog = false;
  showCancelDialog = false;
  selectedAppointment: Appointment | null = null;
  selectedDateTime: Date | null = null;

  // Appointment context menu state
  showAppointmentMenu = false;
  appointmentMenuPosition = { x: 0, y: 0 };
  appointmentActionLoading = false;
  private pendingMenuAppointment: CalendarAppointmentBlock | null = null;

  constructor(
    private scheduleApiService: ScheduleApiService,
    private therapistManagementService: TherapistManagementService,
    private clientService: ClientService,
    private authService: AuthService,
    private readonly calendarFacade: CalendarFacadeService,
    private readonly appointmentApiService: AppointmentApiService
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
    this.pendingMenuAppointment = appointment;
    this.showAppointmentMenu = true;
  }

  captureClickPosition(event: MouseEvent): void {
    this.appointmentMenuPosition = { x: event.clientX, y: event.clientY };
  }

  closeAppointmentMenu(): void {
    this.showAppointmentMenu = false;
    this.pendingMenuAppointment = null;
    this.appointmentActionLoading = false;
  }

  private fetchAndOpenDialog(action: 'edit' | 'reschedule' | 'cancel'): void {
    if (!this.pendingMenuAppointment) return;
    this.appointmentActionLoading = true;
    this.appointmentApiService.getAppointment(this.pendingMenuAppointment.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: appointment => {
          this.selectedAppointment = appointment;
          this.appointmentActionLoading = false;
          this.showAppointmentMenu = false;
          this.pendingMenuAppointment = null;
          if (action === 'edit') this.showEditDialog = true;
          else if (action === 'reschedule') this.showRescheduleDialog = true;
          else this.showCancelDialog = true;
        },
        error: err => {
          console.error('Failed to load appointment details:', err);
          this.appointmentActionLoading = false;
          this.showAppointmentMenu = false;
        }
      });
  }

  onEditMenuAction(): void {
    this.fetchAndOpenDialog('edit');
  }

  onRescheduleMenuAction(): void {
    this.fetchAndOpenDialog('reschedule');
  }

  onCancelMenuAction(): void {
    this.fetchAndOpenDialog('cancel');
  }

  // ========== Booking Dialog ==========

  openBookingDialog(dateTime?: Date): void {
    this.selectedDateTime = dateTime || null;
    this.showBookingDialog = true;
  }

  onBookingSubmitted(appointment: Appointment): void {
    this.showBookingDialog = false;
    this.selectedDateTime = null;
    console.log('Appointment booked:', appointment);
    this.loadCalendarData();
  }

  onSeriesCreated(response: CreateRecurringSeriesResponse): void {
    this.showBookingDialog = false;
    this.selectedDateTime = null;
    console.log(`Recurring series created: id=${response.seriesId}`);
    this.loadCalendarData();
  }

  onBookingCancelled(): void {
    this.showBookingDialog = false;
    this.selectedDateTime = null;
  }

  // ========== Reschedule Dialog ==========

  onRescheduleSubmitted(appointment: Appointment): void {
    this.showRescheduleDialog = false;
    this.selectedAppointment = null;
    console.log('Appointment rescheduled:', appointment);
    this.loadCalendarData();
  }

  onRescheduleCancelled(): void {
    this.showRescheduleDialog = false;
    this.selectedAppointment = null;
  }

  // ========== Cancel Dialog ==========

  onCancelSubmitted(appointment: Appointment): void {
    this.showCancelDialog = false;
    this.selectedAppointment = null;
    console.log('Appointment cancelled:', appointment);
    this.loadCalendarData();
  }

  onCancelDialogCancelled(): void {
    this.showCancelDialog = false;
    this.selectedAppointment = null;
  }

  // ========== Edit Dialog ==========

  onEditSubmitted(appointment: Appointment): void {
    this.showEditDialog = false;
    this.selectedAppointment = null;
    console.log('Appointment updated:', appointment);
    this.loadCalendarData();
  }

  onEditCancelled(): void {
    this.showEditDialog = false;
    this.selectedAppointment = null;
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
      // User selected "Select a therapist..." - clear schedule and refresh calendar
      this.therapistProfileId = null;
      this.schedule = null;
      this.therapistName = '';
      this.loadCalendarData();
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
