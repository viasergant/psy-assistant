import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppointmentBlockComponent } from '../appointment-block/appointment-block.component';
import { CalendarAppointmentBlock } from '../../../models/calendar.model';

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
              
              <!-- Appointments Overlay -->
              <div class="appointments-overlay">
                <div
                  *ngFor="let appointment of getTherapistAppointments(therapist.id)"
                  class="appointment-wrapper"
                  [style.top.px]="calculateTopPosition(appointment.startTime)"
                  [style.height.px]="calculateHeight(appointment.durationMinutes)"
                >
                  <app-appointment-block
                    [appointment]="appointment"
                    (click)="onAppointmentClick(appointment)"
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
      left: 4px;
      right: 4px;
      pointer-events: auto;
    }

    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 300px;
      color: var(--color-text-muted, #94A3B8);
      font-size: 0.9375rem;
    }
  `]
})
export class CalendarDayViewComponent {
  @Input() dayDate!: Date;
  @Input() appointments: CalendarAppointmentBlock[] = [];
  @Input() therapists: Array<{id: string; name: string; specialization: string}> = [];
  @Output() appointmentClicked = new EventEmitter<CalendarAppointmentBlock>();

  // Time slots from 8 AM to 8 PM (12 hours)
  timeSlots = Array.from({ length: 12 }, (_, i) => i + 8);

  formatHour(hour: number): string {
    const period = hour >= 12 ? 'PM' : 'AM';
    const displayHour = hour > 12 ? hour - 12 : hour;
    return `${displayHour}:00 ${period}`;
  }

  getTherapistAppointments(therapistId: string): CalendarAppointmentBlock[] {
    return this.appointments.filter(apt => apt.therapistProfileId === therapistId);
  }

  calculateTopPosition(startTime: string): number {
    const date = new Date(startTime);
    const hour = date.getHours();
    const minutes = date.getMinutes();
    
    // Calculate offset from 8 AM
    const offsetHours = hour - 8;
    const offsetMinutes = minutes;
    
    // Each hour = 60px, each minute = 1px
    return (offsetHours * 60) + offsetMinutes;
  }

  calculateHeight(durationMinutes: number): number {
    // Each minute = 1px
    return durationMinutes;
  }

  onAppointmentClick(appointment: CalendarAppointmentBlock): void {
    this.appointmentClicked.emit(appointment);
  }
}
