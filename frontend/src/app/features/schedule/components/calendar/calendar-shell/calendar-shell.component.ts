import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { Subject, takeUntil } from 'rxjs';
import { CalendarWeekViewComponent } from '../calendar-week-view/calendar-week-view.component';
import { CalendarFacadeService } from '../../../services/calendar-facade.service';
import {
  CalendarWeekViewResponse,
  CalendarAppointmentBlock,
  CalendarViewMode
} from '../../../models/calendar.model';

/**
 * Main calendar shell component (PA-32).
 *
 * Orchestrates:
 * - View mode switching (day/week/month)
 * - Date navigation (prev/next/today)
 * - Filter controls
 * - Date picker
 * - Loading and error states
 *
 * Phase 1: Week view only
 * Phase 2: Add day and month views, filter bar, legend
 */
@Component({
  selector: 'app-calendar-shell',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslocoPipe,
    CalendarWeekViewComponent
  ],
  template: `
    <div class="calendar-shell">
      <!-- Header: Navigation and View Controls -->
      <header class="calendar-header">
        <div class="header-left">
          <h1>{{ 'calendar.title' | transloco }}</h1>
        </div>

        <div class="header-center">
          <!-- Date Navigation -->
          <div class="date-navigation">
            <button
              type="button"
              class="nav-button btn-icon"
              (click)="onPreviousPeriod()"
              [attr.aria-label]="'calendar.previous' | transloco"
            >
              ←
            </button>

            <button
              type="button"
              class="today-button btn-secondary"
              (click)="onToday()"
            >
              {{ 'calendar.today' | transloco }}
            </button>

            <button
              type="button"
              class="nav-button btn-icon"
              (click)="onNextPeriod()"
              [attr.aria-label]="'calendar.next' | transloco"
            >
              →
            </button>
          </div>

          <!-- Current Period Display -->
          <div class="period-display">
            <h2>{{ getCurrentPeriodLabel() }}</h2>
          </div>
        </div>

        <div class="header-right">
          <!-- View Mode Selector (Phase 1: week only) -->
          <div class="view-mode-selector" role="group" [attr.aria-label]="'calendar.viewMode' | transloco">
            <button
              type="button"
              class="view-button"
              [class.active]="viewMode === 'week'"
              (click)="setViewMode('week')"
            >
              {{ 'calendar.weekView' | transloco }}
            </button>
            <!-- Phase 2: Enable day and month views -->
            <!--
            <button
              type="button"
              class="view-button"
              [class.active]="viewMode === 'day'"
              (click)="setViewMode('day')"
            >
              {{ 'calendar.dayView' | transloco }}
            </button>
            <button
              type="button"
              class="view-button"
              [class.active]="viewMode === 'month'"
              (click)="setViewMode('month')"
            >
              {{ 'calendar.monthView' | transloco }}
            </button>
            -->
          </div>
        </div>
      </header>

      <!-- Calendar Content Area -->
      <main class="calendar-content">
        <!-- Loading State -->
        <div *ngIf="loading" class="loading-state">
          <div class="spinner"></div>
          <p>{{ 'calendar.loading' | transloco }}</p>
        </div>

        <!-- Error State -->
        <div *ngIf="error" class="error-state">
          <p class="error-message">{{ error }}</p>
          <button type="button" class="btn-primary" (click)="onRetry()">
            {{ 'calendar.retry' | transloco }}
          </button>
        </div>

        <!-- Week View -->
        <app-calendar-week-view
          *ngIf="!loading && !error && viewMode === 'week'"
          [weekData]="weekData"
          (appointmentClicked)="onAppointmentClick($event)"
        ></app-calendar-week-view>

        <!-- Phase 2: Day and Month Views -->
        <!-- <app-calendar-day-view *ngIf="viewMode === 'day'"></app-calendar-day-view> -->
        <!-- <app-calendar-month-view *ngIf="viewMode === 'month'"></app-calendar-month-view> -->
      </main>
    </div>
  `,
  styles: [`
    .calendar-shell {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: var(--color-background);
    }

    .calendar-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--spacing-lg);
      background: var(--color-surface);
      border-bottom: 1px solid var(--color-border);
      gap: var(--spacing-lg);
    }

    .header-left h1 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--color-text-primary);
    }

    .header-center {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--spacing-sm);
    }

    .date-navigation {
      display: flex;
      gap: var(--spacing-sm);
      align-items: center;
    }

    .nav-button {
      width: 36px;
      height: 36px;
      border: 1px solid var(--color-border);
      background: var(--color-surface);
      border-radius: var(--radius-sm);
      cursor: pointer;
      font-size: 1.25rem;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.15s ease;
    }

    .nav-button:hover {
      background: var(--color-surface-hover);
      border-color: var(--color-primary);
    }

    .today-button {
      padding: var(--spacing-sm) var(--spacing-md);
      font-weight: 600;
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
      border-radius: var(--radius-sm);
      overflow: hidden;
    }

    .view-button {
      padding: var(--spacing-sm) var(--spacing-md);
      border: none;
      background: var(--color-surface);
      cursor: pointer;
      font-weight: 500;
      transition: all 0.15s ease;
      border-right: 1px solid var(--color-border);
    }

    .view-button:last-child {
      border-right: none;
    }

    .view-button:hover {
      background: var(--color-surface-hover);
    }

    .view-button.active {
      background: var(--color-primary);
      color: white;
    }

    .calendar-content {
      flex: 1;
      overflow: hidden;
      position: relative;
    }

    .loading-state,
    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      gap: var(--spacing-md);
    }

    .spinner {
      width: 48px;
      height: 48px;
      border: 4px solid var(--color-border);
      border-top-color: var(--color-primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error-message {
      color: var(--color-error);
      font-weight: 500;
    }
  `]
})
export class CalendarShellComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  viewMode: CalendarViewMode = 'week';
  weekData: CalendarWeekViewResponse | null = null;
  loading = false;
  error: string | null = null;

  constructor(private readonly calendarFacade: CalendarFacadeService) {}

  ngOnInit(): void {
    this.subscribeToState();
    this.loadCalendarData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private subscribeToState(): void {
    // Subscribe to week data (Observable from BehaviorSubject)
    this.calendarFacade.weekData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        this.weekData = data;
      });

    // Note: loading$ and error$ are computed signals, not observables
    // They can be accessed directly in the template or via effect()
    // For now, we'll sync them manually when data changes
    this.calendarFacade.weekData$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loading = this.calendarFacade.loading$();
        this.error = this.calendarFacade.error$();
      });
  }

  private loadCalendarData(): void {
    this.calendarFacade.fetchWeekView().subscribe();
  }

  setViewMode(mode: CalendarViewMode): void {
    this.viewMode = mode;
    this.calendarFacade.setViewMode(mode);
  }

  onPreviousPeriod(): void {
    this.calendarFacade.navigatePrevious();
    this.loadCalendarData();
  }

  onNextPeriod(): void {
    this.calendarFacade.navigateNext();
    this.loadCalendarData();
  }

  onToday(): void {
    this.calendarFacade.goToToday();
    this.loadCalendarData();
  }

  onRetry(): void {
    this.loadCalendarData();
  }

  getCurrentPeriodLabel(): string {
    if (!this.weekData) return '';

    const start = new Date(this.weekData.weekStart);
    const end = new Date(this.weekData.weekEnd);

    const startMonth = start.toLocaleDateString('en-US', { month: 'short' });
    const endMonth = end.toLocaleDateString('en-US', { month: 'short' });

    const startDay = start.getDate();
    const endDay = end.getDate();
    const year = start.getFullYear();

    if (startMonth === endMonth) {
      return `${startMonth} ${startDay}-${endDay}, ${year}`;
    } else {
      return `${startMonth} ${startDay} - ${endMonth} ${endDay}, ${year}`;
    }
  }

  onAppointmentClick(appointment: CalendarAppointmentBlock): void {
    // Phase 1: Just log; Phase 2: Open detail panel
    console.log('Appointment clicked:', appointment);
    // TODO: Open appointment detail dialog
  }
}
