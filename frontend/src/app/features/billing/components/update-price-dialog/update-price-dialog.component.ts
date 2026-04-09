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
import { UpdateDefaultPriceRequest } from '../../models/service-catalog.model';

@Component({
  selector: 'app-update-price-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true"
         aria-labelledby="update-price-title">
      <div class="dialog dialog-sm">
        <div class="dialog-header">
          <h2 id="update-price-title">{{ 'billing.catalog.updatePrice.title' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="onCancel()"
                  [attr.aria-label]="'common.actions.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="dialog-content" novalidate>

          <!-- Current price info -->
          <div class="info-banner" *ngIf="currentPrice !== null">
            <span class="info-banner__label">{{ 'billing.catalog.updatePrice.current' | transloco }}</span>
            <span class="info-banner__value">{{ currentPrice | number:'1.2-2' }}</span>
          </div>

          <!-- New price -->
          <div class="field">
            <label for="up-price">
              {{ 'billing.catalog.updatePrice.newPrice' | transloco }} <span class="required">*</span>
            </label>
            <input id="up-price" type="number" formControlName="price"
                   min="0" step="0.01"
                   [class.is-error]="isInvalid('price')"
                   [attr.aria-describedby]="isInvalid('price') ? 'up-price-error' : null" />
            <span *ngIf="isInvalid('price')" id="up-price-error" class="error-msg" role="alert">
              <ng-container *ngIf="form.get('price')?.hasError('required')">
                {{ 'common.validation.required' | transloco }}
              </ng-container>
              <ng-container *ngIf="form.get('price')?.hasError('min')">
                {{ 'billing.catalog.validation.priceMin' | transloco }}
              </ng-container>
            </span>
          </div>

          <!-- Effective from date -->
          <div class="field">
            <label for="up-effective">
              {{ 'billing.catalog.fields.effectiveFrom' | transloco }} <span class="required">*</span>
            </label>
            <input id="up-effective" type="date" formControlName="effectiveFrom"
                   [class.is-error]="isInvalid('effectiveFrom')"
                   [attr.aria-describedby]="isInvalid('effectiveFrom') ? 'up-effective-error' : null" />
            <span *ngIf="isInvalid('effectiveFrom')" id="up-effective-error" class="error-msg" role="alert">
              {{ 'common.validation.required' | transloco }}
            </span>
          </div>

          <p class="hint-text">{{ 'billing.catalog.updatePrice.hint' | transloco }}</p>

          <div *ngIf="serverError" class="alert-error" role="alert">{{ serverError }}</div>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="onCancel()" [disabled]="saving">
              {{ 'common.actions.cancel' | transloco }}
            </button>
            <button type="submit" class="btn-primary" [disabled]="saving || form.invalid">
              <span *ngIf="saving">{{ 'common.status.loading' | transloco }}</span>
              <span *ngIf="!saving">{{ 'billing.catalog.updatePrice.confirm' | transloco }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class UpdatePriceDialogComponent implements OnInit {
  @Input() currentPrice: number | null = null;
  @Input() saving = false;
  @Input() serverError: string | null = null;

  @Output() submitted = new EventEmitter<UpdateDefaultPriceRequest>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      price: [null, [Validators.required, Validators.min(0)]],
      effectiveFrom: [this.todayIso(), Validators.required],
    });
  }

  private todayIso(): string {
    return new Date().toISOString().split('T')[0];
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl.touched);
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { return; }
    this.submitted.emit(this.form.value as UpdateDefaultPriceRequest);
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
