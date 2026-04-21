import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { Tag } from 'primeng/tag';
import { AttendanceOutcome, GroupSessionParticipant } from '../../models/session.model';

/**
 * Panel component that displays the participant list for a GROUP session record.
 *
 * Shows each participant's name, active status, and their individual attendance
 * outcome (if recorded). Per-client attendance inputs are in GroupAttendancePanelComponent.
 */
@Component({
  selector: 'app-group-participant-panel',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, Tag],
  template: `
    <div class="group-participant-panel">
      <div class="panel-header">
        <span class="panel-title">{{ 'sessions.group.participants' | transloco }}</span>
        <span class="participant-count">
          ({{ activeParticipants.length }} {{ 'sessions.group.participants' | transloco }})
        </span>
      </div>

      <div *ngIf="activeParticipants.length === 0" class="empty-state">
        {{ 'sessions.group.noParticipants' | transloco }}
      </div>

      <ul class="participant-list" *ngIf="activeParticipants.length > 0">
        <li *ngFor="let p of activeParticipants" class="participant-item">
          <div class="participant-info">
            <span class="participant-name">{{ p.clientName }}</span>
          </div>
          <div class="participant-attendance" *ngIf="p.attendanceOutcome">
            <p-tag
              [value]="getOutcomeLabel(p.attendanceOutcome)"
              [severity]="getOutcomeSeverity(p.attendanceOutcome)"
            ></p-tag>
          </div>
          <div class="participant-attendance-pending" *ngIf="!p.attendanceOutcome">
            <span class="pending-label">{{ 'sessions.attendance.pending' | transloco }}</span>
          </div>
        </li>
      </ul>
    </div>
  `,
  styles: [`
    .group-participant-panel {
      margin-bottom: 1rem;
    }
    .panel-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
      font-weight: 600;
    }
    .participant-count {
      font-weight: 400;
      color: var(--text-color-secondary);
      font-size: 0.875rem;
    }
    .participant-list {
      list-style: none;
      padding: 0;
      margin: 0;
    }
    .participant-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.5rem 0;
      border-bottom: 1px solid var(--surface-border);
    }
    .participant-item:last-child {
      border-bottom: none;
    }
    .participant-name {
      font-weight: 500;
    }
    .pending-label {
      font-size: 0.75rem;
      color: var(--text-color-secondary);
      font-style: italic;
    }
    .empty-state {
      color: var(--text-color-secondary);
      font-style: italic;
      font-size: 0.875rem;
    }
  `]
})
export class GroupParticipantPanelComponent implements OnChanges {
  @Input() participants: GroupSessionParticipant[] = [];

  activeParticipants: GroupSessionParticipant[] = [];

  AttendanceOutcome = AttendanceOutcome;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['participants']) {
      this.activeParticipants = (this.participants ?? []).filter(p => !p.removedAt);
    }
  }

  getOutcomeLabel(outcome: AttendanceOutcome): string {
    const map: Record<AttendanceOutcome, string> = {
      [AttendanceOutcome.ATTENDED]: 'Attended',
      [AttendanceOutcome.NO_SHOW]: 'No Show',
      [AttendanceOutcome.LATE_CANCELLATION]: 'Late Cancel',
      [AttendanceOutcome.CANCELLED]: 'Cancelled',
      [AttendanceOutcome.THERAPIST_CANCELLATION]: 'Therapist Cancel',
    };
    return map[outcome] ?? outcome;
  }

  getOutcomeSeverity(
    outcome: AttendanceOutcome
  ): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | null {
    switch (outcome) {
      case AttendanceOutcome.ATTENDED:
        return 'success';
      case AttendanceOutcome.NO_SHOW:
        return 'danger';
      case AttendanceOutcome.LATE_CANCELLATION:
        return 'warn';
      case AttendanceOutcome.CANCELLED:
        return 'secondary';
      case AttendanceOutcome.THERAPIST_CANCELLATION:
        return 'info';
      default:
        return 'secondary';
    }
  }
}
