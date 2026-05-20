import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { RiskFlagTypeAdminService } from '../../services/risk-flag-type-admin.service';

@Component({
  selector: 'app-risk-flag-type-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe],
  template: `
    <div class="dialog-overlay" role="dialog"
         [attr.aria-label]="'admin.riskFlagTypes.form.title' | transloco">
      <div class="dialog">
        <div class="dialog-header">
          <h2>{{ 'admin.riskFlagTypes.form.title' | transloco }}</h2>
          <button class="dialog-close" type="button" (click)="cancel()">✕</button>
        </div>

        <div class="dialog-content">
          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

            <div class="field">
              <label for="name">
                {{ 'admin.riskFlagTypes.form.name' | transloco }}
                <span class="required">*</span>
              </label>
              <input
                id="name"
                type="text"
                formControlName="name"
                [placeholder]="'admin.riskFlagTypes.form.namePlaceholder' | transloco"
              />
              <span
                class="field-error"
                *ngIf="form.get('name')?.invalid && form.get('name')?.touched"
              >
                {{ 'admin.riskFlagTypes.form.nameRequired' | transloco }}
              </span>
            </div>

            <div class="field">
              <label for="displayOrder">
                {{ 'admin.riskFlagTypes.form.displayOrder' | transloco }}
                <span class="required">*</span>
              </label>
              <input
                id="displayOrder"
                type="number"
                formControlName="displayOrder"
                min="0"
              />
              <span
                class="field-error"
                *ngIf="form.get('displayOrder')?.invalid && form.get('displayOrder')?.touched"
              >
                {{ 'admin.riskFlagTypes.form.displayOrderRequired' | transloco }}
              </span>
            </div>

            <div *ngIf="saveError" class="alert-error" role="alert">{{ saveError }}</div>

          </form>
        </div>

        <div class="dialog-actions">
          <button type="button" class="btn-secondary" (click)="cancel()">
            {{ 'admin.riskFlagTypes.form.cancel' | transloco }}
          </button>
          <button type="button" class="btn-primary" [disabled]="saving" (click)="submit()">
            {{ saving ? ('admin.riskFlagTypes.form.saving' | transloco) :
                        ('admin.riskFlagTypes.form.save' | transloco) }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class RiskFlagTypeFormDialogComponent implements OnInit {
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  saveError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private svc: RiskFlagTypeAdminService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      displayOrder: [0, [Validators.required, Validators.min(0)]]
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.saveError = null;
    const { name, displayOrder } = this.form.value as { name: string; displayOrder: number };
    this.svc.create(name, displayOrder).subscribe({
      next: () => {
        this.saving = false;
        this.saved.emit();
      },
      error: () => {
        this.saving = false;
        this.saveError = 'Failed to create flag type. Please try again.';
      }
    });
  }
}
