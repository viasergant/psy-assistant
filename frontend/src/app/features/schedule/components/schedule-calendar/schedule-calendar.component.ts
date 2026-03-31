import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ScheduleSummary,
  DayOfWeek,
  getDayLabel,
  AvailabilitySlot,
  getDayOfWeek
} from '../../models/schedule.model';
import { AvailabilityService } from '../../services/availability.service';
import { startOfWeek, addDays, format, isSameDay, parseISO } from 'date-fns';

interface WeekDay {
  date: Date;
  dateString: string;
  dayOfWeek: DayOfWeek;
  dayLabel: string;
}

interface TimeSlot {
  hour: number;
  minute: number;
  timeString: string;
  displayTime: string;
}

interface CalendarCell {
  day: WeekDay;
  timeSlot: TimeSlot;
  available: boolean;
  hasOverride: boolean;
  isLeave: boolean;
  isBooked: boolean;
}

@Component({
  selector: 'app-schedule-calendar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './schedule-calendar.component.html',
  styleUrls: ['./schedule-calendar.component.scss']
})
export class ScheduleCalendarComponent implements OnInit {
  @Input() therapistProfileId!: string;
  @Input() schedule?: ScheduleSummary;
  @Input() editable = false;

  currentWeekStart: Date = startOfWeek(new Date(), { weekStartsOn: 1 });
  weekDays: WeekDay[] = [];
  timeSlots: TimeSlot[] = [];
  calendarCells: CalendarCell[][] = [];
  loading = false;

  constructor(private availabilityService: AvailabilityService) {}

  ngOnInit(): void {
    this.generateWeekDays();
    this.generateTimeSlots();
    this.loadAvailability();
  }

  /**
   * Generate array of days for current week (Monday-Sunday)
   */
  private generateWeekDays(): void {
    this.weekDays = [];
    for (let i = 0; i < 7; i++) {
      const date = addDays(this.currentWeekStart, i);
      const dateString = format(date, 'yyyy-MM-dd');
      const dayOfWeek = getDayOfWeek(date);
      const dayLabel = getDayLabel(dayOfWeek);

      this.weekDays.push({
        date,
        dateString,
        dayOfWeek,
        dayLabel
      });
    }
  }

  /**
   * Generate array of 30-minute time slots (6:00 AM - 10:00 PM)
   */
  private generateTimeSlots(): void {
    this.timeSlots = [];
    const startHour = 6;
    const endHour = 22;

    for (let hour = startHour; hour <= endHour; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const timeString = `${hour.toString().padStart(2, '0')}:${minute
          .toString()
          .padStart(2, '0')}`;
        const isPM = hour >= 12;
        const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
        const displayTime = `${displayHour}:${minute
          .toString()
          .padStart(2, '0')} ${isPM ? 'PM' : 'AM'}`;

        this.timeSlots.push({
          hour,
          minute,
          timeString,
          displayTime
        });
      }
    }
  }

  /**
   * Load availability data from backend
   */
  private loadAvailability(): void {
    if (!this.therapistProfileId) return;

    this.loading = true;
    const startDate = format(this.currentWeekStart, 'yyyy-MM-dd');
    const endDate = format(addDays(this.currentWeekStart, 6), 'yyyy-MM-dd');

    this.availabilityService
      .getAvailableSlots(this.therapistProfileId, startDate, endDate)
      .subscribe({
        next: slots => {
          this.buildCalendarGrid(slots);
          this.loading = false;
        },
        error: err => {
          console.error('Error loading availability:', err);
          this.loading = false;
        }
      });
  }

  /**
   * Build 2D grid of calendar cells
   */
  private buildCalendarGrid(availabilitySlots: AvailabilitySlot[]): void {
    this.calendarCells = [];

    for (const timeSlot of this.timeSlots) {
      const row: CalendarCell[] = [];

      for (const day of this.weekDays) {
        const slot = availabilitySlots.find(
          s =>
            s.date === day.dateString &&
            s.startTime === timeSlot.timeString
        );

        const isLeave = this.isDateInLeave(day.date);
        const hasOverride = this.hasOverrideForDate(day.date);
        const available = slot?.available ?? false;

        row.push({
          day,
          timeSlot,
          available,
          hasOverride,
          isLeave,
          isBooked: false // TODO: integrate with appointment data
        });
      }

      this.calendarCells.push(row);
    }
  }

  /**
   * Check if date falls within a leave period
   */
  private isDateInLeave(date: Date): boolean {
    if (!this.schedule?.leavePeriodsup) return false;

    return this.schedule.leavePeriodsup.some(leave => {
      if (leave.status !== 'APPROVED') return false;

      const start = parseISO(leave.startDate);
      const end = parseISO(leave.endDate);
      return date >= start && date <= end;
    });
  }

  /**
   * Check if date has a schedule override
   */
  private hasOverrideForDate(date: Date): boolean {
    if (!this.schedule?.overrides) return false;

    return this.schedule.overrides.some(override =>
      isSameDay(parseISO(override.date), date)
    );
  }

  /**
   * Navigate to previous week
   */
  previousWeek(): void {
    this.currentWeekStart = addDays(this.currentWeekStart, -7);
    this.generateWeekDays();
    this.loadAvailability();
  }

  /**
   * Navigate to next week
   */
  nextWeek(): void {
    this.currentWeekStart = addDays(this.currentWeekStart, 7);
    this.generateWeekDays();
    this.loadAvailability();
  }

  /**
   * Jump to current week
   */
  goToToday(): void {
    this.currentWeekStart = startOfWeek(new Date(), { weekStartsOn: 1 });
    this.generateWeekDays();
    this.loadAvailability();
  }

  /**
   * Get formatted week range display
   */
  getWeekRangeDisplay(): string {
    const start = format(this.currentWeekStart, 'MMM d');
    const end = format(addDays(this.currentWeekStart, 6), 'MMM d, yyyy');
    return `${start} - ${end}`;
  }

  /**
   * Get CSS class for calendar cell
   */
  getCellClass(cell: CalendarCell): string {
    if (cell.isLeave) return 'cell-leave';
    if (cell.isBooked) return 'cell-booked';
    if (cell.hasOverride) return 'cell-override';
    if (cell.available) return 'cell-available';
    return 'cell-unavailable';
  }

  /**
   * Get tooltip text for calendar cell
   */
  getCellTitle(cell: CalendarCell): string {
    if (cell.isLeave) return 'On leave';
    if (cell.isBooked) return 'Appointment booked';
    if (cell.hasOverride) return 'Schedule override';
    if (cell.available) return 'Available';
    return 'Unavailable';
  }

  /**
   * Handle cell click
   */
  onCellClick(cell: CalendarCell): void {
    if (!this.editable) return;
    // TODO: Open edit dialog
    console.log('Cell clicked:', cell);
  }
}
