import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { ScheduleApiService } from '../../services/schedule-api.service';
import {
  DayOfWeek,
  RecurringSchedule,
  RecurringScheduleRequest,
  ScheduleOverride,
  ScheduleSummary
} from '../../models/schedule.model';

interface WeekdaySchedule {
  dayOfWeek: DayOfWeek;
  name: string;
  enabled: boolean;
  startTime: string;
  endTime: string;
  scheduleId?: string; // ID of existing recurring schedule if any
}

@Component({
  selector: 'app-schedule-config-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './schedule-config-panel.component.html',
  styleUrls: ['./schedule-config-panel.component.scss'],
  animations: [
    trigger('slideIn', [
      state('void', style({ transform: 'translateX(100%)' })),
      state('*', style({ transform: 'translateX(0)' })),
      transition('void => *', animate('300ms ease-out')),
      transition('* => void', animate('250ms ease-in'))
    ])
  ]
})
export class ScheduleConfigPanelComponent implements OnInit {
  @Input() therapistProfileId!: string;
  @Input() timezone: string = 'America/New_York';
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  weekDays: WeekdaySchedule[] = [
    { dayOfWeek: DayOfWeek.MONDAY, name: 'Monday', enabled: false, startTime: '09:00', endTime: '17:00' },
    { dayOfWeek: DayOfWeek.TUESDAY, name: 'Tuesday', enabled: false, startTime: '09:00', endTime: '17:00' },
    { dayOfWeek: DayOfWeek.WEDNESDAY, name: 'Wednesday', enabled: false, startTime: '09:00', endTime: '17:00' },
    { dayOfWeek: DayOfWeek.THURSDAY, name: 'Thursday', enabled: false, startTime: '09:00', endTime: '17:00' },
    { dayOfWeek: DayOfWeek.FRIDAY, name: 'Friday', enabled: false, startTime: '09:00', endTime: '17:00' },
    { dayOfWeek: DayOfWeek.SATURDAY, name: 'Saturday', enabled: false, startTime: '09:00', endTime: '17:00' },
    { dayOfWeek: DayOfWeek.SUNDAY, name: 'Sunday', enabled: false, startTime: '09:00', endTime: '17:00' }
  ];

  timeOptions: string[] = [];
  overrides: ScheduleOverride[] = [];
  isLoading = false;
  isSaving = false;

  constructor(private scheduleApiService: ScheduleApiService) {}

  ngOnInit(): void {
    this.generateTimeOptions();
    this.loadScheduleSummary();
  }

  /**
   * Generate time options from 6:00 AM to 10:00 PM in 30-minute increments
   */
  private generateTimeOptions(): void {
    this.timeOptions = [];
    for (let hour = 6; hour <= 22; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const timeStr = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
        this.timeOptions.push(timeStr);
      }
    }
  }

  /**
   * Load current schedule summary and populate form
   */
  private loadScheduleSummary(): void {
    this.isLoading = true;
    this.scheduleApiService.getScheduleSummary(this.therapistProfileId).subscribe({
      next: (summary: ScheduleSummary) => {
        this.populateWeekdaysFromSummary(summary.recurringSchedule);
        this.overrides = summary.overrides || [];
        this.timezone = summary.timezone || this.timezone;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load schedule summary:', error);
        this.isLoading = false;
      }
    });
  }

  /**
   * Map backend recurring schedule to weekday form model
   */
  private populateWeekdaysFromSummary(recurringSchedule: RecurringSchedule[]): void {
    console.log('Populating weekdays from summary:', recurringSchedule);
    this.weekDays.forEach(day => {
      const schedule = recurringSchedule.find(s => this.intToDayOfWeek(s.dayOfWeek) === day.dayOfWeek);
      if (schedule) {
        console.log(`Mapping schedule for ${day.name}:`, schedule);
        day.enabled = true;
        // Ensure times are in HH:mm format
        day.startTime = this.formatTime(schedule.startTime);
        day.endTime = this.formatTime(schedule.endTime);
        day.scheduleId = schedule.id;
      }
    });
  }

  /**
   * Format time value to HH:mm string
   * Handles both string "HH:mm" and array [hour, minute] formats from backend
   */
  private formatTime(time: any): string {
    if (typeof time === 'string') {
      // Already a string, return as-is (might be HH:mm:ss or HH:mm)
      return time.substring(0, 5); // Take first 5 chars to get HH:mm
    } else if (Array.isArray(time) && time.length >= 2) {
      // Jackson default format: [hour, minute, second, nano]
      const hour = String(time[0]).padStart(2, '0');
      const minute = String(time[1]).padStart(2, '0');
      return `${hour}:${minute}`;
    }
    return '09:00'; // Default fallback
  }

  /**
   * Handle weekday checkbox toggle
   */
  onDayToggle(day: WeekdaySchedule): void {
    if (!day.enabled) {
      // Reset to default times when disabled
      day.startTime = '09:00';
      day.endTime = '17:00';
    }
  }

  /**
   * Convert Integer (1-7) to DayOfWeek enum
   */
  private intToDayOfWeek(dayInt: number): DayOfWeek {
    const mapping: Record<number, DayOfWeek> = {
      1: DayOfWeek.MONDAY,
      2: DayOfWeek.TUESDAY,
      3: DayOfWeek.WEDNESDAY,
      4: DayOfWeek.THURSDAY,
      5: DayOfWeek.FRIDAY,
      6: DayOfWeek.SATURDAY,
      7: DayOfWeek.SUNDAY
    };
    return mapping[dayInt];
  }

  /**
   * Convert DayOfWeek enum to Integer (1-7 for Monday-Sunday)
   */
  private dayOfWeekToInt(dayOfWeek: DayOfWeek): number {
    const mapping: Record<DayOfWeek, number> = {
      [DayOfWeek.MONDAY]: 1,
      [DayOfWeek.TUESDAY]: 2,
      [DayOfWeek.WEDNESDAY]: 3,
      [DayOfWeek.THURSDAY]: 4,
      [DayOfWeek.FRIDAY]: 5,
      [DayOfWeek.SATURDAY]: 6,
      [DayOfWeek.SUNDAY]: 7
    };
    return mapping[dayOfWeek];
  }

  /**
   * Save all recurring schedule changes
   */
  applyRecurringSchedule(): void {
    this.isSaving = true;
    const requests: Promise<any>[] = [];

    this.weekDays.forEach(day => {
      if (day.enabled) {
        const request: RecurringScheduleRequest = {
          dayOfWeek: this.dayOfWeekToInt(day.dayOfWeek),
          startTime: day.startTime,
          endTime: day.endTime,
          timezone: this.timezone
        };
        console.log(`Saving schedule for ${day.name}:`, request);

        if (day.scheduleId) {
          // Update existing schedule
          requests.push(
            this.scheduleApiService
              .updateRecurringSchedule(this.therapistProfileId, day.scheduleId, request)
              .toPromise()
          );
        } else {
          // Create new schedule
          requests.push(
            this.scheduleApiService
              .createRecurringSchedule(this.therapistProfileId, request)
              .toPromise()
          );
        }
      } else if (day.scheduleId) {
        // Delete schedule if day is disabled but had a schedule
        requests.push(
          this.scheduleApiService
            .deleteRecurringSchedule(this.therapistProfileId, day.scheduleId)
            .toPromise()
        );
      }
    });

    console.log(`Submitting ${requests.length} schedule requests`);

    Promise.all(requests)
      .then(() => {
        console.log('All schedule requests completed successfully');
        this.isSaving = false;
        this.saved.emit();
        // Reload summary to refresh IDs
        this.loadScheduleSummary();
      })
      .catch((error) => {
        console.error('Failed to save recurring schedule:', error);
        this.isSaving = false;
      });
  }

  /**
   * Delete a schedule override
   */
  deleteOverride(override: ScheduleOverride): void {
    if (!override.id || !confirm('Delete this override?')) {
      return;
    }

    this.scheduleApiService
      .deleteScheduleOverride(this.therapistProfileId, override.id)
      .subscribe({
        next: () => {
          this.overrides = this.overrides.filter(o => o.id !== override.id);
          this.saved.emit();
        },
        error: (error) => {
          console.error('Failed to delete override:', error);
        }
      });
  }

  /**
   * Close the configuration panel
   */
  onClose(): void {
    this.close.emit();
  }

  /**
   * Format override description
   */
  getOverrideDescription(override: ScheduleOverride): string {
    if (override.available && override.startTime && override.endTime) {
      return `${override.startTime} — ${override.endTime}`;
    } else if (!override.available) {
      return override.reason || 'Unavailable';
    }
    return 'Custom schedule';
  }

  /**
   * Get override type badge text
   */
  getOverrideType(override: ScheduleOverride): string {
    return override.available ? 'Available' : 'Unavailable';
  }
}
