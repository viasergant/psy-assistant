import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { ProgressSpinner } from 'primeng/progressspinner';
import { NoteVersion, SessionNote } from '../../models/session-note.model';
import { SessionNoteService } from '../../services/session-note.service';

/**
 * Modal for viewing the full version history of a clinical note.
 *
 * Loaded lazily when the user clicks "View History" on a note that has
 * at least one prior version. Versions are displayed oldest-first.
 */
@Component({
  selector: 'app-note-version-history-dialog',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, Dialog, Button, ProgressSpinner],
  templateUrl: './note-version-history-dialog.component.html',
  styleUrls: ['./note-version-history-dialog.component.scss'],
})
export class NoteVersionHistoryDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() sessionId!: string;
  @Input() note: SessionNote | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();

  versions: NoteVersion[] = [];
  loading = false;

  constructor(private noteService: SessionNoteService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['visible'] || changes['note']) && this.visible && this.note) {
      this.loadHistory();
    }
    if (changes['visible'] && !this.visible) {
      this.versions = [];
    }
  }

  loadHistory(): void {
    if (!this.note) return;
    this.loading = true;
    this.noteService.getVersionHistory(this.sessionId, this.note.id).subscribe({
      next: versions => {
        this.versions = versions;
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  close(): void {
    this.visibleChange.emit(false);
  }
}
