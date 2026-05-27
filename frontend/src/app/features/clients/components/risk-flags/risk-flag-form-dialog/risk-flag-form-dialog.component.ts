import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { RiskFlagType } from '../models/risk-flag.model';
import { RiskFlagService } from '../services/risk-flag.service';

@Component({
  selector: 'app-risk-flag-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule, Select],
  template: `
    <div class="dialog-overlay" (click)="onOverlayClick($event)">
      <div class="dialog" role="dialog" [attr.aria-label]="'admin.riskFlags.form.title' | transloco">
        <div class="dialog-header">
          <h2>{{ 'admin.riskFlags.form.title' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="cancelled.emit()" [attr.aria-label]="'common.actions.close' | transloco">&#x2715;</button>
        </div>

        <div class="dialog-content">
          <form [formGroup]="form" (ngSubmit)="submit()">

            <div class="field">
              <label>{{ 'admin.riskFlags.form.flagType.label' | transloco }} <span class="required">*</span></label>
              <p-select
                formControlName="flagTypeId"
                [options]="flagTypeOptions"
                optionLabel="label"
                optionValue="value"
                styleClass="w-full"
              />
              <span class="error-msg"
                    *ngIf="form.get('flagTypeId')!.invalid && form.get('flagTypeId')!.touched">
                {{ 'admin.riskFlags.form.flagType.required' | transloco }}
              </span>
            </div>

            <div class="field">
              <label>{{ 'admin.riskFlags.form.reviewDate.label' | transloco }} <span class="required">*</span></label>
              <input type="date" formControlName="reviewDate" [min]="today" />
              <span class="error-msg"
                    *ngIf="form.get('reviewDate')!.invalid && form.get('reviewDate')!.touched">
                {{ 'admin.riskFlags.form.reviewDate.required' | transloco }}
              </span>
            </div>

            <div class="field">
              <label>{{ 'admin.riskFlags.form.clinicalNote' | transloco }}</label>
              <textarea formControlName="clinicalNote" rows="4"
                        [placeholder]="'admin.riskFlags.clinicalNote.placeholder' | transloco"></textarea>
            </div>

            <div *ngIf="submitError" class="alert-error" role="alert">{{ submitError }}</div>

            <div class="dialog-actions">
              <button type="button" class="btn-secondary" (click)="cancelled.emit()">
                {{ 'common.actions.cancel' | transloco }}
              </button>
              <button type="submit" class="btn-primary"
                      [disabled]="form.invalid || saving">
                {{ saving ? ('admin.riskFlags.form.saving' | transloco) : ('admin.riskFlags.form.submit' | transloco) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,.45);
      display: flex; align-items: center; justify-content: center; z-index: 1000;
    }
    .dialog {
      background: #fff; border-radius: 12px; width: 100%; max-width: 480px;
      box-shadow: 0 8px 32px rgba(0,0,0,.18); overflow: hidden;
    }
    .dialog-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 1.25rem 1.5rem; border-bottom: 1px solid #E2E8F0;
    }
    .dialog-header h2 { margin: 0; font-size: 1.1rem; }
    .dialog-close {
      border: 0; background: transparent; color: #64748B;
      font-size: 1.1rem; cursor: pointer; line-height: 1;
    }
    .dialog-close:hover { color: #1E293B; }
    .dialog-content { padding: 1.5rem; }
    .field { display: flex; flex-direction: column; gap: .35rem; margin-bottom: 1rem; }
    .field label { font-size: .875rem; font-weight: 500; color: #374151; }
    .field input, .field select, .field textarea {
      border: 1px solid #CBD5E1; border-radius: 8px; padding: .5rem .7rem;
      font: inherit; background: #fff; color: #1E293B;
    }
    .field input:focus, .field select:focus, .field textarea:focus {
      outline: none; border-color: #0EA5A0; box-shadow: 0 0 0 2px rgba(14,165,160,.15);
    }
    .required { color: #DC2626; }
    .error-msg { color: #DC2626; font-size: .8rem; }
    .alert-error {
      padding: .65rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; margin-bottom: .75rem; font-size: .875rem;
    }
    .dialog-actions {
      display: flex; justify-content: flex-end; gap: .5rem; margin-top: 1.25rem;
    }
    .btn-primary, .btn-secondary {
      border-radius: 8px; padding: .5rem .9rem; border: 1px solid transparent;
      font-weight: 600; cursor: pointer; font-size: .875rem;
    }
    .btn-primary { background: #0EA5A0; color: #fff; }
    .btn-primary:disabled { opacity: .6; cursor: default; }
    .btn-secondary { background: #fff; border-color: #CBD5E1; color: #334155; }
    .btn-secondary:hover { background: #F8FAFC; }
  `]
})
export class RiskFlagFormDialogComponent implements OnInit {
  @Input({ required: true }) clientId!: string;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  flagTypes: RiskFlagType[] = [];
  saving = false;
  submitError: string | null = null;

  get flagTypeOptions(): { label: string; value: string }[] {
    return [
      { label: this.transloco.translate('admin.riskFlags.form.flagType.placeholder'), value: '' },
      ...this.flagTypes.map(t => ({ label: this.localizeTypeName(t.name), value: t.id })),
    ];
  }

  private localizeTypeName(name: string): string {
    const key = `admin.riskFlagTypes.names.${name.toLowerCase().replace(/[^a-z0-9]+/g, '_')}`;
    const translated = this.transloco.translate(key);
    return translated === key ? name : translated;
  }

  /** ISO date for the [min] attribute on the date input (today). */
  readonly today: string = new Date().toISOString().split('T')[0];

  constructor(
    private fb: FormBuilder,
    private riskFlagService: RiskFlagService,
    private transloco: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      flagTypeId: ['', Validators.required],
      reviewDate: ['', Validators.required],
      clinicalNote: ['']
    });

    this.riskFlagService.listTypes().subscribe({
      next: (types) => { this.flagTypes = types; }
    });
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-overlay')) {
      this.cancelled.emit();
    }
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.submitError = null;

    const raw = this.form.getRawValue() as { flagTypeId: string; reviewDate: string; clinicalNote: string };
    const payload = {
      flagTypeId: raw.flagTypeId,
      reviewDate: raw.reviewDate,
      clinicalNote: raw.clinicalNote?.trim() || null
    };

    this.riskFlagService.create(this.clientId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.saved.emit();
      },
      error: () => {
        this.saving = false;
        this.submitError = 'Failed to add risk flag. Please try again.';
      }
    });
  }
}
