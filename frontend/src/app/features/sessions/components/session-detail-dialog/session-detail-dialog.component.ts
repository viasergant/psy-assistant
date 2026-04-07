import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { DialogModule } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { SessionRecord, SessionStatus } from '../../models/session.model';
import { SessionNotesPanelComponent } from '../session-notes-panel/session-notes-panel.component';

/**
 * Session detail dialog component for viewing complete session information
 * @class SessionDetailDialogComponent
 */
@Component({
  selector: 'app-session-detail-dialog',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, DialogModule, Button, Tag, SessionNotesPanelComponent],
  templateUrl: './session-detail-dialog.component.html',
  styleUrls: ['./session-detail-dialog.component.scss'],
})
export class SessionDetailDialogComponent {
  @Input() visible = false;
  @Input() session!: SessionRecord;
  @Output() visibleChange = new EventEmitter<boolean>();

  SessionStatus = SessionStatus;

  close(): void {
    this.visible = false;
    this.visibleChange.emit(false);
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
