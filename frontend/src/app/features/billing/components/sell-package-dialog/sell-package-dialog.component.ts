import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { PackageDefinition, PackageInstance, SellPackageRequest } from '../../models/package.model';
import { PackageService } from '../../services/package.service';

@Component({
  selector: 'app-sell-package-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="sell-pkg-title">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="sell-pkg-title">{{ 'billing.packages.sell.title' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="cancel.emit()"
                  [attr.aria-label]="'common.actions.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="dialog-content" novalidate>

          <!-- Package definition -->
          <div class="field">
            <label for="sp-definition">
              {{ 'billing.packages.sell.package' | transloco }} <span class="required">*</span>
            </label>
            <select id="sp-definition" formControlName="definitionId"
                    [class.is-error]="isInvalid('definitionId')"
                    (change)="onDefinitionChange()">
              <option value="">{{ 'billing.packages.sell.selectPackage' | transloco }}</option>
              <option *ngFor="let pkg of definitions" [value]="pkg.id">
                {{ pkg.name }} — {{ pkg.sessionQty }} {{ 'billing.packages.sell.sessions' | transloco }}
                @ {{ pkg.price | number:'1.2-2' }}
              </option>
            </select>
            <span *ngIf="isInvalid('definitionId')" class="error-msg" role="alert">
              {{ 'common.validation.required' | transloco }}
            </span>
          </div>

          <!-- Selected package summary card -->
          <div *ngIf="selectedDefinition" class="package-summary-card">
            <div class="pkg-summary-row">
              <span class="pkg-summary-label">{{ 'billing.packages.fields.serviceType' | transloco }}</span>
              <span>{{ 'billing.catalog.serviceTypes.' + selectedDefinition.serviceType | transloco }}</span>
            </div>
            <div class="pkg-summary-row">
              <span class="pkg-summary-label">{{ 'billing.packages.fields.sessionQty' | transloco }}</span>
              <span>{{ selectedDefinition.sessionQty }} {{ 'billing.packages.sell.sessions' | transloco }}</span>
            </div>
            <div class="pkg-summary-row">
              <span class="pkg-summary-label">{{ 'billing.packages.fields.price' | transloco }}</span>
              <span class="price-cell">{{ selectedDefinition.price | number:'1.2-2' }}</span>
            </div>
            <div class="pkg-summary-row">
              <span class="pkg-summary-label">{{ 'billing.packages.fields.perSession' | transloco }}</span>
              <span class="muted">{{ selectedDefinition.perSessionDisplay | number:'1.2-2' }}</span>
            </div>
          </div>

          <div class="field-row">
            <!-- Purchase date -->
            <div class="field">
              <label for="sp-purchasedAt">
                {{ 'billing.packages.sell.purchaseDate' | transloco }} <span class="required">*</span>
              </label>
              <input id="sp-purchasedAt" type="date" formControlName="purchasedAt"
                     [class.is-error]="isInvalid('purchasedAt')" />
              <span *ngIf="isInvalid('purchasedAt')" class="error-msg" role="alert">
                {{ 'common.validation.required' | transloco }}
              </span>
            </div>

            <!-- Validity days (optional) -->
            <div class="field">
              <label for="sp-validityDays">
                {{ 'billing.packages.sell.validityDays' | transloco }}
              </label>
              <input id="sp-validityDays" type="number" formControlName="validityDays"
                     min="1" [placeholder]="'billing.packages.sell.validityDaysPlaceholder' | transloco" />
              <span class="field-hint">{{ 'billing.packages.sell.validityDaysHint' | transloco }}</span>
            </div>
          </div>

          <div *ngIf="submitError" class="alert-error" role="alert">{{ submitError }}</div>

          <p class="field-hint">
            {{ 'billing.packages.sell.invoiceNote' | transloco }}
          </p>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="cancel.emit()">
              {{ 'common.actions.cancel' | transloco }}
            </button>
            <button type="submit" class="btn-primary" [disabled]="saving">
              {{ saving
                  ? ('common.actions.saving' | transloco)
                  : ('billing.packages.sell.confirm' | transloco) }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .package-summary-card {
      background: var(--color-surface, #F8FAFC);
      border: 1px solid var(--color-border, #E2E8F0);
      border-radius: var(--radius-md, 8px);
      padding: var(--spacing-md, 1rem);
      margin-bottom: var(--spacing-md, 1rem);
    }
    .pkg-summary-row {
      display: flex;
      justify-content: space-between;
      padding: .25rem 0;
      font-size: .9rem;
    }
    .pkg-summary-label {
      color: var(--color-text-secondary, #64748B);
      font-size: .875rem;
    }
  `],
})
export class SellPackageDialogComponent implements OnInit {
  @Input() clientId!: string;
  @Input() definitions: PackageDefinition[] = [];

  @Output() sold = new EventEmitter<PackageInstance>();
  @Output() cancel = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  submitError = '';
  selectedDefinition: PackageDefinition | null = null;

  constructor(
    private fb: FormBuilder,
    private packageService: PackageService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      definitionId: ['', Validators.required],
      purchasedAt: [new Date().toISOString().slice(0, 10), Validators.required],
      validityDays: [null],
    });
  }

  onDefinitionChange(): void {
    const id = this.form.get('definitionId')?.value;
    this.selectedDefinition = this.definitions.find((d) => d.id === id) ?? null;
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
    this.saving = true;
    this.submitError = '';
    const v = this.form.value;
    const req: SellPackageRequest = {
      definitionId: v.definitionId,
      clientId: this.clientId,
      purchasedAt: new Date(v.purchasedAt).toISOString(),
      validityDays: v.validityDays ? Number(v.validityDays) : null,
    };
    this.packageService.sellPackage(req).subscribe({
      next: (instance) => {
        this.saving = false;
        this.sold.emit(instance);
      },
      error: () => {
        this.saving = false;
        this.submitError = 'billing.packages.sell.error';
      },
    });
  }
}
