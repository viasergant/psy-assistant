import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { Editor } from 'primeng/editor';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import {
  CreateNoteRequest,
  NoteTemplate,
  NoteType,
  NoteVisibility,
  SessionNote,
  UpdateNoteRequest,
} from '../../models/session-note.model';
import { SessionNoteService } from '../../services/session-note.service';

interface NoteTypeOption {
  label: string;
  value: NoteType;
}

/**
 * Dialog for creating or editing a clinical session note.
 *
 * Supports free-form rich text (via PrimeNG Editor / Quill) and structured
 * notes using a global template (4 fields: presenting problem, interventions
 * used, client response, plan for next session).
 */
@Component({
  selector: 'app-note-editor-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslocoPipe,
    Dialog,
    Button,
    Editor,
    Checkbox,
    Select,
    Textarea,
  ],
  templateUrl: './note-editor-dialog.component.html',
  styleUrls: ['./note-editor-dialog.component.scss'],
})
export class NoteEditorDialogComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() sessionId!: string;
  @Input() existingNote: SessionNote | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() noteSaved = new EventEmitter<SessionNote>();

  noteType: NoteType = 'FREE_FORM';
  content = '';
  supervisorVisible = false;
  saving = false;

  templates: NoteTemplate[] = [];
  selectedTemplate: NoteTemplate | null = null;
  structuredFieldValues: Record<string, string> = {};

  noteTypeOptions: NoteTypeOption[] = [];

  constructor(private noteService: SessionNoteService) {}

  ngOnInit(): void {
    this.noteTypeOptions = [
      { label: 'notes.types.freeForm', value: 'FREE_FORM' },
      { label: 'notes.types.structured', value: 'STRUCTURED' },
    ];
    this.noteService.getTemplates().subscribe({
      next: templates => {
        this.templates = templates;
        if (templates.length > 0) {
          this.selectedTemplate = templates[0];
          this.initStructuredFields();
        }
      },
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.resetForm();
    }
    if (changes['existingNote'] && this.existingNote) {
      this.populateFromExistingNote();
    }
  }

  get isEdit(): boolean {
    return this.existingNote !== null;
  }

  get canSave(): boolean {
    if (this.noteType === 'FREE_FORM') {
      return this.content.trim().length > 0;
    }
    if (!this.selectedTemplate) return false;
    return this.selectedTemplate.fields
      .filter(f => f.required)
      .every(f => (this.structuredFieldValues[f.key] ?? '').trim().length > 0);
  }

  onNoteTypeChange(): void {
    if (this.noteType === 'STRUCTURED' && this.selectedTemplate) {
      this.initStructuredFields();
    }
  }

  initStructuredFields(): void {
    if (!this.selectedTemplate) return;
    const existing = this.existingNote?.structuredFields ?? {};
    this.structuredFieldValues = Object.fromEntries(
      this.selectedTemplate.fields.map(f => [f.key, existing[f.key] ?? ''])
    );
  }

  save(): void {
    if (!this.canSave || this.saving) return;

    const visibility: NoteVisibility = this.supervisorVisible
      ? 'SUPERVISOR_VISIBLE'
      : 'PRIVATE';

    this.saving = true;

    if (this.isEdit && this.existingNote) {
      const req: UpdateNoteRequest = { visibility };
      if (this.noteType === 'FREE_FORM') {
        req.content = this.content;
      } else {
        req.structuredFields = { ...this.structuredFieldValues };
      }
      this.noteService.updateNote(this.sessionId, this.existingNote.id, req).subscribe({
        next: note => this.handleSaved(note),
        error: () => { this.saving = false; },
      });
    } else {
      const req: CreateNoteRequest = {
        noteType: this.noteType,
        visibility,
      };
      if (this.noteType === 'FREE_FORM') {
        req.content = this.content;
      } else {
        req.structuredFields = { ...this.structuredFieldValues };
      }
      this.noteService.createNote(this.sessionId, req).subscribe({
        next: note => this.handleSaved(note),
        error: () => { this.saving = false; },
      });
    }
  }

  close(): void {
    this.visibleChange.emit(false);
  }

  private handleSaved(note: SessionNote): void {
    this.saving = false;
    this.noteSaved.emit(note);
  }

  private resetForm(): void {
    if (!this.existingNote) {
      this.noteType = 'FREE_FORM';
      this.content = '';
      this.supervisorVisible = false;
      this.structuredFieldValues = {};
      if (this.selectedTemplate) {
        this.initStructuredFields();
      }
    }
  }

  private populateFromExistingNote(): void {
    if (!this.existingNote) return;
    this.noteType = this.existingNote.noteType;
    this.content = this.existingNote.content ?? '';
    this.supervisorVisible = this.existingNote.visibility === 'SUPERVISOR_VISIBLE';
    if (this.noteType === 'STRUCTURED' && this.existingNote.structuredFields) {
      this.structuredFieldValues = { ...this.existingNote.structuredFields };
    }
  }
}
