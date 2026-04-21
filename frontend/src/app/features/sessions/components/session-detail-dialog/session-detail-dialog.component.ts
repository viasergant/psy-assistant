import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { DialogModule } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { AttendanceOutcome, AttendanceOutcomeResponse, GroupSessionParticipant, RecordKind, SessionRecord, SessionStatus } from '../../models/session.model';
import { SessionNotesPanelComponent } from '../session-notes-panel/session-notes-panel.component';
import { RecordAttendanceDialogComponent } from '../record-attendance-dialog/record-attendance-dialog.component';
import { GroupParticipantPanelComponent } from '../group-participant-panel/group-participant-panel.component';
import { GroupAttendancePanelComponent, GroupAttendanceResult } from '../group-attendance-panel/group-attendance-panel.component';

/**
 * Session detail dialog component for viewing complete session information.
 *
 * Supports both INDIVIDUAL and GROUP session records. For GROUP records,
 * the participant list panel and per-client attendance panel are rendered
 * in place of the single-client attendance dialog.
 */
@Component({
  selector: 'app-session-detail-dialog',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoPipe,
    DialogModule,
    Button,
    Tag,
    SessionNotesPanelComponent,
    RecordAttendanceDialogComponent,
    GroupParticipantPanelComponent,
    GroupAttendancePanelComponent,
  ],
  templateUrl: './session-detail-dialog.component.html',
  styleUrls: ['./session-detail-dialog.component.scss'],
})
export class SessionDetailDialogComponent {
  @Input() visible = false;
  @Input() session!: SessionRecord;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() attendanceRecorded = new EventEmitter<AttendanceOutcomeResponse>();

  SessionStatus = SessionStatus;
  AttendanceOutcome = AttendanceOutcome;
  RecordKind = RecordKind;
  attendanceDialogVisible = false;
  showGroupAttendancePanel = false;

  get isGroupSession(): boolean {
    return this.session?.recordKind === RecordKind.GROUP;
  }

  get activeParticipants(): GroupSessionParticipant[] {
    return (this.session?.participants ?? []).filter(p => !p.removedAt);
  }

  close(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  openAttendanceDialog(): void {
    if (this.isGroupSession) {
      this.showGroupAttendancePanel = !this.showGroupAttendancePanel;
    } else {
      this.attendanceDialogVisible = true;
    }
  }

  onAttendanceRecorded(response: AttendanceOutcomeResponse): void {
    this.attendanceDialogVisible = false;
    this.session = { ...this.session, attendanceOutcome: response.effectiveOutcome };
    this.attendanceRecorded.emit(response);
  }

  onAttendanceDialogClosed(): void {
    this.attendanceDialogVisible = false;
  }

  onGroupAttendanceSaved(results: GroupAttendanceResult[]): void {
    // Update participant outcomes in the session object
    const updatedParticipants = (this.session.participants ?? []).map(p => {
      const result = results.find(r => r.clientId === p.clientId);
      if (result) {
        return { ...p, attendanceOutcome: result.response.effectiveOutcome };
      }
      return p;
    });
    this.session = { ...this.session, participants: updatedParticipants };
    this.showGroupAttendancePanel = false;
  }

  getAttendanceOutcomeI18nKey(outcome: AttendanceOutcome): string {
    const keys: Record<AttendanceOutcome, string> = {
      [AttendanceOutcome.ATTENDED]: 'sessions.attendance.outcome.attended',
      [AttendanceOutcome.NO_SHOW]: 'sessions.attendance.outcome.noShow',
      [AttendanceOutcome.LATE_CANCELLATION]: 'sessions.attendance.outcome.lateCancellation',
      [AttendanceOutcome.CANCELLED]: 'sessions.attendance.outcome.cancelled',
      [AttendanceOutcome.THERAPIST_CANCELLATION]: 'sessions.attendance.outcome.therapistCancellation',
    };
    return keys[outcome] ?? 'sessions.attendance.outcome.cancelled';
  }

  getAttendanceOutcomeSeverity(
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

  getStatusSeverity(
    status: SessionStatus
  ): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' | null {
    switch (status) {
      case SessionStatus.PENDING:
        return 'secondary';
      case SessionStatus.IN_PROGRESS:
        return 'success';
      case SessionStatus.COMPLETED:
        return 'info';
      case SessionStatus.CANCELLED:
        return 'danger';
      default:
        return 'secondary';
    }
  }

  getStatusKey(status: SessionStatus): string {
    return status.toLowerCase().replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
  }
}
