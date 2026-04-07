import { CommonModule } from '@angular/common';
import {
  Component,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
} from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { Button } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Tag } from 'primeng/tag';
import { SessionNote } from '../../models/session-note.model';
import { SessionNoteService } from '../../services/session-note.service';
import { NoteEditorDialogComponent } from '../note-editor-dialog/note-editor-dialog.component';
import { NoteVersionHistoryDialogComponent } from '../note-version-history-dialog/note-version-history-dialog.component';

/**
 * Panel displaying clinical notes for a session record.
 *
 * Handles RBAC transparently: if the backend returns HTTP 403, a
 * permission-denied message is shown instead of notes.
 */
@Component({
  selector: 'app-session-notes-panel',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoPipe,
    Button,
    Tag,
    ProgressSpinner,
    NoteEditorDialogComponent,
    NoteVersionHistoryDialogComponent,
  ],
  templateUrl: './session-notes-panel.component.html',
  styleUrls: ['./session-notes-panel.component.scss'],
})
export class SessionNotesPanelComponent implements OnInit, OnChanges {
  @Input() sessionId!: string;
  @Input() active = false;
  @Input() readonly = false;

  notes: SessionNote[] = [];
  loading = false;
  forbidden = false;

  editorVisible = false;
  editingNote: SessionNote | null = null;

  historyVisible = false;
  historyNote: SessionNote | null = null;

  private loaded = false;

  constructor(private noteService: SessionNoteService) {}

  ngOnInit(): void {
    if (this.active) {
      this.loadNotes();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['active'] && this.active && !this.loaded) {
      this.loadNotes();
    }
  }

  loadNotes(): void {
    this.loading = true;
    this.forbidden = false;
    this.noteService.getNotes(this.sessionId).subscribe({
      next: notes => {
        this.notes = notes;
        this.loading = false;
        this.loaded = true;
      },
      error: err => {
        this.loading = false;
        this.loaded = true;
        if (this.noteService.isForbiddenError(err)) {
          this.forbidden = true;
        }
      },
    });
  }

  openCreateEditor(): void {
    this.editingNote = null;
    this.editorVisible = true;
  }

  openEditEditor(note: SessionNote): void {
    this.editingNote = note;
    this.editorVisible = true;
  }

  openVersionHistory(note: SessionNote): void {
    this.historyNote = note;
    this.historyVisible = true;
  }

  onNoteSaved(note: SessionNote): void {
    const idx = this.notes.findIndex(n => n.id === note.id);
    if (idx >= 0) {
      this.notes = [...this.notes.slice(0, idx), note, ...this.notes.slice(idx + 1)];
    } else {
      this.notes = [...this.notes, note];
    }
    this.editorVisible = false;
  }

  getVisibilitySeverity(visibility: string): 'success' | 'warn' {
    return visibility === 'SUPERVISOR_VISIBLE' ? 'success' : 'warn';
  }

  getVisibilityKey(visibility: string): string {
    return visibility === 'SUPERVISOR_VISIBLE'
      ? 'notes.visibility.supervisorVisible'
      : 'notes.visibility.private';
  }
}
