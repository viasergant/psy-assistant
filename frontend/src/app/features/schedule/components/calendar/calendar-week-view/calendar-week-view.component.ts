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

interface PositionedAppointment {
  appointment: CalendarAppointmentBlock;
  top: number;          // px from top of day column (1px = 1 min)
  height: number;       // px
  leftPercent: number;  // % within therapist band
  widthPercent: number; // % within therapist band
}

const HOUR_HEIGHT = 60;    // pixels per hour = 1px per minute
const DAY_START_HOUR = 8;
const MIN_APPT_HEIGHT = 20; // minimum visible block height in px

/**
 * Week calendar view component with multi-therapist column display (PA-32).
 *
 * Features:
 * - Side-by-side therapist columns
 * - 8AM–8PM time grid with 1px-per-minute absolute positioning
 * - Overlapping appointments rendered side-by-side horizontally
 * - Current-time indicator
 * - Keyboard navigation support
 */
@Component({
  selector: 'app-calendar-week-view',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, AppointmentBlockComponent],
  template: `
    <div class="week-view-container">
      <!-- Fixed header row: Time + Day columns -->
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

      <!-- Scrollable body: time labels + day columns side by side -->
      <div class="grid-scroll-body">
        <div class="grid-body">
          <!-- Time labels column -->
          <div class="time-column">
            <div *ngFor="let slot of timeSlots" class="time-label">{{ slot.displayTime }}</div>
          </div>

          <!-- Day columns (one per day, absolutely-positioned appointments inside) -->
          <div
            *ngFor="let day of weekDays; let dayIndex = index"
            class="day-col"
            [class.today]="day.isToday"
            [style.height.px]="totalDayHeight"
          >
            <!-- Red current-time line -->
            <div
              *ngIf="isToday(day.date)"
              class="current-time-line"
              [style.top.px]="getCurrentTimeTop()"
            ></div>

            <!-- Therapist bands (split day column horizontally per therapist) -->
            <div
              *ngFor="let therapist of therapistsArray; let tIdx = index"
              class="therapist-band"
              [style.left.%]="(tIdx / therapistsArray.length) * 100"
              [style.width.%]="100 / therapistsArray.length"
            >
              <!-- Absolutely-positioned appointment wrappers -->
              <div
                *ngFor="let block of getPositionedAppointments(therapist.id, dayIndex)"
                class="appt-wrapper"
                [style.top.px]="block.top"
                [style.height.px]="block.height"
                [style.left.%]="block.leftPercent"
                [style.width.%]="block.widthPercent"
              >
                <app-appointment-block
                  [appointment]="block.appointment"
                  (appointmentClicked)="onAppointmentClick($event)"
                ></app-appointment-block>
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
    :host {
      display: block;
      height: 100%;
      min-height: 0;
    }

    .week-view-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      min-height: 0;
      overflow-x: auto;
    }

    /* ── Fixed header ─────────────────────────────────────────── */

    .grid-header {
      display: grid;
      grid-template-columns: 80px repeat(7, 1fr);
      background: var(--color-surface);
      border-bottom: 2px solid var(--color-border);
      flex-shrink: 0;
      min-width: 1024px;
    }

    .time-header {
      padding: 4px 8px;
      font-size: 0.7rem;
      font-weight: 600;
      border-right: 1px solid var(--color-border);
      background: var(--color-surface);
    }

    .day-header {
      padding: 4px 2px;
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
      font-size: 0.65rem;
      text-transform: uppercase;
    }

    .day-number {
      font-size: 0.9rem;
      font-weight: 700;
      color: var(--color-text-secondary);
    }

    /* ── Scrollable body ──────────────────────────────────────── */

    .grid-scroll-body {
      flex: 1;
      overflow-y: auto;
      min-height: 0;
    }

    .grid-body {
      display: flex;
      flex-direction: row;
      min-width: 1024px;
    }

    /* ── Time labels column ───────────────────────────────────── */

    .time-column {
      width: 80px;
      flex-shrink: 0;
      border-right: 1px solid var(--color-border);
    }

    /* Each label is exactly HOUR_HEIGHT (60px) tall → 1px = 1 minute */
    .time-label {
      height: 60px;
      padding: 2px 8px 0 0;
      font-size: 0.65rem;
      color: var(--color-text-secondary);
      font-weight: 600;
      text-align: right;
      box-sizing: border-box;
    }

    /* ── Day columns ──────────────────────────────────────────── */

    .day-col {
      flex: 1;
      position: relative;
      border-right: 1px solid var(--color-border-light);
      /* Hour-separator lines via CSS gradient (60px repeat = 1 line per hour) */
      background-image: repeating-linear-gradient(
        to bottom,
        transparent 0px,
        transparent 59px,
        var(--color-border-light) 59px,
        var(--color-border-light) 60px
      );
    }

    .day-col.today {
      background-color: rgba(var(--color-primary-rgb), 0.02);
      background-image: repeating-linear-gradient(
        to bottom,
        transparent 0px,
        transparent 59px,
        var(--color-border-light) 59px,
        var(--color-border-light) 60px
      );
    }

    /* ── Current-time indicator ───────────────────────────────── */

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

    /* ── Therapist bands ──────────────────────────────────────── */

    .therapist-band {
      position: absolute;
      top: 0;
      bottom: 0;
    }

    /* ── Appointment wrappers ─────────────────────────────────── */

    .appt-wrapper {
      position: absolute;
      padding: 0 2px;
      box-sizing: border-box;
      z-index: 1;
    }

    .appt-wrapper:hover {
      z-index: 10;
    }

    /* ── Misc ─────────────────────────────────────────────────── */

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

  private positionedAppointmentsMap = new Map<string, PositionedAppointment[]>();

  /** Total pixel height of a day column: one slot per hour × HOUR_HEIGHT */
  get totalDayHeight(): number {
    return this.timeSlots.length * HOUR_HEIGHT;
  }

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
    // 8 AM to 8 PM (business hours), 13 slots × 60px = 780px total
    for (let hour = 8; hour <= 20; hour++) {
      const displayHour = hour % 12 || 12;
      const period = hour < 12 ? 'AM' : 'PM';
      this.timeSlots.push({ hour, displayTime: `${displayHour}:00 ${period}` });
    }
  }

  /**
   * Groups appointments by therapist+day, then computes pixel positions
   * with overlap detection so concurrent appointments sit side-by-side.
   */
  private buildAppointmentGrid(): void {
    if (!this.weekData) return;

    this.positionedAppointmentsMap.clear();

    const groupMap = new Map<string, CalendarAppointmentBlock[]>();

    this.weekData.appointments.forEach(appointment => {
      const startTime = new Date(appointment.startTime);
      const dayIndex = this.weekDays.findIndex(day => this.isSameDay(day.date, startTime));
      if (dayIndex === -1) return;

      const key = `${appointment.therapistProfileId}-${dayIndex}`;
      const existing = groupMap.get(key) || [];
      existing.push(appointment);
      groupMap.set(key, existing);
    });

    groupMap.forEach((appointments, key) => {
      this.positionedAppointmentsMap.set(key, this.computeOverlapPositions(appointments));
    });
  }

  /**
   * Assigns pixel top/height and left/width percentages so overlapping
   * appointments are rendered in columns side-by-side (not stacked).
   *
   * Algorithm:
   * 1. Sort by start time.
   * 2. Greedy sweep-line column assignment (no two overlapping share a column).
   * 3. For each appointment, count max column index among all its overlapping
   *    peers → determines how wide each block should be.
   */
  private computeOverlapPositions(appointments: CalendarAppointmentBlock[]): PositionedAppointment[] {
    if (appointments.length === 0) return [];

    interface Meta {
      apt: CalendarAppointmentBlock;
      startMin: number; // minutes from DAY_START_HOUR
      endMin: number;
      col: number;
      totalCols: number;
    }

    const metas: Meta[] = appointments.map(apt => {
      const start = new Date(apt.startTime);
      const end = new Date(apt.endTime);
      const startMin = (start.getHours() - DAY_START_HOUR) * 60 + start.getMinutes();
      const endMin = Math.max(
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

    // Determine totalCols = (max col used by any overlapping peer) + 1
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

    const maxViewMin = this.timeSlots.length * 60;

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

  getPositionedAppointments(therapistId: string, dayIndex: number): PositionedAppointment[] {
    return this.positionedAppointmentsMap.get(`${therapistId}-${dayIndex}`) || [];
  }

  /** Top offset in px for the current-time red line */
  getCurrentTimeTop(): number {
    const now = new Date();
    return (now.getHours() - DAY_START_HOUR) * HOUR_HEIGHT + now.getMinutes();
  }

  isToday(date: Date): boolean {
    return this.isSameDay(date, new Date());
  }

  private isSameDay(date1: Date, date2: Date): boolean {
    return (
      date1.getFullYear() === date2.getFullYear() &&
      date1.getMonth()    === date2.getMonth()    &&
      date1.getDate()     === date2.getDate()
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
      const end   = new Date(leave.endDate   + 'T23:59:59');
      if (date >= start && date <= end) {
        return leave.status === LeaveStatus.APPROVED ? 'APPROVED' : 'PENDING';
      }
    }
    return null;
  }
}
