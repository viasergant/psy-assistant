import { CommonModule, DatePipe } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { GoalProgressNote } from '../../models/care-plan.model';
import { GoalProgressService } from '../../services/goal-progress.service';

@Component({
  selector: 'app-goal-progress-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule, DatePipe],
  template: `
    <div class="progress-panel">

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'carePlans.progress.loading' | transloco }}
      </div>

      <div *ngIf="error" class="alert-error" role="alert">{{ error | transloco }}</div>

      <!-- Notes list -->
      <div *ngIf="!loading">
        <div *ngIf="notes.length === 0 && !canManage" class="empty-state">
          {{ 'carePlans.progress.noNotes' | transloco }}
        </div>

        <ul class="notes-list" *ngIf="notes.length > 0" aria-label="{{ 'carePlans.progress.notesListLabel' | transloco }}">
          <li *ngFor="let note of notes" class="note-item">
            <div class="note-meta">
              <span class="note-author">{{ note.authorName }}</span>
              <span class="note-date">{{ note.createdAt | date:'medium' }}</span>
            </div>
            <p class="note-text">{{ note.noteText }}</p>
          </li>
        </ul>

        <!-- Add note form (write access only) -->
        <form *ngIf="canManage && isActive" class="note-form" (ngSubmit)="submitNote()" novalidate>
          <label class="field-label" for="noteText-{{ goalId }}">
            {{ 'carePlans.progress.addNote' | transloco }}
          </label>
          <textarea
            id="noteText-{{ goalId }}"
            class="note-textarea"
            [(ngModel)]="noteText"
            name="noteText"
            [placeholder]="'carePlans.progress.notePlaceholder' | transloco"
            rows="3"
            maxlength="2000"
            [attr.aria-required]="true"
          ></textarea>
          <div class="form-actions">
            <button
              type="submit"
              class="btn btn-primary btn-sm"
              [disabled]="saving || !noteText.trim()"
            >
              {{ saving ? ('carePlans.progress.saving' | transloco) : ('carePlans.progress.save' | transloco) }}
            </button>
          </div>
          <div *ngIf="saveError" class="alert-error" role="alert">{{ saveError | transloco }}</div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .progress-panel { padding: var(--spacing-sm) 0; }
    .notes-list { list-style: none; padding: 0; margin: 0 0 var(--spacing-md); display: flex; flex-direction: column; gap: var(--spacing-sm); }
    .note-item { border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: var(--spacing-sm) var(--spacing-md); background: var(--color-surface); }
    .note-meta { display: flex; gap: var(--spacing-sm); font-size: 0.8rem; color: var(--color-text-muted); margin-bottom: 4px; }
    .note-author { font-weight: 600; }
    .note-text { margin: 0; font-size: 0.875rem; white-space: pre-wrap; }
    .note-form { margin-top: var(--spacing-md); }
    .field-label { display: block; font-size: 0.875rem; font-weight: 500; margin-bottom: 4px; }
    .note-textarea { width: 100%; padding: var(--spacing-sm); border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-family: inherit; font-size: 0.875rem; resize: vertical; box-sizing: border-box; }
    .note-textarea:focus { outline: 2px solid var(--color-accent); outline-offset: 1px; }
    .form-actions { margin-top: var(--spacing-xs); display: flex; justify-content: flex-end; }
    .btn-sm { padding: 4px 12px; font-size: 0.875rem; }
    .empty-state { color: var(--color-text-muted); font-size: 0.875rem; padding: var(--spacing-sm) 0; }
    .state-msg { color: var(--color-text-muted); font-size: 0.875rem; }
    .alert-error { padding: var(--spacing-xs) var(--spacing-sm); background: #fee2e2; color: #991b1b; border-radius: var(--radius-sm); font-size: 0.875rem; margin-top: var(--spacing-xs); }
  `]
})
export class GoalProgressPanelComponent implements OnChanges {
  @Input() planId!: string;
  @Input() goalId!: string;
  @Input() canManage = false;
  @Input() isActive = false;

  notes: GoalProgressNote[] = [];
  noteText = '';
  loading = false;
  saving = false;
  error: string | null = null;
  saveError: string | null = null;

  constructor(private progressService: GoalProgressService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['planId'] || changes['goalId']) && this.planId && this.goalId) {
      this.loadNotes();
    }
  }

  submitNote(): void {
    const text = this.noteText.trim();
    if (!text) return;

    this.saving = true;
    this.saveError = null;
    this.progressService.addProgressNote(this.planId, this.goalId, { noteText: text }).subscribe({
      next: (note) => {
        this.notes.unshift(note);
        this.noteText = '';
        this.saving = false;
      },
      error: () => {
        this.saveError = 'carePlans.progress.saveError';
        this.saving = false;
      }
    });
  }

  private loadNotes(): void {
    this.loading = true;
    this.error = null;
    this.progressService.getProgressNotes(this.planId, this.goalId).subscribe({
      next: (notes) => {
        this.notes = notes;
        this.loading = false;
      },
      error: () => {
        this.error = 'carePlans.progress.loadError';
        this.loading = false;
      }
    });
  }
}
