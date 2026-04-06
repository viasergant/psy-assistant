import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, of } from 'rxjs';
import {
  CalendarWeekViewResponse,
  CalendarAppointmentBlock,
  CalendarViewMode,
  CalendarFilters
} from '../models/calendar.model';

/**
 * Facade service for calendar state management and API integration.
 *
 * Provides:
 * - Week/day/month view data fetching
 * - Filter state management (therapists, session types, statuses)
 * - View mode switching
 * - Date navigation
 *
 * Uses Angular signals for reactive state management.
 */
@Injectable({
  providedIn: 'root'
})
export class CalendarFacadeService {
  private readonly baseUrl = '/api/v1/calendar';

  // State signals
  private readonly viewModeSignal = signal<CalendarViewMode>('week');
  private readonly currentDateSignal = signal<Date>(new Date());
  private readonly filtersSignal = signal<CalendarFilters>({
    therapistIds: [],
    sessionTypes: [],
    statuses: []
  });
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  // Appointment data
  private readonly weekDataSubject = new BehaviorSubject<CalendarWeekViewResponse | null>(null);

  // Public observables
  readonly weekData$ = this.weekDataSubject.asObservable();
  readonly viewMode$ = computed(() => this.viewModeSignal());
  readonly currentDate$ = computed(() => this.currentDateSignal());
  readonly filters$ = computed(() => this.filtersSignal());
  readonly loading$ = computed(() => this.loadingSignal());
  readonly error$ = computed(() => this.errorSignal());

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches week view data from backend.
   *
   * @param weekDate - Any date within the target week (defaults to current date)
   * @param therapistIds - Optional list of therapist IDs to filter
   * @param timezone - IANA timezone identifier (defaults to browser timezone)
   */
  fetchWeekView(
    weekDate?: Date,
    therapistIds?: string[],
    timezone?: string
  ): Observable<CalendarWeekViewResponse> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    const date = weekDate || this.currentDateSignal();
    const tz = timezone || Intl.DateTimeFormat().resolvedOptions().timeZone;

    let params = new HttpParams()
      .set('timezone', tz);

    // Format date as YYYY-MM-DD
    const dateStr = date.toISOString().split('T')[0];
    params = params.set('weekDate', dateStr);

    // Add therapist filter if provided
    if (therapistIds && therapistIds.length > 0) {
      therapistIds.forEach(id => {
        params = params.append('therapistIds', id);
      });
    }

    return this.http.get<CalendarWeekViewResponse>(`${this.baseUrl}/week`, { params }).pipe(
      tap(response => {
        this.weekDataSubject.next(response);
        this.loadingSignal.set(false);
      }),
      catchError(error => {
        console.error('Failed to fetch week view:', error);
        this.errorSignal.set('Failed to load calendar data. Please try again.');
        this.loadingSignal.set(false);
        return of(error);
      })
    );
  }

  /**
   * Sets the calendar view mode (day/week/month).
   */
  setViewMode(mode: CalendarViewMode): void {
    this.viewModeSignal.set(mode);
  }

  /**
   * Navigates to the previous period (day/week/month based on current view).
   */
  navigatePrevious(): void {
    const currentDate = this.currentDateSignal();
    const viewMode = this.viewModeSignal();

    let newDate: Date;
    switch (viewMode) {
      case 'day':
        newDate = new Date(currentDate);
        newDate.setDate(currentDate.getDate() - 1);
        break;
      case 'week':
        newDate = new Date(currentDate);
        newDate.setDate(currentDate.getDate() - 7);
        break;
      case 'month':
        newDate = new Date(currentDate);
        newDate.setMonth(currentDate.getMonth() - 1);
        break;
    }

    this.currentDateSignal.set(newDate);
  }

  /**
   * Navigates to the next period (day/week/month based on current view).
   */
  navigateNext(): void {
    const currentDate = this.currentDateSignal();
    const viewMode = this.viewModeSignal();

    let newDate: Date;
    switch (viewMode) {
      case 'day':
        newDate = new Date(currentDate);
        newDate.setDate(currentDate.getDate() + 1);
        break;
      case 'week':
        newDate = new Date(currentDate);
        newDate.setDate(currentDate.getDate() + 7);
        break;
      case 'month':
        newDate = new Date(currentDate);
        newDate.setMonth(currentDate.getMonth() + 1);
        break;
    }

    this.currentDateSignal.set(newDate);
  }

  /**
   * Jumps to a specific date.
   */
  goToDate(date: Date): void {
    this.currentDateSignal.set(date);
  }

  /**
   * Returns to today's date.
   */
  goToToday(): void {
    this.currentDateSignal.set(new Date());
  }

  /**
   * Updates active filters.
   */
  setFilters(filters: Partial<CalendarFilters>): void {
    const current = this.filtersSignal();
    this.filtersSignal.set({ ...current, ...filters });
  }

  /**
   * Clears all filters.
   */
  clearFilters(): void {
    this.filtersSignal.set({
      therapistIds: [],
      sessionTypes: [],
      statuses: []
    });
  }
}
