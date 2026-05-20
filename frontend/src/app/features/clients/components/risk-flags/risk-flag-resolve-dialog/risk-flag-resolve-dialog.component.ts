import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { RiskFlagService } from '../services/risk-flag.service';

@Component({
  selector: 'app-risk-flag-resolve-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" (click)="onOverlayClick($event)">
      <div class="dialog" role="dialog" aria-label="Resolve Risk Flag">
        <div class="dialog-header">
          <h2>Resolve Risk Flag</h2>
          <button type="button" class="dialog-close" (click)="cancelled.emit()" aria-label="Close">&#x2715;</button>
        </div>

        <div class="dialog-content">
          <p class="dialog-description">
            Describe how this risk flag has been addressed or resolved.
          </p>

          <form [formGroup]="form" (ngSubmit)="submit()">

            <div class="field">
              <label>Resolution Note <span class="required">*</span></label>
              <textarea formControlName="resolutionNote" rows="5"
                        placeholder="Describe the resolution…"></textarea>
              <span class="error-msg"
                    *ngIf="form.get('resolutionNote')!.invalid && form.get('resolutionNote')!.touched">
                Resolution note is required.
              </span>
            </div>

            <div *ngIf="submitError" class="alert-error" role="alert">{{ submitError }}</div>

            <div class="dialog-actions">
              <button type="button" class="btn-secondary" (click)="cancelled.emit()">
                Cancel
              </button>
              <button type="submit" class="btn-primary"
                      [disabled]="form.invalid || saving">
                {{ saving ? 'Resolving…' : 'Resolve Flag' }}
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
    .dialog-description { margin: 0 0 1rem; font-size: .9rem; color: #64748B; }
    .field { display: flex; flex-direction: column; gap: .35rem; margin-bottom: 1rem; }
    .field label { font-size: .875rem; font-weight: 500; color: #374151; }
    .field textarea {
      border: 1px solid #CBD5E1; border-radius: 8px; padding: .5rem .7rem;
      font: inherit; background: #fff; color: #1E293B; resize: vertical;
    }
    .field textarea:focus {
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
export class RiskFlagResolveDialogComponent implements OnInit {
  @Input({ required: true }) clientId!: string;
  @Input({ required: true }) flagId!: string;
  @Output() resolved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  submitError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private riskFlagService: RiskFlagService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      resolutionNote: ['', Validators.required]
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

    const raw = this.form.getRawValue() as { resolutionNote: string };

    this.riskFlagService.resolve(this.clientId, this.flagId, {
      resolutionNote: raw.resolutionNote.trim()
    }).subscribe({
      next: () => {
        this.saving = false;
        this.resolved.emit();
      },
      error: () => {
        this.saving = false;
        this.submitError = 'Failed to resolve risk flag. Please try again.';
      }
    });
  }
}
