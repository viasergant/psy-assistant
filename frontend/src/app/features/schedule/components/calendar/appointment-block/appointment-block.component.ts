import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoPipe } from '@jsverse/transloco';
import { CalendarAppointmentBlock, STATUS_COLORS, SESSION_TYPE_COLORS } from '../../../models/calendar.model';

/**
 * Display component for individual appointment blocks in calendar views.
 *
 * Shows appointment time, client name, session type, and status indicator.
 * Emits click events for opening appointment detail panels.
 */
@Component({
  selector: 'app-appointment-block',
  standalone: true,
  imports: [CommonModule, TranslocoPipe],
  template: `
    <div
      class="appointment-block"
      [class.modified]="appointment.isModified"
      [style.background-color]="getBackgroundColor()"
      [style.border-left-color]="getBorderColor()"
      (click)="onAppointmentClick()"
      [attr.aria-label]="getAriaLabel()"
      role="button"
      tabindex="0"
      (keydown.enter)="onAppointmentClick()"
      (keydown.space)="onAppointmentClick(); $event.preventDefault()"
    >
      <div class="appointment-time">
        {{ getTimeRange() }}
      </div>
      <div class="appointment-client">
        {{ appointment.clientName }}
      </div>
      <div class="appointment-session-type">
        {{ appointment.sessionTypeName }}
      </div>
      <div class="appointment-status-badge" [class]="appointment.status.toLowerCase()">
        {{ 'calendar.status.' + appointment.status | transloco }}
      </div>
      <div *ngIf="appointment.isModified" class="modified-indicator" 
           [attr.title]="'calendar.modifiedOccurrence' | transloco">
        ✱
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100%;
    }

    .appointment-block {
      position: relative;
      padding: 2px 6px;
      border-left: 4px solid;
      border-radius: var(--radius-sm);
      cursor: pointer;
      transition: box-shadow 0.15s ease;
      background: white;
      height: 100%;
      min-height: 24px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      gap: 1px;
      box-sizing: border-box;
    }

    .appointment-block:hover {
      overflow: visible;
      height: auto;
      min-height: 100%;
      z-index: 50;
      box-shadow: var(--shadow-md);
    }

    .appointment-block:focus {
      outline: 2px solid var(--color-primary);
      outline-offset: 2px;
    }

    .appointment-block.modified {
      border-style: dashed;
    }

    .appointment-time {
      font-size: 0.65rem;
      font-weight: 600;
      color: var(--color-text-secondary);
      white-space: nowrap;
    }

    .appointment-client {
      font-weight: 600;
      color: var(--color-text-primary);
      font-size: 0.7rem;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .appointment-session-type {
      font-size: 0.65rem;
      color: var(--color-text-secondary);
      display: none;
    }

    .appointment-block:hover .appointment-session-type {
      display: block;
    }

    .appointment-status-badge {
      font-size: 0.6rem;
      padding: 1px 4px;
      border-radius: var(--radius-xs);
      background: var(--color-surface-secondary);
      width: fit-content;
      text-transform: uppercase;
      font-weight: 600;
      display: none;
    }

    .appointment-block:hover .appointment-status-badge {
      display: block;
    }

    .appointment-status-badge.in_progress {
      background: #CCE5FF;
      color: #004085;
    }

    .appointment-status-badge.scheduled {
      background: #FFF3CD;
      color: #856404;
    }

    .appointment-status-badge.confirmed {
      background: #D4EDDA;
      color: #155724;
    }

    .appointment-status-badge.completed {
      background: #E2E3E5;
      color: #383D41;
    }

    .appointment-status-badge.cancelled {
      background: #F8D7DA;
      color: #721C24;
    }

    .modified-indicator {
      position: absolute;
      top: 4px;
      right: 4px;
      color: var(--color-warning);
      font-size: 1rem;
      font-weight: bold;
    }
  `]
})
export class AppointmentBlockComponent {
  @Input() appointment!: CalendarAppointmentBlock;
  @Output() appointmentClicked = new EventEmitter<CalendarAppointmentBlock>();

  onAppointmentClick(): void {
    this.appointmentClicked.emit(this.appointment);
  }

  getTimeRange(): string {
    const start = new Date(this.appointment.startTime);
    const end = new Date(this.appointment.endTime);
    return `${this.formatTime(start)} - ${this.formatTime(end)}`;
  }

  private formatTime(date: Date): string {
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  }

  getBackgroundColor(): string {
    const statusColor = STATUS_COLORS[this.appointment.status] ?? '#9E9E9E';
    return this.lightenColor(statusColor, 0.9);
  }

  getBorderColor(): string {
    const typeColor = SESSION_TYPE_COLORS[this.appointment.sessionTypeCode];
    return typeColor || SESSION_TYPE_COLORS['INDIVIDUAL'] || '#9E9E9E';
  }

  private lightenColor(color: string, amount: number): string {
    const hex = color.replace('#', '');
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${amount})`;
  }

  getAriaLabel(): string {
    const time = this.getTimeRange();
    return `Appointment with ${this.appointment.clientName} from ${time}. ${this.appointment.sessionTypeName}. Status: ${this.appointment.status}`;
  }
}
