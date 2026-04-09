import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { TherapistManagementService } from '../../../admin/therapists/services/therapist-management.service';
import { TherapistProfile } from '../../../admin/therapists/models/therapist.model';
import { TherapistOverride, UpsertTherapistOverrideRequest } from '../../models/service-catalog.model';

export interface OverrideSubmission {
  therapistId: string;
  request: UpsertTherapistOverrideRequest;
}

@Component({
  selector: 'app-therapist-override-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true"
         aria-labelledby="override-dialog-title">
      <div class="dialog dialog-sm">
        <div class="dialog-header">
          <h2 id="override-dialog-title">{{ 'billing.catalog.override.title' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="onCancel()"
                  [attr.aria-label]="'common.actions.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="dialog-content" novalidate>

          <!-- Therapist select (only for new override) -->
          <div class="field" *ngIf="!editOverride">
            <label for="ov-therapist">
              {{ 'billing.catalog.override.therapist' | transloco }} <span class="required">*</span>
            </label>
            <div *ngIf="loadingTherapists" class="state-msg">
              {{ 'common.status.loading' | transloco }}
            </div>
            <select *ngIf="!loadingTherapists" id="ov-therapist" formControlName="therapistId"
                    [class.is-error]="isInvalid('therapistId')">
              <option value="" disabled>— {{ 'billing.catalog.override.selectTherapist' | transloco }} —</option>
              <option *ngFor="let t of availableTherapists" [value]="t.id">
                {{ t.name }}
              </option>
            </select>
            <span *ngIf="isInvalid('therapistId')" class="error-msg" role="alert">
              {{ 'common.validation.required' | transloco }}
            </span>
          </div>

          <!-- When editing, show therapist name (read-only) -->
          <div class="field" *ngIf="editOverride">
            <label>{{ 'billing.catalog.override.therapist' | transloco }}</label>
            <p class="read-only-value">{{ editOverride.therapistName }}</p>
          </div>

          <!-- Override price -->
          <div class="field">
            <label for="ov-price">
              {{ 'billing.catalog.override.price' | transloco }} <span class="required">*</span>
            </label>
            <input id="ov-price" type="number" formControlName="price"
                   min="0" step="0.01"
                   [class.is-error]="isInvalid('price')"
                   [attr.aria-describedby]="isInvalid('price') ? 'ov-price-error' : null" />
            <span *ngIf="isInvalid('price')" id="ov-price-error" class="error-msg" role="alert">
              <ng-container *ngIf="form.get('price')?.hasError('required')">
                {{ 'common.validation.required' | transloco }}
              </ng-container>
              <ng-container *ngIf="form.get('price')?.hasError('min')">
                {{ 'billing.catalog.validation.priceMin' | transloco }}
              </ng-container>
            </span>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">{{ serverError }}</div>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="onCancel()" [disabled]="saving">
              {{ 'common.actions.cancel' | transloco }}
            </button>
            <button type="submit" class="btn-primary" [disabled]="saving || form.invalid">
              <span *ngIf="saving">{{ 'common.status.loading' | transloco }}</span>
              <span *ngIf="!saving">{{ 'common.actions.save' | transloco }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class TherapistOverrideDialogComponent implements OnInit {
  @Input() existingOverrides: TherapistOverride[] = [];
  @Input() editOverride: TherapistOverride | null = null;
  @Input() saving = false;
  @Input() serverError: string | null = null;

  @Output() submitted = new EventEmitter<OverrideSubmission>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  availableTherapists: TherapistProfile[] = [];
  loadingTherapists = false;

  constructor(
    private fb: FormBuilder,
    private therapistService: TherapistManagementService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      therapistId: [this.editOverride?.therapistId ?? '', this.editOverride ? [] : Validators.required],
      price: [this.editOverride?.price ?? null, [Validators.required, Validators.min(0)]],
    });

    if (!this.editOverride) {
      this.loadTherapists();
    }
  }

  private loadTherapists(): void {
    this.loadingTherapists = true;
    this.therapistService.getTherapists(0, 100, undefined, true).subscribe({
      next: (page) => {
        const existingIds = new Set(this.existingOverrides.map(o => o.therapistId));
        this.availableTherapists = page.content.filter(t => !existingIds.has(t.id));
        this.loadingTherapists = false;
      },
      error: () => {
        this.loadingTherapists = false;
      },
    });
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl.touched);
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { return; }

    const therapistId = this.editOverride
      ? this.editOverride.therapistId
      : this.form.value.therapistId;

    this.submitted.emit({
      therapistId,
      request: { price: this.form.value.price },
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
