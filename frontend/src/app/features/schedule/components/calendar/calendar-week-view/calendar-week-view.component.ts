import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppointmentBlockComponent } from '../appointment-block/appointment-block.component';
import {
  CalendarWeekViewResponse,
  CalendarAppointmentBlock,
  TherapistInfo
} from '../../../models/calendar.model';
import { Leave, LeaveStatus } from '../../../models/schedule.model';

interface WeekDay {
  date: Date;
  dayName: string;
  dayNumber: number;
  isToday: boolean;
}

interface TimeSlot {
  hour: number;
  displayTime: string;
}

interface CellAppointments {
  therapistId: string;
  dayIndex: number;
  hour: number;
  appointments: CalendarAppointmentBlock[];
}

/**
 * Week calendar view component with multi-therapist column display (PA-32).
 *
 * Features:
 * - Side-by-side therapist columns
 * - 8AM-8PM time grid
 * - Appointment block positioning
 * - Keyboard navigation support
 */
@Component({
  selector: 'app-calendar-week-view',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, AppointmentBlockComponent],
  template: `
    <div class="week-view-container">
      <!-- Time column header (empty cell) -->
      <div class="calendar-grid">
        <!-- Header row: Time + Therapist columns -->
        <div class="grid-header">
          <div class="time-header">{{ 'calendar.time' | transloco }}</div>
          <div 
            *ngFor="let day of weekDays" 
            class="day-header"
            [class.today]="day.isToday"
            [class.leave-approved]="getLeaveStatus(day.date) === 'APPROVED'"
            [class.leave-pending]="getLeaveStatus(day.date) === 'PENDING'"
          >
            <div class="day-name">{{ day.dayName }}</div>
            <div class="day-number">{{ day.dayNumber }}</div>
            <div *ngIf="getLeaveStatus(day.date) === 'APPROVED'" class="leave-badge leave-badge-approved">
              {{ 'schedule.legend.leave' | transloco }}
            </div>
            <div *ngIf="getLeaveStatus(day.date) === 'PENDING'" class="leave-badge leave-badge-pending">
              {{ 'schedule.leaveStatus.pending' | transloco }}
            </div>
          </div>
        </div>

        <!-- Time slots grid -->
        <div class="grid-body">
          <div *ngFor="let timeSlot of timeSlots" class="time-row">
            <!-- Time label column -->
            <div class="time-label">{{ timeSlot.displayTime }}</div>

            <!-- Day cells -->
            <div 
              *ngFor="let day of weekDays; let dayIndex = index" 
              class="day-cell"
              [class.current-hour]="isCurrentHour(day.date, timeSlot.hour)"
            >
              <!-- Therapist columns within each day cell -->
              <div class="therapist-columns">
                <div 
                  *ngFor="let therapist of therapistsArray" 
                  class="therapist-column"
                  [style.width.%]="100 / therapistsArray.length"
                >
                  <!-- Appointments for this therapist/day/hour -->
                  <app-appointment-block
                    *ngFor="let appointment of getAppointmentsForCell(therapist.id, dayIndex, timeSlot.hour)"
                    [appointment]="appointment"
                    (appointmentClicked)="onAppointmentClick($event)"
                  ></app-appointment-block>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div *ngIf="!weekData" class="empty-state">
        <p>{{ 'calendar.noData' | transloco }}</p>
      </div>
    </div>
  `,
  styles: [`
    .week-view-container {
      height: 100%;
      overflow: auto;
    }

    .calendar-grid {
      display: grid;
      grid-template-rows: auto 1fr;
      min-width: 1024px;
    }

    .grid-header {
      display: grid;
      grid-template-columns: 80px repeat(7, 1fr);
      background: var(--color-surface);
      border-bottom: 2px solid var(--color-border);
      position: sticky;
      top: 0;
      z-index: 10;
    }

    .time-header {
      padding: var(--spacing-md);
      font-weight: 600;
      border-right: 1px solid var(--color-border);
      background: var(--color-surface);
    }

    .day-header {
      padding: var(--spacing-md);
      text-align: center;
      border-right: 1px solid var(--color-border);
      background: var(--color-surface);
    }

    .day-header.today {
      background: var(--color-primary-light);
    }

    .day-name {
      font-weight: 600;
      color: var(--color-text-primary);
      font-size: 0 875rem;
    }

    .day-number {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--color-text-secondary);
      margin-top: var(--spacing-xs);
    }

    .grid-body {
      display: flex;
      flex-direction: column;
    }

    .time-row {
      display: grid;
      grid-template-columns: 80px repeat(7, 1fr);
      min-height: 60px;
      border-bottom: 1px solid var(--color-border-light);
    }

    .time-label {
      padding: var(--spacing-sm);
      border-right: 1px solid var(--color-border);
      font-size: 0.75rem;
      color: var(--color-text-secondary);
      font-weight: 600;
      text-align: right;
    }

    .day-cell {
      border-right: 1px solid var(--color-border-light);
      position: relative;
      min-height: 60px;
    }

    .day-cell.current-hour {
      background: rgba(var(--color-primary-rgb), 0.05);
    }

    .therapist-columns {
      display: flex;
      height: 100%;
    }

    .therapist-column {
      border-left: 1px solid var(--color-border-light);
      padding: var(--spacing-xs);
      display: flex;
      flex-direction: column;
      gap: var(--spacing-xs);
    }

    .therapist-column:first-child {
      border-left: none;
    }

    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 400px;
      color: var(--color-text-secondary);
    }

    .day-header.leave-approved {
      background: rgba(239, 68, 68, 0.08);
    }

    .day-header.leave-pending {
      background: rgba(245, 158, 11, 0.08);
    }

    .leave-badge {
      margin-top: 4px;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 0.65rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .leave-badge-approved {
      background: rgba(239, 68, 68, 0.15);
      color: #dc2626;
    }

    .leave-badge-pending {
      background: rgba(245, 158, 11, 0.15);
      color: #d97706;
    }
  `]
})
export class CalendarWeekViewComponent implements OnChanges {
  @Input() weekData: CalendarWeekViewResponse | null = null;
  @Input() leavePeriods: Leave[] = [];
  @Output() appointmentClicked = new EventEmitter<CalendarAppointmentBlock>();

  weekDays: WeekDay[] = [];
  therapistsArray: TherapistInfo[] = [];
  timeSlots: TimeSlot[] = [];
  appointmentGrid: Map<string, CalendarAppointmentBlock[]> = new Map();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['weekData'] && this.weekData) {
      this.buildWeekDays();
      this.buildTherapistsList();
      this.buildTimeSlots();
      this.buildAppointmentGrid();
    }
  }

  private buildWeekDays(): void {
    if (!this.weekData) return;

    const startDate = new Date(this.weekData.weekStart);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    this.weekDays = [];
    for (let i = 0; i < 7; i++) {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + i);

      const checkDate = new Date(date);
      checkDate.setHours(0, 0, 0, 0);

      this.weekDays.push({
        date,
        dayName: date.toLocaleDateString('en-US', { weekday: 'short' }),
        dayNumber: date.getDate(),
        isToday: checkDate.getTime() === today.getTime()
      });
    }
  }

  private buildTherapistsList(): void {
    if (!this.weekData) return;
    this.therapistsArray = Object.values(this.weekData.therapists);
  }

  private buildTimeSlots(): void {
    this.timeSlots = [];
    // 8 AM to 8 PM (business hours)
    for (let hour = 8; hour <= 20; hour++) {
      const displayHour = hour % 12 || 12;
      const period = hour < 12 ? 'AM' : 'PM';
      this.timeSlots.push({
        hour,
        displayTime: `${displayHour}:00 ${period}`
      });
    }
  }

  private buildAppointmentGrid(): void {
    if (!this.weekData) return;

    this.appointmentGrid.clear();

    this.weekData.appointments.forEach(appointment => {
      const startTime = new Date(appointment.startTime);
      const therapistId = appointment.therapistProfileId;

      // Find which day of the week this appointment belongs to
      const dayIndex = this.weekDays.findIndex(day =>
        this.isSameDay(day.date, startTime)
      );

      if (dayIndex === -1) return; // Appointment outside current week

      const hour = startTime.getHours();
      const key = `${therapistId}-${dayIndex}-${hour}`;

      const existing = this.appointmentGrid.get(key) || [];
      existing.push(appointment);
      this.appointmentGrid.set(key, existing);
    });
  }

  getAppointmentsForCell(therapistId: string, dayIndex: number, hour: number): CalendarAppointmentBlock[] {
    const key = `${therapistId}-${dayIndex}-${hour}`;
    return this.appointmentGrid.get(key) || [];
  }

  isCurrentHour(date: Date, hour: number): boolean {
    const now = new Date();
    return this.isSameDay(date, now) && now.getHours() === hour;
  }

  private isSameDay(date1: Date, date2: Date): boolean {
    return (
      date1.getFullYear() === date2.getFullYear() &&
      date1.getMonth() === date2.getMonth() &&
      date1.getDate() === date2.getDate()
    );
  }

  onAppointmentClick(appointment: CalendarAppointmentBlock): void {
    this.appointmentClicked.emit(appointment);
  }

  getLeaveStatus(date: Date): 'PENDING' | 'APPROVED' | null {
    for (const leave of this.leavePeriods) {
      if (leave.status !== LeaveStatus.PENDING && leave.status !== LeaveStatus.APPROVED) {
        continue;
      }
      const start = new Date(leave.startDate + 'T00:00:00');
      const end = new Date(leave.endDate + 'T23:59:59');
      if (date >= start && date <= end) {
        return leave.status === LeaveStatus.APPROVED ? 'APPROVED' : 'PENDING';
      }
    }
    return null;
  }
}
