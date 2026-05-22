import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { CreateDiscountRuleRequest, DiscountScope, DiscountType } from '../../models/discount.model';
import { ServiceCatalogItem } from '../../models/service-catalog.model';

@Component({
  selector: 'app-discount-rule-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule, Select],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="dr-form-title">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="dr-form-title">{{ 'billing.discounts.form.createTitle' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="cancel.emit()"
                  [attr.aria-label]="'common.actions.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="dialog-content" novalidate>

          <!-- Name -->
          <div class="field">
            <label for="dr-name">
              {{ 'billing.discounts.fields.name' | transloco }} <span class="required">*</span>
            </label>
            <input id="dr-name" type="text" formControlName="name"
                   [placeholder]="'billing.discounts.fields.namePlaceholder' | transloco"
                   [class.is-error]="isInvalid('name')" />
            <span *ngIf="isInvalid('name')" class="error-msg" role="alert">
              <ng-container *ngIf="form.get('name')?.hasError('required')">
                {{ 'common.validation.required' | transloco }}
              </ng-container>
            </span>
          </div>

          <div class="field-row">
            <!-- Type -->
            <div class="field">
              <label for="dr-type">
                {{ 'billing.discounts.fields.type' | transloco }} <span class="required">*</span>
              </label>
              <p-select
                inputId="dr-type"
                formControlName="type"
                [options]="discountTypeOptions"
                optionLabel="label"
                optionValue="value"
                [invalid]="isInvalid('type')"
                styleClass="w-full"
              />
              <span *ngIf="isInvalid('type')" class="error-msg" role="alert">
                {{ 'common.validation.required' | transloco }}
              </span>
            </div>

            <!-- Value -->
            <div class="field">
              <label for="dr-value">
                {{ 'billing.discounts.fields.value' | transloco }} <span class="required">*</span>
                <span *ngIf="form.get('type')?.value === 'PERCENTAGE'" class="field-label-hint">(%)</span>
              </label>
              <input id="dr-value" type="number" formControlName="value" min="0.01" step="0.01"
                     [class.is-error]="isInvalid('value')" />
              <span *ngIf="form.get('type')?.value === 'PERCENTAGE' && form.get('value')?.value > 100"
                    class="field-hint text-warn">
                {{ 'billing.discounts.validation.percentOver100' | transloco }}
              </span>
              <span *ngIf="isInvalid('value')" class="error-msg" role="alert">
                {{ 'common.validation.required' | transloco }}
              </span>
            </div>
          </div>

          <!-- Scope -->
          <div class="field">
            <label for="dr-scope">
              {{ 'billing.discounts.fields.scope' | transloco }} <span class="required">*</span>
            </label>
            <p-select
              inputId="dr-scope"
              formControlName="scope"
              [options]="discountScopeOptions"
              optionLabel="label"
              optionValue="value"
              [invalid]="isInvalid('scope')"
              (onChange)="onScopeChange()"
              styleClass="w-full"
            />
            <span *ngIf="isInvalid('scope')" class="error-msg" role="alert">
              {{ 'common.validation.required' | transloco }}
            </span>
          </div>

          <!-- Client ID (scoped to client) -->
          <div *ngIf="form.get('scope')?.value === 'CLIENT'" class="field">
            <label for="dr-clientId">
              {{ 'billing.discounts.fields.clientId' | transloco }} <span class="required">*</span>
            </label>
            <input id="dr-clientId" type="text" formControlName="clientId"
                   [placeholder]="'billing.discounts.fields.clientIdPlaceholder' | transloco"
                   [class.is-error]="isInvalid('clientId')" />
            <span class="field-hint">{{ 'billing.discounts.fields.clientIdHint' | transloco }}</span>
            <span *ngIf="isInvalid('clientId')" class="error-msg" role="alert">
              {{ 'common.validation.required' | transloco }}
            </span>
          </div>

          <!-- Service (scoped to service) -->
          <div *ngIf="form.get('scope')?.value === 'SERVICE'" class="field">
            <label for="dr-serviceId">
              {{ 'billing.discounts.fields.serviceCatalogId' | transloco }} <span class="required">*</span>
            </label>
            <p-select
              inputId="dr-serviceId"
              formControlName="serviceCatalogId"
              [options]="serviceOptions"
              optionLabel="label"
              optionValue="value"
              [invalid]="isInvalid('serviceCatalogId')"
              styleClass="w-full"
            />
            <span *ngIf="isInvalid('serviceCatalogId')" class="error-msg" role="alert">
              {{ 'common.validation.required' | transloco }}
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
export class DiscountRuleFormComponent implements OnInit {
  @Input() services: ServiceCatalogItem[] = [];
  @Input() prefilledClientId: string | null = null;

  @Output() saved = new EventEmitter<CreateDiscountRuleRequest>();
  @Output() cancel = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  submitError = '';
  discountTypeOptions: { label: string; value: string }[] = [];
  discountScopeOptions: { label: string; value: string }[] = [];

  get serviceOptions(): { label: string; value: string }[] {
    return [
      { label: this.transloco.translate('billing.discounts.fields.servicePlaceholder'), value: '' },
      ...this.services.map(svc => ({ label: svc.name, value: svc.id })),
    ];
  }

  constructor(private fb: FormBuilder, private transloco: TranslocoService) {}

  ngOnInit(): void {
    this.discountTypeOptions = [
      { label: this.transloco.translate('billing.discounts.fields.typePlaceholder'), value: '' },
      { label: this.transloco.translate('billing.discounts.types.PERCENTAGE'), value: 'PERCENTAGE' },
      { label: this.transloco.translate('billing.discounts.types.FIXED_AMOUNT'), value: 'FIXED_AMOUNT' },
    ];
    this.discountScopeOptions = [
      { label: this.transloco.translate('billing.discounts.fields.scopePlaceholder'), value: '' },
      { label: this.transloco.translate('billing.discounts.scopes.CLIENT'), value: 'CLIENT' },
      { label: this.transloco.translate('billing.discounts.scopes.SERVICE'), value: 'SERVICE' },
    ];
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      type: ['', Validators.required],
      value: [null, [Validators.required, Validators.min(0.01)]],
      scope: [this.prefilledClientId ? 'CLIENT' : '', Validators.required],
      clientId: [this.prefilledClientId ?? ''],
      serviceCatalogId: [''],
    });
  }

  onScopeChange(): void {
    this.form.patchValue({ clientId: '', serviceCatalogId: '' });
    this.form.get('clientId')?.clearValidators();
    this.form.get('serviceCatalogId')?.clearValidators();
    this.form.get('clientId')?.updateValueAndValidity();
    this.form.get('serviceCatalogId')?.updateValueAndValidity();
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched));
  }

  onSubmit(): void {
    const scope: DiscountScope = this.form.get('scope')?.value;
    if (scope === 'CLIENT') {
      this.form.get('clientId')?.setValidators(Validators.required);
      this.form.get('clientId')?.updateValueAndValidity();
    } else if (scope === 'SERVICE') {
      this.form.get('serviceCatalogId')?.setValidators(Validators.required);
      this.form.get('serviceCatalogId')?.updateValueAndValidity();
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    const req: CreateDiscountRuleRequest = {
      name: v.name.trim(),
      type: v.type as DiscountType,
      value: Number(v.value),
      scope: v.scope as DiscountScope,
    };
    if (scope === 'CLIENT') {
      req.clientId = v.clientId;
    } else if (scope === 'SERVICE') {
      req.serviceCatalogId = v.serviceCatalogId;
    }
    this.saved.emit(req);
  }
}
