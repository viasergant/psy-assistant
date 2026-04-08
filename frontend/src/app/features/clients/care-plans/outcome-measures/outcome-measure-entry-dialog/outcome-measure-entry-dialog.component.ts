import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import {
  OutcomeMeasureDefinition,
  OutcomeMeasureEntry,
  RecordOutcomeMeasureRequest,
} from '../../models/care-plan.model';
import { OutcomeMeasureService } from '../../services/outcome-measure.service';

@Component({
  selector: 'app-outcome-measure-entry-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div *ngIf="visible" class="dialog-overlay" role="dialog" aria-modal="true"
         [attr.aria-labelledby]="'om-dialog-title'">
      <div class="dialog-panel">
        <header class="dialog-header">
          <h2 id="om-dialog-title">{{ 'carePlans.outcomeMeasures.recordTitle' | transloco }}</h2>
          <button type="button" class="btn-icon" (click)="close()" [attr.aria-label]="'common.close' | transloco">✕</button>
        </header>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="dialog-body">

          <div class="field">
            <label class="field-label" for="measureSelect">
              {{ 'carePlans.outcomeMeasures.measure' | transloco }}
              <span class="required" aria-hidden="true">*</span>
            </label>
            <select id="measureSelect" class="field-select" formControlName="measureDefinitionId">
              <option value="">{{ 'carePlans.outcomeMeasures.selectMeasure' | transloco }}</option>
              <optgroup *ngFor="let group of definitionGroups" [label]="group.label">
                <option *ngFor="let def of group.items" [value]="def.id">
                  {{ def.displayName }} ({{ def.minScore }}–{{ def.maxScore }})
                </option>
              </optgroup>
            </select>
            <div *ngIf="form.get('measureDefinitionId')?.invalid && form.get('measureDefinitionId')?.touched"
                 class="field-error" role="alert">
              {{ 'carePlans.outcomeMeasures.measureRequired' | transloco }}
            </div>
          </div>

          <div class="field">
            <label class="field-label" for="scoreInput">
              {{ 'carePlans.outcomeMeasures.score' | transloco }}
              <span *ngIf="selectedDef" class="field-hint">({{ selectedDef.minScore }}–{{ selectedDef.maxScore }})</span>
              <span class="required" aria-hidden="true">*</span>
            </label>
            <input
              id="scoreInput"
              type="number"
              class="field-input"
              formControlName="score"
              [min]="selectedDef?.minScore ?? 0"
              [max]="selectedDef?.maxScore ?? 9999"
              [attr.aria-describedby]="'scoreHelp'"
            />
            <div *ngIf="form.get('score')?.invalid && form.get('score')?.touched"
                 class="field-error" role="alert" id="scoreHelp">
              {{ 'carePlans.outcomeMeasures.scoreRequired' | transloco }}
            </div>
          </div>

          <div class="field">
            <label class="field-label" for="assessmentDate">
              {{ 'carePlans.outcomeMeasures.assessmentDate' | transloco }}
              <span class="required" aria-hidden="true">*</span>
            </label>
            <input
              id="assessmentDate"
              type="date"
              class="field-input"
              formControlName="assessmentDate"
              [max]="today"
            />
            <div *ngIf="form.get('assessmentDate')?.invalid && form.get('assessmentDate')?.touched"
                 class="field-error" role="alert">
              {{ 'carePlans.outcomeMeasures.dateRequired' | transloco }}
            </div>
          </div>

          <div class="field">
            <label class="field-label" for="notes">
              {{ 'carePlans.outcomeMeasures.notes' | transloco }}
            </label>
            <textarea
              id="notes"
              class="field-textarea"
              formControlName="notes"
              rows="2"
              maxlength="1000"
              [placeholder]="'carePlans.outcomeMeasures.notesPlaceholder' | transloco"
            ></textarea>
          </div>

          <div *ngIf="saveError" class="alert-error" role="alert">{{ saveError | transloco }}</div>

          <footer class="dialog-footer">
            <button type="button" class="btn btn-ghost" (click)="close()">
              {{ 'common.cancel' | transloco }}
            </button>
            <button type="submit" class="btn btn-primary" [disabled]="saving || form.invalid">
              {{ saving ? ('common.saving' | transloco) : ('carePlans.outcomeMeasures.record' | transloco) }}
            </button>
          </footer>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .dialog-panel { background: var(--color-surface); border-radius: var(--radius-lg); width: min(500px, 95vw); max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
    .dialog-header { display: flex; justify-content: space-between; align-items: center; padding: var(--spacing-lg); border-bottom: 1px solid var(--color-border); }
    .dialog-header h2 { margin: 0; font-size: 1.1rem; }
    .dialog-body { padding: var(--spacing-lg); display: flex; flex-direction: column; gap: var(--spacing-md); }
    .dialog-footer { display: flex; justify-content: flex-end; gap: var(--spacing-sm); margin-top: var(--spacing-sm); }
    .field { display: flex; flex-direction: column; gap: 4px; }
    .field-label { font-size: 0.875rem; font-weight: 500; }
    .field-hint { font-size: 0.8rem; color: var(--color-text-muted); font-weight: 400; margin-left: 4px; }
    .required { color: #dc2626; margin-left: 2px; }
    .field-input, .field-select, .field-textarea { padding: var(--spacing-sm); border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-family: inherit; font-size: 0.875rem; }
    .field-input:focus, .field-select:focus, .field-textarea:focus { outline: 2px solid var(--color-accent); outline-offset: 1px; }
    .field-textarea { resize: vertical; }
    .field-error { font-size: 0.8rem; color: #dc2626; }
    .alert-error { padding: var(--spacing-xs) var(--spacing-sm); background: #fee2e2; color: #991b1b; border-radius: var(--radius-sm); font-size: 0.875rem; }
    .btn-icon { background: none; border: none; cursor: pointer; font-size: 1.2rem; color: var(--color-text-muted); padding: 4px; }
    .btn-icon:hover { color: var(--color-text); }
  `]
})
export class OutcomeMeasureEntryDialogComponent implements OnInit {
  @Input() planId!: string;
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() entryRecorded = new EventEmitter<OutcomeMeasureEntry>();

  form!: FormGroup;
  definitions: OutcomeMeasureDefinition[] = [];
  saving = false;
  saveError: string | null = null;
  today = new Date().toISOString().split('T')[0];

  definitionGroups: { label: string; items: OutcomeMeasureDefinition[] }[] = [];

  constructor(
    private fb: FormBuilder,
    private outcomeMeasureService: OutcomeMeasureService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      measureDefinitionId: ['', Validators.required],
      score: [null, Validators.required],
      assessmentDate: [this.today, Validators.required],
      notes: ['']
    });

    this.outcomeMeasureService.getDefinitions().subscribe({
      next: (defs) => {
        this.definitions = defs;
        this.definitionGroups = this.groupDefinitions(defs);
      }
    });
  }

  get selectedDef(): OutcomeMeasureDefinition | undefined {
    const id = this.form.get('measureDefinitionId')?.value;
    return this.definitions.find(d => d.id === id);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.value;
    const request: RecordOutcomeMeasureRequest = {
      measureDefinitionId: value.measureDefinitionId,
      score: value.score,
      assessmentDate: value.assessmentDate,
      notes: value.notes || undefined
    };

    this.saving = true;
    this.saveError = null;
    this.outcomeMeasureService.recordEntry(this.planId, request).subscribe({
      next: (entry) => {
        this.saving = false;
        this.entryRecorded.emit(entry);
        this.close();
      },
      error: () => {
        this.saveError = 'carePlans.outcomeMeasures.saveError';
        this.saving = false;
      }
    });
  }

  close(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.form.reset({ assessmentDate: this.today });
    this.saveError = null;
  }

  private groupDefinitions(
    defs: OutcomeMeasureDefinition[]
  ): { label: string; items: OutcomeMeasureDefinition[] }[] {
    const groups: Record<string, OutcomeMeasureDefinition[]> = {};
    for (const def of defs) {
      const prefix = def.code.startsWith('DASS21') ? 'DASS-21' : def.code;
      const label = prefix === def.code ? def.displayName : 'DASS-21 Battery';
      if (!groups[label]) groups[label] = [];
      groups[label].push(def);
    }
    return Object.entries(groups).map(([label, items]) => ({ label, items }));
  }
}
