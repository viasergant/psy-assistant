import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppointmentBlockComponent } from '../appointment-block/appointment-block.component';
import { CalendarAppointmentBlock } from '../../../models/calendar.model';
import { Leave, LeaveStatus } from '../../../models/schedule.model';

interface PositionedAppointment {
  appointment: CalendarAppointmentBlock;
  top: number;          // px from top of slots-container (1px = 1 min)
  height: number;       // px
  leftPercent: number;  // % within therapist column
  widthPercent: number; // % within therapist column
}

const HOUR_HEIGHT = 60;    // pixels per hour = 1px per minute
const DAY_START_HOUR = 8;
const MIN_APPT_HEIGHT = 20;

/**
 * Day view calendar component showing hourly time slots for multiple therapists side-by-side.
 *
 * Features:
 * - Multi-therapist columns (side-by-side)
 * - Hourly time grid (8 AM - 8 PM)
 * - Appointment blocks positioned by time
 * - Horizontal scroll for >8 therapists
 */
@Component({
  selector: 'app-calendar-day-view',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, AppointmentBlockComponent],
  template: `
    <div class="day-view">
      <!-- Leave Banner -->
      <div *ngIf="getDayLeaveStatus() === 'APPROVED'" class="leave-banner leave-banner-approved">
        {{ 'schedule.legend.leave' | transloco }}
      </div>
      <div *ngIf="getDayLeaveStatus() === 'PENDING'" class="leave-banner leave-banner-pending">
        {{ 'schedule.leaveStatus.pending' | transloco }}
      </div>

      <div class="time-grid-container">
        <!-- Time Column -->
        <div class="time-column">
          <div class="time-header"></div>
          <div class="time-slot" *ngFor="let hour of timeSlots">
            {{ formatHour(hour) }}
          </div>
        </div>

        <!-- Therapist Columns -->
        <div class="therapists-container">
          <div class="therapist-column" *ngFor="let therapist of therapists">
            <!-- Therapist Header -->
            <div class="therapist-header">
              <div class="therapist-name">{{ therapist.name }}</div>
              <div class="therapist-specialization">{{ therapist.specialization }}</div>
            </div>

            <!-- Time Slots Grid -->
            <div class="slots-container">
              <div class="time-slot-row" *ngFor="let hour of timeSlots"></div>

              <!-- Current time indicator -->
              <div
                *ngIf="isToday()"
                class="current-time-line"
                [style.top.px]="getCurrentTimeTop()"
              ></div>
              
              <!-- Appointments Overlay -->
              <div class="appointments-overlay">
                <div
                  *ngFor="let block of getPositionedAppointments(therapist.id)"
                  class="appointment-wrapper"
                  [style.top.px]="block.top"
                  [style.height.px]="block.height"
                  [style.left.%]="block.leftPercent"
                  [style.width.%]="block.widthPercent"
                >
                  <app-appointment-block
                    [appointment]="block.appointment"
                    (click)="onAppointmentClick(block.appointment)"
                  ></app-appointment-block>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div *ngIf="!therapists || therapists.length === 0" class="empty-state">
        <p>{{ 'calendar.noData' | transloco }}</p>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100%;
      min-height: 0;
    }

    .day-view {
      height: 100%;
      overflow: auto;
      background: var(--color-background, #F8FAFC);
    }

    .time-grid-container {
      display: flex;
      min-width: fit-content;
      background: white;
      border-radius: var(--radius-md, 8px);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    }

    .time-column {
      position: sticky;
      left: 0;
      z-index: 10;
      background: white;
      border-right: 2px solid var(--color-border, #E2E8F0);
    }

    .time-header {
      height: 80px;
      border-bottom: 1px solid var(--color-border, #E2E8F0);
    }

    .time-slot {
      height: 60px;
      padding: var(--spacing-sm, 0.5rem);
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--color-text-secondary, #64748B);
      border-bottom: 1px solid var(--color-border-light, #F1F5F9);
      display: flex;
      align-items: flex-start;
      width: 70px;
    }

    .therapists-container {
      display: flex;
      flex: 1;
      overflow-x: auto;
    }

    .therapist-column {
      min-width: 220px;
      flex-shrink: 0;
      border-right: 1px solid var(--color-border, #E2E8F0);
    }

    .therapist-column:last-child {
      border-right: none;
    }

    .therapist-header {
      height: 80px;
      padding: var(--spacing-md, 1rem);
      border-bottom: 1px solid var(--color-border, #E2E8F0);
      background: linear-gradient(135deg, #F8FAFC 0%, #F1F5F9 100%);
    }

    .therapist-name {
      font-weight: 700;
      font-size: 0.9375rem;
      color: var(--color-text-primary, #0F172A);
      margin-bottom: 0.25rem;
    }

    .therapist-specialization {
      font-size: 0.75rem;
      color: var(--color-text-secondary, #64748B);
      font-weight: 500;
    }

    .slots-container {
      position: relative;
    }

    .time-slot-row {
      height: 60px;
      border-bottom: 1px solid var(--color-border-light, #F1F5F9);
    }

    .current-time-line {
      position: absolute;
      left: 0;
      right: 0;
      height: 2px;
      background: var(--color-error, #ef4444);
      z-index: 5;
      pointer-events: none;
    }

    .current-time-line::before {
      content: '';
      position: absolute;
      left: -4px;
      top: -3px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--color-error, #ef4444);
    }

    .appointments-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      pointer-events: none;
    }

    .appointment-wrapper {
      position: absolute;
      padding: 0 2px;
      box-sizing: border-box;
      pointer-events: auto;
      z-index: 1;
    }

    .appointment-wrapper:hover {
      z-index: 10;
    }

    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 300px;
      color: var(--color-text-muted, #94A3B8);
      font-size: 0.9375rem;
    }

    .leave-banner {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0.5rem 1rem;
      font-size: 0.875rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .leave-banner-approved {
      background: rgba(239, 68, 68, 0.12);
      color: #dc2626;
      border-bottom: 2px solid rgba(239, 68, 68, 0.3);
    }

    .leave-banner-pending {
      background: rgba(245, 158, 11, 0.12);
      color: #d97706;
      border-bottom: 2px solid rgba(245, 158, 11, 0.3);
    }
  `]
})
export class CalendarDayViewComponent {
  @Input() dayDate!: Date;
  @Input() appointments: CalendarAppointmentBlock[] = [];
  @Input() therapists: Array<{id: string; name: string; specialization: string}> = [];
  @Input() leavePeriods: Leave[] = [];
  @Output() appointmentClicked = new EventEmitter<CalendarAppointmentBlock>();

  // Time slots from 8 AM to 7 PM (12 hours × 60px = 720px total)
  timeSlots = Array.from({ length: 12 }, (_, i) => i + 8);

  formatHour(hour: number): string {
    const period = hour >= 12 ? 'PM' : 'AM';
    const displayHour = hour > 12 ? hour - 12 : hour;
    return `${displayHour}:00 ${period}`;
  }

  getDayLeaveStatus(): 'PENDING' | 'APPROVED' | null {
    if (!this.dayDate) return null;
    for (const leave of this.leavePeriods) {
      if (leave.status !== LeaveStatus.PENDING && leave.status !== LeaveStatus.APPROVED) {
        continue;
      }
      const start = new Date(leave.startDate + 'T00:00:00');
      const end   = new Date(leave.endDate   + 'T23:59:59');
      if (this.dayDate >= start && this.dayDate <= end) {
        return leave.status === LeaveStatus.APPROVED ? 'APPROVED' : 'PENDING';
      }
    }
    return null;
  }

  isToday(): boolean {
    if (!this.dayDate) return false;
    const today = new Date();
    return (
      this.dayDate.getFullYear() === today.getFullYear() &&
      this.dayDate.getMonth()    === today.getMonth()    &&
      this.dayDate.getDate()     === today.getDate()
    );
  }

  getCurrentTimeTop(): number {
    const now = new Date();
    return (now.getHours() - DAY_START_HOUR) * HOUR_HEIGHT + now.getMinutes();
  }

  getPositionedAppointments(therapistId: string): PositionedAppointment[] {
    const apts = this.appointments.filter(a => {
      if (a.therapistProfileId !== therapistId) return false;
      const start = new Date(a.startTime);
      return (
        start.getFullYear() === this.dayDate.getFullYear() &&
        start.getMonth() === this.dayDate.getMonth() &&
        start.getDate() === this.dayDate.getDate()
      );
    });
    return this.computeOverlapPositions(apts);
  }

  /**
   * Assigns pixel top/height and left/width percentages so overlapping
   * appointments are rendered side-by-side horizontally (not stacked).
   */
  private computeOverlapPositions(appointments: CalendarAppointmentBlock[]): PositionedAppointment[] {
    if (appointments.length === 0) return [];

    interface Meta {
      apt: CalendarAppointmentBlock;
      startMin: number;
      endMin: number;
      col: number;
      totalCols: number;
    }

    const maxViewMin = this.timeSlots.length * 60;

    const metas: Meta[] = appointments.map(apt => {
      const start = new Date(apt.startTime);
      const end   = new Date(apt.endTime);
      const startMin = (start.getHours() - DAY_START_HOUR) * 60 + start.getMinutes();
      const endMin   = Math.max(
        (end.getHours() - DAY_START_HOUR) * 60 + end.getMinutes(),
        startMin + 1
      );
      return { apt, startMin, endMin, col: 0, totalCols: 1 };
    });

    metas.sort((a, b) => a.startMin - b.startMin || a.endMin - b.endMin);

    // Greedy column assignment: pick the first free column
    const colEndTimes: number[] = [];
    for (const meta of metas) {
      let col = 0;
      while (col < colEndTimes.length && colEndTimes[col] > meta.startMin) {
        col++;
      }
      meta.col = col;
      colEndTimes[col] = meta.endMin;
    }

    // totalCols = (max col among all overlapping peers) + 1
    for (let i = 0; i < metas.length; i++) {
      let maxCol = metas[i].col;
      for (let j = 0; j < metas.length; j++) {
        if (i !== j &&
            metas[i].startMin < metas[j].endMin &&
            metas[i].endMin   > metas[j].startMin) {
          maxCol = Math.max(maxCol, metas[j].col);
        }
      }
      metas[i].totalCols = maxCol + 1;
    }

    return metas.map(meta => {
      const clampedStart = Math.max(0, meta.startMin);
      const clampedEnd   = Math.min(meta.endMin, maxViewMin);
      return {
        appointment:  meta.apt,
        top:          clampedStart,
        height:       Math.max(clampedEnd - clampedStart, MIN_APPT_HEIGHT),
        leftPercent:  (meta.col / meta.totalCols) * 100,
        widthPercent: (1 / meta.totalCols) * 100,
      };
    });
  }

  onAppointmentClick(appointment: CalendarAppointmentBlock): void {
    this.appointmentClicked.emit(appointment);
  }
}
