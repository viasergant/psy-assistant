import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { TherapistManagementService } from '../../services/therapist-management.service';
import {
  EMPLOYMENT_STATUS_OPTIONS,
  EMPLOYMENT_STATUS_LABELS,
  IdNamePair
} from '../../models/therapist.model';
import { UserCreationResponse } from '../../../users/models/user.model';
import { TherapistAccountCreatedModalComponent } from '../../../components/therapist-account-created-modal/therapist-account-created-modal.component';
import { TranslateSpecializationPipe } from '../../pipes/translate-specialization.pipe';

interface TherapistWithAccountResponseDto {
  userDetails: UserCreationResponse;
  therapistProfile: any;
}

/**
 * Modal dialog for creating a new therapist account with temporary password.
 *
 * Simplified creation flow:
 * - Admin provides: name, email, phone, employment status, primary specialization
 * - System generates secure temporary password
 * - Admin receives credentials to communicate to therapist
 * - Therapist completes profile on first login
 *
 * Emits `created` with UserCreationResponse on success,
 * or `cancelled` when the user dismisses without saving.
 */
@Component({
  selector: 'app-create-therapist-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe, TranslateSpecializationPipe, TherapistAccountCreatedModalComponent],
  styleUrl: './create-therapist-dialog.component.scss',
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="create-therapist-title">
      <div class="dialog">
        <h2 id="create-therapist-title">{{ 'admin.therapists.create.title' | transloco }}</h2>
        <p class="subtitle">{{ 'admin.therapists.create.subtitle' | transloco }}</p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

          <div class="field">
            <label for="fullName">{{ 'admin.therapists.create.fullNameLabel' | transloco }} <span aria-hidden="true">*</span></label>
            <input
              id="fullName"
              type="text"
              formControlName="fullName"
              [attr.aria-invalid]="isInvalid('fullName')"
              aria-required="true"
              [placeholder]="'admin.therapists.create.fullNamePlaceholder' | transloco"
            />
            <span *ngIf="isInvalid('fullName')" class="error-msg" role="alert">
              {{ 'admin.therapists.create.fullNameRequired' | transloco }}
            </span>
          </div>

          <div class="field">
            <label for="email">{{ 'admin.therapists.create.emailLabel' | transloco }} <span aria-hidden="true">*</span></label>
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="off"
              [attr.aria-invalid]="isInvalid('email')"
              aria-required="true"
              [placeholder]="'admin.therapists.create.emailPlaceholder' | transloco"
            />
            <span *ngIf="isInvalid('email')" class="error-msg" role="alert">
              {{ 'admin.therapists.create.emailInvalid' | transloco }}
            </span>
          </div>

          <div class="field">
            <label for="phone">{{ 'admin.therapists.create.phoneLabel' | transloco }}</label>
            <input
              id="phone"
              type="tel"
              formControlName="phone"
              [attr.aria-invalid]="isInvalid('phone')"
              [placeholder]="'admin.therapists.create.phonePlaceholder' | transloco"
            />
          </div>

          <div class="field">
            <label for="employmentStatus">{{ 'admin.therapists.create.employmentStatusLabel' | transloco }} <span aria-hidden="true">*</span></label>
            <select
              id="employmentStatus"
              formControlName="employmentStatus"
              [attr.aria-invalid]="isInvalid('employmentStatus')"
              aria-required="true">
              <option value="">{{ 'admin.therapists.create.selectPlaceholder' | transloco }}</option>
              <option *ngFor="let status of employmentStatuses" [value]="status">
                {{ 'admin.therapists.create.employmentStatuses.' + status | transloco }}
              </option>
            </select>
            <span *ngIf="isInvalid('employmentStatus')" class="error-msg" role="alert">
              {{ 'admin.therapists.create.employmentStatusRequired' | transloco }}
            </span>
          </div>

          <div class="field">
            <label for="primarySpecialization">{{ 'admin.therapists.create.primarySpecializationLabel' | transloco }} <span aria-hidden="true">*</span></label>
            <select
              id="primarySpecialization"
              formControlName="primarySpecializationId"
              [attr.aria-invalid]="isInvalid('primarySpecializationId')"
              aria-required="true">
              <option value="">{{ 'admin.therapists.create.selectPlaceholder' | transloco }}</option>
              <option *ngFor="let spec of availableSpecializations" [value]="spec.id">
                {{ spec.name | translateSpecialization }}
              </option>
            </select>
            <span *ngIf="isInvalid('primarySpecializationId')" class="error-msg" role="alert">
              {{ 'admin.therapists.create.primarySpecializationRequired' | transloco }}
            </span>
            <small class="help-text">{{ 'admin.therapists.create.specializationHint' | transloco }}</small>
          </div>

          <div class="info-box">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M10 0C4.48 0 0 4.48 0 10s4.48 10 10 10 10-4.48 10-10S15.52 0 10 0zm1 15H9v-2h2v2zm0-4H9V5h2v6z" fill="currentColor"/>
            </svg>
            <div>
              <strong>{{ 'admin.therapists.create.afterCreationTitle' | transloco }}</strong>
              <p>{{ 'admin.therapists.create.afterCreationText' | transloco }}</p>
            </div>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">
            {{ serverError }}
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="cancel()" [disabled]="saving">{{ 'common.actions.cancel' | transloco }}</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? ('admin.therapists.create.creatingLabel' | transloco) : ('admin.therapists.create.submitButton' | transloco) }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Credentials Modal shown after successful creation -->
    <app-therapist-account-created-modal
      *ngIf="showCredentialsModal"
      [visible]="showCredentialsModal"
      [userData]="createdUser!"
      (close)="onCredentialsModalClose()"
      (viewProfile)="onViewProfile()"
    ></app-therapist-account-created-modal>
  `,
})

export class CreateTherapistDialogComponent implements OnInit {
  readonly employmentStatuses = EMPLOYMENT_STATUS_OPTIONS;
  readonly statusLabels = EMPLOYMENT_STATUS_LABELS;

  /** Optional prefilled data from user creation wizard redirect */
  @Input() prefilledData?: { fullName: string; email: string };

  @Output() created = new EventEmitter<UserCreationResponse>();
  @Output() cancelled = new EventEmitter<void>();

  form: FormGroup;
  saving = false;
  serverError: string | null = null;

  availableSpecializations: IdNamePair[] = [];
  
  showCredentialsModal = false;
  createdUser: UserCreationResponse | null = null;

  constructor(
    private fb: FormBuilder,
    private therapistService: TherapistManagementService,
    private http: HttpClient,
    private transloco: TranslocoService
  ) {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(255)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      phone: ['', Validators.maxLength(50)],
      employmentStatus: ['', Validators.required],
      primarySpecializationId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // Apply prefilled data if provided
    if (this.prefilledData) {
      this.form.patchValue({
        fullName: this.prefilledData.fullName,
        email: this.prefilledData.email
      });
    }
    this.loadSpecializations();
  }

  loadSpecializations(): void {
    this.therapistService.getSpecializations().subscribe({
      next: (data) => {
        this.availableSpecializations = data;
      },
      error: () => {
        this.serverError = this.transloco.translate('admin.therapists.create.errors.loadSpecializationsFailed');
      }
    });
  }

  /** Returns true when the field is invalid and touched. */
  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  /** Submits the form to create therapist with temporary password and profile. */
  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.saving = true;
    this.serverError = null;

    const payload = {
      email: this.form.value.email,
      fullName: this.form.value.fullName,
      phone: this.form.value.phone || null,
      employmentStatus: this.form.value.employmentStatus,
      primarySpecializationId: this.form.value.primarySpecializationId
    };

    console.log('Submitting therapist creation payload:', payload);

    // Call the new endpoint that creates user AND profile atomically
    this.http.post<TherapistWithAccountResponseDto>(
      '/api/v1/therapists/with-account', 
      payload
    ).subscribe({
      next: (response) => {
        console.log('Therapist creation successful:', response);
        this.saving = false;
        this.createdUser = response.userDetails;
        this.showCredentialsModal = true;
        console.log('showCredentialsModal set to:', this.showCredentialsModal);
        console.log('createdUser:', this.createdUser);
        // Don't emit created yet - wait for credentials modal close
      },
      error: (err: HttpErrorResponse) => {
        console.error('Therapist creation failed:', err);
        this.saving = false;
        this.serverError = this.mapError(err);
      }
    });
  }

  /** Called when credentials modal is closed. */
  onCredentialsModalClose(): void {
    this.showCredentialsModal = false;
    // Emit created event to parent
    if (this.createdUser) {
      this.created.emit(this.createdUser);
    }
  }

  /** Called when "View Profile" is clicked in credentials modal. */
  onViewProfile(): void {
    this.showCredentialsModal = false;
    if (this.createdUser) {
      this.created.emit(this.createdUser);
    }
    // Parent component should handle navigation to profile
  }

  /** Dismisses the dialog without saving. */
  cancel(): void {
    this.cancelled.emit();
  }

  private mapError(err: HttpErrorResponse): string {
    const code = err.error?.code as string | undefined;
    const t = (key: string) => this.transloco.translate(`admin.therapists.create.errors.${key}`);

    if (err.status === 401) {
      return t('unauthorized');
    }
    if (err.status === 403) {
      return t('forbidden');
    }
    if (code === 'DUPLICATE_EMAIL') {
      return t('duplicateEmail');
    }
    if (err.status === 400) {
      if (err.error?.message) {
        return err.error.message;
      }
      return t('invalidRequest');
    }
    if (err.status === 500) {
      return t('serverError');
    }

    console.error('Therapist creation error:', err);
    return t('genericFailure');
  }
}
