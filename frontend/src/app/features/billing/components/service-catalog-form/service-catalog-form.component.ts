import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import {
  CreateServiceRequest,
  ServiceCatalogItem,
  UpdateServiceRequest,
} from '../../models/service-catalog.model';
import { AppointmentApiService } from '../../../schedule/services/appointment-api.service';
import { SessionType } from '../../../schedule/models/schedule.model';

@Component({
  selector: 'app-service-catalog-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true"
         [attr.aria-labelledby]="'catalog-form-title'">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="catalog-form-title">
            {{ (editMode ? 'billing.catalog.form.editTitle' : 'billing.catalog.form.createTitle') | transloco }}
          </h2>
          <button type="button" class="dialog-close" (click)="onCancel()"
                  [attr.aria-label]="'common.actions.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="dialog-content" novalidate>

          <!-- Name -->
          <div class="field">
            <label for="cf-name">
              {{ 'billing.catalog.fields.name' | transloco }} <span class="required">*</span>
            </label>
            <input id="cf-name" type="text" formControlName="name"
                   [placeholder]="'billing.catalog.fields.namePlaceholder' | transloco"
                   [class.is-error]="isInvalid('name')"
                   [attr.aria-describedby]="isInvalid('name') ? 'cf-name-error' : null" />
            <span *ngIf="isInvalid('name')" id="cf-name-error" class="error-msg" role="alert">
              <ng-container *ngIf="form.get('name')?.hasError('required')">
                {{ 'common.validation.required' | transloco }}
              </ng-container>
              <ng-container *ngIf="form.get('name')?.hasError('maxlength')">
                {{ 'common.validation.maxLength' | transloco : { max: 200 } }}
              </ng-container>
            </span>
          </div>

          <!-- Category -->
          <div class="field">
            <label for="cf-category">
              {{ 'billing.catalog.fields.category' | transloco }} <span class="required">*</span>
            </label>
            <input id="cf-category" type="text" formControlName="category"
                   [placeholder]="'billing.catalog.fields.categoryPlaceholder' | transloco"
                   [class.is-error]="isInvalid('category')"
                   [attr.aria-describedby]="isInvalid('category') ? 'cf-category-error' : null" />
            <span *ngIf="isInvalid('category')" id="cf-category-error" class="error-msg" role="alert">
              {{ 'common.validation.required' | transloco }}
            </span>
          </div>

          <!-- Service Type + Duration -->
          <div class="field-row">
            <div class="field">
              <label for="cf-type">
                {{ 'billing.catalog.fields.serviceType' | transloco }} <span class="required">*</span>
              </label>
              <select id="cf-type" formControlName="sessionTypeId"
                      [class.is-error]="isInvalid('sessionTypeId')">
                <option value="" disabled>— {{ 'billing.catalog.fields.serviceTypePlaceholder' | transloco }} —</option>
                <option *ngFor="let t of sessionTypes" [value]="t.id">
                  {{ 'sessions.types.' + t.code | transloco }}
                </option>
              </select>
              <span *ngIf="isInvalid('sessionTypeId')" class="error-msg" role="alert">
                {{ 'common.validation.required' | transloco }}
              </span>
            </div>

            <div class="field">
              <label for="cf-duration">
                {{ 'billing.catalog.fields.durationMin' | transloco }} <span class="required">*</span>
              </label>
              <input id="cf-duration" type="number" formControlName="durationMin"
                     min="1" step="5"
                     [class.is-error]="isInvalid('durationMin')"
                     [attr.aria-describedby]="isInvalid('durationMin') ? 'cf-duration-error' : null" />
              <span *ngIf="isInvalid('durationMin')" id="cf-duration-error" class="error-msg" role="alert">
                <ng-container *ngIf="form.get('durationMin')?.hasError('required')">
                  {{ 'common.validation.required' | transloco }}
                </ng-container>
                <ng-container *ngIf="form.get('durationMin')?.hasError('min')">
                  {{ 'billing.catalog.validation.durationMin' | transloco }}
                </ng-container>
              </span>
            </div>
          </div>

          <!-- Initial price + effective date (create mode only) -->
          <ng-container *ngIf="!editMode">
            <div class="field-row">
              <div class="field">
                <label for="cf-price">
                  {{ 'billing.catalog.fields.defaultPrice' | transloco }} <span class="required">*</span>
                </label>
                <input id="cf-price" type="number" formControlName="defaultPrice"
                       min="0" step="0.01"
                       [class.is-error]="isInvalid('defaultPrice')"
                       [attr.aria-describedby]="isInvalid('defaultPrice') ? 'cf-price-error' : null" />
                <span *ngIf="isInvalid('defaultPrice')" id="cf-price-error" class="error-msg" role="alert">
                  <ng-container *ngIf="form.get('defaultPrice')?.hasError('required')">
                    {{ 'common.validation.required' | transloco }}
                  </ng-container>
                  <ng-container *ngIf="form.get('defaultPrice')?.hasError('min')">
                    {{ 'billing.catalog.validation.priceMin' | transloco }}
                  </ng-container>
                </span>
              </div>

              <div class="field">
                <label for="cf-effective-from">
                  {{ 'billing.catalog.fields.effectiveFrom' | transloco }} <span class="required">*</span>
                </label>
                <input id="cf-effective-from" type="date" formControlName="effectiveFrom"
                       [class.is-error]="isInvalid('effectiveFrom')"
                       [attr.aria-describedby]="isInvalid('effectiveFrom') ? 'cf-effective-error' : null" />
                <span *ngIf="isInvalid('effectiveFrom')" id="cf-effective-error" class="error-msg" role="alert">
                  {{ 'common.validation.required' | transloco }}
                </span>
              </div>
            </div>
          </ng-container>

          <!-- Server error -->
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
export class ServiceCatalogFormComponent implements OnInit, OnChanges {
  @Input() editMode = false;
  @Input() service: ServiceCatalogItem | null = null;
  @Input() saving = false;
  @Input() serverError: string | null = null;

  @Output() submitted = new EventEmitter<CreateServiceRequest | UpdateServiceRequest>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  sessionTypes: SessionType[] = [];

  constructor(private fb: FormBuilder, private appointmentApi: AppointmentApiService) {}

  ngOnInit(): void {
    this.buildForm();
    this.appointmentApi.getSessionTypes().subscribe(types => {
      this.sessionTypes = types;
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['service'] && this.form) {
      this.patchForm();
    }
    if (changes['editMode'] && this.form) {
      this.adjustModeControls();
    }
  }

  private buildForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      category: ['', [Validators.required, Validators.maxLength(100)]],
      sessionTypeId: ['', Validators.required],
      durationMin: [50, [Validators.required, Validators.min(1)]],
      defaultPrice: [null, this.editMode ? [] : [Validators.required, Validators.min(0)]],
      effectiveFrom: [this.todayIso(), this.editMode ? [] : Validators.required],
    });
    this.adjustModeControls();
    if (this.service) {
      this.patchForm();
    }
  }

  private adjustModeControls(): void {
    if (!this.form) { return; }
    if (this.editMode) {
      this.form.get('defaultPrice')?.clearValidators();
      this.form.get('effectiveFrom')?.clearValidators();
      this.form.get('defaultPrice')?.updateValueAndValidity();
      this.form.get('effectiveFrom')?.updateValueAndValidity();
    } else {
      this.form.get('defaultPrice')?.setValidators([Validators.required, Validators.min(0)]);
      this.form.get('effectiveFrom')?.setValidators(Validators.required);
      this.form.get('defaultPrice')?.updateValueAndValidity();
      this.form.get('effectiveFrom')?.updateValueAndValidity();
    }
  }

  private patchForm(): void {
    if (!this.service) { return; }
    this.form.patchValue({
      name: this.service.name,
      category: this.service.category,
      sessionTypeId: this.service.sessionType.id,
      durationMin: this.service.durationMin,
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

    if (this.editMode) {
      const { name, category, sessionTypeId, durationMin } = this.form.value;
      this.submitted.emit({ name, category, sessionTypeId, durationMin } as UpdateServiceRequest);
    } else {
      this.submitted.emit(this.form.value as CreateServiceRequest);
    }
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
