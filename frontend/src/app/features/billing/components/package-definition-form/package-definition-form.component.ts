import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { CreatePackageDefinitionRequest, ServiceType } from '../../models/package.model';

export const SERVICE_TYPES: ServiceType[] = [
  'INDIVIDUAL_SESSION',
  'GROUP_SESSION',
  'INTAKE_ASSESSMENT',
  'FOLLOW_UP',
];

@Component({
  selector: 'app-package-definition-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="pkg-form-title">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="pkg-form-title">{{ 'billing.packages.form.createTitle' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="cancel.emit()"
                  [attr.aria-label]="'common.actions.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="dialog-content" novalidate>

          <div class="field">
            <label for="pkg-name">
              {{ 'billing.packages.fields.name' | transloco }} <span class="required">*</span>
            </label>
            <input id="pkg-name" type="text" formControlName="name"
                   [placeholder]="'billing.packages.fields.namePlaceholder' | transloco"
                   [class.is-error]="isInvalid('name')" />
            <span *ngIf="isInvalid('name')" class="error-msg" role="alert">
              <ng-container *ngIf="form.get('name')?.hasError('required')">
                {{ 'common.validation.required' | transloco }}
              </ng-container>
              <ng-container *ngIf="form.get('name')?.hasError('maxlength')">
                {{ 'common.validation.maxLength' | transloco : { max: 200 } }}
              </ng-container>
            </span>
          </div>

          <div class="field-row">
            <div class="field">
              <label for="pkg-serviceType">
                {{ 'billing.packages.fields.serviceType' | transloco }} <span class="required">*</span>
              </label>
              <select id="pkg-serviceType" formControlName="serviceType"
                      [class.is-error]="isInvalid('serviceType')">
                <option value="">{{ 'billing.packages.fields.serviceTypePlaceholder' | transloco }}</option>
                <option *ngFor="let t of serviceTypes" [value]="t">
                  {{ 'billing.catalog.serviceTypes.' + t | transloco }}
                </option>
              </select>
              <span *ngIf="isInvalid('serviceType')" class="error-msg" role="alert">
                {{ 'common.validation.required' | transloco }}
              </span>
            </div>

            <div class="field">
              <label for="pkg-sessionQty">
                {{ 'billing.packages.fields.sessionQty' | transloco }} <span class="required">*</span>
              </label>
              <input id="pkg-sessionQty" type="number" formControlName="sessionQty" min="1"
                     [class.is-error]="isInvalid('sessionQty')" />
              <span *ngIf="isInvalid('sessionQty')" class="error-msg" role="alert">
                <ng-container *ngIf="form.get('sessionQty')?.hasError('required')">
                  {{ 'common.validation.required' | transloco }}
                </ng-container>
                <ng-container *ngIf="form.get('sessionQty')?.hasError('min')">
                  {{ 'billing.packages.validation.sessionQtyMin' | transloco }}
                </ng-container>
              </span>
            </div>
          </div>

          <div class="field">
            <label for="pkg-price">
              {{ 'billing.packages.fields.price' | transloco }} <span class="required">*</span>
            </label>
            <input id="pkg-price" type="number" formControlName="price" min="0" step="0.01"
                   [class.is-error]="isInvalid('price')" />
            <span *ngIf="isInvalid('price')" class="error-msg" role="alert">
              <ng-container *ngIf="form.get('price')?.hasError('required')">
                {{ 'common.validation.required' | transloco }}
              </ng-container>
              <ng-container *ngIf="form.get('price')?.hasError('min')">
                {{ 'billing.packages.validation.priceMin' | transloco }}
              </ng-container>
            </span>
            <span *ngIf="perSessionHint" class="field-hint">
              {{ 'billing.packages.fields.perSessionHint' | transloco : { price: perSessionHint } }}
            </span>
          </div>

          <div *ngIf="submitError" class="alert-error" role="alert">{{ submitError }}</div>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="cancel.emit()">
              {{ 'common.actions.cancel' | transloco }}
            </button>
            <button type="submit" class="btn-primary" [disabled]="saving">
              {{ saving ? ('common.actions.saving' | transloco) : ('common.actions.save' | transloco) }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class PackageDefinitionFormComponent implements OnInit {
  @Output() saved = new EventEmitter<CreatePackageDefinitionRequest>();
  @Output() cancel = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  submitError = '';
  serviceTypes = SERVICE_TYPES;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      serviceType: ['', Validators.required],
      sessionQty: [null, [Validators.required, Validators.min(1)]],
      price: [null, [Validators.required, Validators.min(0)]],
    });
  }

  get perSessionHint(): string | null {
    const qty = this.form.get('sessionQty')?.value;
    const price = this.form.get('price')?.value;
    if (qty > 0 && price >= 0) {
      return (price / qty).toFixed(2);
    }
    return null;
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched));
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    this.saved.emit({
      name: v.name.trim(),
      serviceType: v.serviceType,
      sessionQty: Number(v.sessionQty),
      price: Number(v.price),
    });
  }
}
