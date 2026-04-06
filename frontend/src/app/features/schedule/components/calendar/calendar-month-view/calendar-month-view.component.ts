import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { CalendarAppointmentBlock } from '../../../models/calendar.model';
import { Leave, LeaveStatus } from '../../../models/schedule.model';

/**
 * Month view calendar component showing appointment counts per day.
 *
 * Features:
 * - Traditional month grid (7 columns x 5-6 rows)
 * - Appointment count badges per day
 * - Color-coded status indicators
 * - Click day to see appointments
 */
@Component({
  selector: 'app-calendar-month-view',
  standalone: true,
  imports: [CommonModule, TranslocoPipe],
  template: `
    <div class="month-view">
      <!-- Month Grid -->
      <div class="month-grid">
        <!-- Day Headers -->
        <div class="day-header" *ngFor="let day of dayHeaders">
          {{ day }}
        </div>

        <!-- Calendar Days -->
        <div
          *ngFor="let day of calendarDays"
          class="calendar-day"
          [class.other-month]="day.isOtherMonth"
          [class.today]="day.isToday"
          [class.has-appointments]="day.appointmentCount > 0"
          [class.leave-approved]="day.leaveStatus === 'APPROVED'"
          [class.leave-pending]="day.leaveStatus === 'PENDING'"
          (click)="onDayClick(day.date)"
        >
          <div class="day-number">{{ day.dayNumber }}</div>
          
          <!-- Appointment Count Badge -->
          <div *ngIf="day.appointmentCount > 0" class="appointment-badge">
            <span class="badge-count">{{ day.appointmentCount }}</span>
            <span class="badge-label">{{ day.appointmentCount === 1 ? 'apt' : 'apts' }}</span>
          </div>

          <!-- Status Indicators Dots -->
          <div *ngIf="day.statusCounts.length > 0" class="status-dots">
            <span
              *ngFor="let status of day.statusCounts"
              class="status-dot"
              [class]="'status-' + status.toLowerCase()"
              [title]="status"
            ></span>
          </div>

          <!-- Leave Indicator -->
          <div *ngIf="day.leaveStatus === 'APPROVED'" class="leave-chip leave-chip-approved">
            {{ 'schedule.legend.leave' | transloco }}
          </div>
          <div *ngIf="day.leaveStatus === 'PENDING'" class="leave-chip leave-chip-pending">
            {{ 'schedule.leaveStatus.pending' | transloco }}
          </div>
        </div>
      </div>

      <!-- Month Navigation Info -->
      <div class="month-info">
        <p class="info-text">
          Click a day to view appointments
        </p>
      </div>
    </div>
  `,
  styles: [`
    .month-view {
      padding: var(--spacing-lg, 1.5rem);
      background: white;
      border-radius: var(--radius-md, 8px);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    }

    .month-grid {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 1px;
      background: var(--color-border, #E2E8F0);
      border: 1px solid var(--color-border, #E2E8F0);
      border-radius: var(--radius-sm, 6px);
      overflow: hidden;
    }

    .day-header {
      background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
      padding: var(--spacing-md, 1rem);
      text-align: center;
      font-weight: 700;
      font-size: 0.8125rem;
      color: var(--color-text-primary, #0F172A);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .calendar-day {
      background: white;
      min-height: 100px;
      padding: var(--spacing-sm, 0.5rem);
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      flex-direction: column;
      position: relative;
    }

    .calendar-day:hover {
      background: #F8FAFC;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      z-index: 1;
    }

    .calendar-day.other-month {
      background: #FAFAFA;
      opacity: 0.5;
    }

    .calendar-day.other-month:hover {
      background: #F5F5F5;
    }

    .calendar-day.today {
      border: 2px solid var(--color-accent, #0EA5A0);
      background: linear-gradient(135deg, #F0FDFC 0%, #CCFBF1 100%);
    }

    .calendar-day.today .day-number {
      color: var(--color-accent, #0EA5A0);
      font-weight: 800;
    }

    .calendar-day.has-appointments {
      background: linear-gradient(135deg, #FEFCE8 0%, #FEF9C3 100%);
    }

    .day-number {
      font-size: 1.125rem;
      font-weight: 600;
      color: var(--color-text-primary, #0F172A);
      margin-bottom: 0.5rem;
    }

    .appointment-badge {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      padding: 0.25rem 0.5rem;
      background: var(--color-accent, #0EA5A0);
      color: white;
      border-radius: var(--radius-sm, 6px);
      font-size: 0.75rem;
      font-weight: 700;
      width: fit-content;
      margin-top: auto;
    }

    .badge-count {
      font-size: 0.875rem;
    }

    .badge-label {
      font-size: 0.625rem;
      text-transform: uppercase;
      opacity: 0.9;
    }

    .status-dots {
      display: flex;
      gap: 0.25rem;
      margin-top: 0.5rem;
      flex-wrap: wrap;
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;
    }

    .status-dot.status-scheduled {
      background: #FFC107;
    }

    .status-dot.status-confirmed {
      background: #4CAF50;
    }

    .status-dot.status-completed {
      background: #9E9E9E;
    }

    .status-dot.status-cancelled {
      background: #F44336;
    }

    .status-dot.status-no_show {
      background: #FF5722;
    }

    .month-info {
      margin-top: var(--spacing-lg, 1.5rem);
      padding-top: var(--spacing-md, 1rem);
      border-top: 1px solid var(--color-border, #E2E8F0);
      text-align: center;
    }

    .info-text {
      color: var(--color-text-muted, #94A3B8);
      font-size: 0.875rem;
      margin: 0;
    }

    .calendar-day.leave-approved {
      background: rgba(239, 68, 68, 0.06);
    }

    .calendar-day.leave-pending {
      background: rgba(245, 158, 11, 0.06);
    }

    .leave-chip {
      margin-top: auto;
      padding: 2px 5px;
      border-radius: 3px;
      font-size: 0.6rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      align-self: flex-start;
    }

    .leave-chip-approved {
      background: rgba(239, 68, 68, 0.15);
      color: #dc2626;
    }

    .leave-chip-pending {
      background: rgba(245, 158, 11, 0.15);
      color: #d97706;
    }
  `]
})
export class CalendarMonthViewComponent implements OnChanges {
  @Input() monthDate!: Date;
  @Input() appointments: CalendarAppointmentBlock[] = [];
  @Input() leavePeriods: Leave[] = [];
  @Output() dayClicked = new EventEmitter<Date>();

  dayHeaders = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  calendarDays: Array<{
    date: Date;
    dayNumber: number;
    isOtherMonth: boolean;
    isToday: boolean;
    appointmentCount: number;
    statusCounts: string[];
    leaveStatus: 'PENDING' | 'APPROVED' | null;
  }> = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['monthDate'] || changes['appointments'] || changes['leavePeriods']) {
      this.buildCalendarDays();
    }
  }

  private buildCalendarDays(): void {
    if (!this.monthDate) return;

    const year = this.monthDate.getFullYear();
    const month = this.monthDate.getMonth();
    
    // First day of month and last day of month
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    
    // Start from the Sunday before or on the first day
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - startDate.getDay());
    
    // Build 42 days (6 weeks)
    this.calendarDays = [];
    const current = new Date(startDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (let i = 0; i < 42; i++) {
      const dayDate = new Date(current);
      const isOtherMonth = current.getMonth() !== month;
      const isToday = current.getTime() === today.getTime();
      
      // Count appointments for this day
      const dayAppointments = this.getAppointmentsForDay(dayDate);
      const statusCounts = [...new Set(dayAppointments.map(apt => apt.status))];

      this.calendarDays.push({
        date: dayDate,
        dayNumber: current.getDate(),
        isOtherMonth,
        isToday,
        appointmentCount: dayAppointments.length,
        statusCounts,
        leaveStatus: this.getLeaveStatusForDate(dayDate)
      });

      current.setDate(current.getDate() + 1);
    }
  }

  private getAppointmentsForDay(date: Date): CalendarAppointmentBlock[] {
    const dateStr = date.toISOString().split('T')[0];
    return this.appointments.filter(apt => {
      const aptDate = new Date(apt.startTime).toISOString().split('T')[0];
      return aptDate === dateStr;
    });
  }

  private getLeaveStatusForDate(date: Date): 'PENDING' | 'APPROVED' | null {
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

  onDayClick(date: Date): void {
    this.dayClicked.emit(date);
  }
}
