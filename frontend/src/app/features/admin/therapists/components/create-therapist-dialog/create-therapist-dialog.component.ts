import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TherapistManagementService } from '../../services/therapist-management.service';
import {
  EMPLOYMENT_STATUS_OPTIONS,
  EMPLOYMENT_STATUS_LABELS,
  IdNamePair
} from '../../models/therapist.model';
import { UserCreationResponse } from '../../../users/models/user.model';
import { TherapistAccountCreatedModalComponent } from '../../../components/therapist-account-created-modal/therapist-account-created-modal.component';

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
  imports: [CommonModule, ReactiveFormsModule, TherapistAccountCreatedModalComponent],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="create-therapist-title">
      <div class="dialog">
        <h2 id="create-therapist-title">Create Therapist Account</h2>
        <p class="subtitle">Create a therapist account. The system will generate a secure temporary password.</p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

          <div class="field">
            <label for="fullName">Full name <span aria-hidden="true">*</span></label>
            <input
              id="fullName"
              type="text"
              formControlName="fullName"
              [attr.aria-invalid]="isInvalid('fullName')"
              aria-required="true"
              placeholder="Enter therapist's full name"
            />
            <span *ngIf="isInvalid('fullName')" class="error-msg" role="alert">
              Full name is required.
            </span>
          </div>

          <div class="field">
            <label for="email">Email <span aria-hidden="true">*</span></label>
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="off"
              [attr.aria-invalid]="isInvalid('email')"
              aria-required="true"
              placeholder="therapist@example.com"
            />
            <span *ngIf="isInvalid('email')" class="error-msg" role="alert">
              Enter a valid email address.
            </span>
          </div>

          <div class="field">
            <label for="phone">Phone number</label>
            <input
              id="phone"
              type="tel"
              formControlName="phone"
              [attr.aria-invalid]="isInvalid('phone')"
              placeholder="+1 (555) 123-4567"
            />
          </div>

          <div class="field">
            <label for="employmentStatus">Employment Status <span aria-hidden="true">*</span></label>
            <select
              id="employmentStatus"
              formControlName="employmentStatus"
              [attr.aria-invalid]="isInvalid('employmentStatus')"
              aria-required="true">
              <option value="">— select —</option>
              <option *ngFor="let status of employmentStatuses" [value]="status">
                {{ statusLabels[status] }}
              </option>
            </select>
            <span *ngIf="isInvalid('employmentStatus')" class="error-msg" role="alert">
              Select an employment status.
            </span>
          </div>

          <div class="field">
            <label for="primarySpecialization">Primary Specialization <span aria-hidden="true">*</span></label>
            <select
              id="primarySpecialization"
              formControlName="primarySpecializationId"
              [attr.aria-invalid]="isInvalid('primarySpecializationId')"
              aria-required="true">
              <option value="">— select —</option>
              <option *ngFor="let spec of availableSpecializations" [value]="spec.id">
                {{ spec.name }}
              </option>
            </select>
            <span *ngIf="isInvalid('primarySpecializationId')" class="error-msg" role="alert">
              Select a primary specialization.
            </span>
            <small class="help-text">Therapist can add more specializations later in their profile</small>
          </div>

          <div class="info-box">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M10 0C4.48 0 0 4.48 0 10s4.48 10 10 10 10-4.48 10-10S15.52 0 10 0zm1 15H9v-2h2v2zm0-4H9V5h2v6z" fill="currentColor"/>
            </svg>
            <div>
              <strong>After creation:</strong>
              <p>A secure temporary password will be generated. The therapist must change it on first login and complete their profile.</p>
            </div>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">
            {{ serverError }}
          </div>

          <div class="actions">
            <button type="button" (click)="cancel()" [disabled]="saving">Cancel</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? 'Creating…' : 'Create account' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Credentials Modal shown after successful creation -->
    <app-therapist-account-created-modal
      *ngIf="showCredentialsModal"
      [userData]="createdUser!"
      (close)="onCredentialsModalClose()"
      (viewProfile)="onViewProfile()"
    ></app-therapist-account-created-modal>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed; inset: 0;
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(3px);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.2s ease;
    }
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    .dialog {
      background: #fff; border-radius: 16px;
      padding: 2rem; width: 540px; max-width: 95vw;
      max-height: 90vh; overflow-y: auto;
      box-shadow: 0 20px 40px rgba(0,0,0,.25);
      animation: slideUp 0.25s ease;
    }
    @keyframes slideUp {
      from { transform: translateY(20px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
    h2 {
      margin: 0 0 1.75rem;
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--color-text-primary, #0F172A);
    }
    .field {
      display: flex; flex-direction: column;
      margin-bottom: 1.25rem;
    }
    label {
      font-weight: 500;
      margin-bottom: .5rem;
      color: var(--color-text-primary, #0F172A);
      font-size: .9375rem;
    }
    input, select, textarea {
      appearance: none;
      -webkit-appearance: none;
      padding: .7rem .875rem;
      border: 1.5px solid var(--color-border, #D1D5DB);
      border-radius: 10px;
      font-size: .9375rem;
      font-family: inherit;
      color: var(--color-text-primary, #0F172A);
      background: #fff;
      outline: none;
      transition: all 0.15s ease;
    }
    textarea {
      resize: vertical;
      min-height: 100px;
    }
    textarea::placeholder {
      color: var(--color-text-muted, #9CA3AF);
    }
    input::placeholder { color: var(--color-text-muted, #9CA3AF); }
    input:hover, select:hover, textarea:hover {
      border-color: #9CA3AF;
    }
    input:focus, select:focus, textarea:focus {
      border-color: var(--color-accent, #0EA5A0);
      box-shadow: 0 0 0 4px rgba(14,165,160,.12);
    }
    input[aria-invalid="true"], select[aria-invalid="true"],
    .checkbox-group[aria-invalid="true"] {
      border-color: var(--color-error, #DC2626);
    }
    input[aria-invalid="true"]:focus, select[aria-invalid="true"]:focus {
      box-shadow: 0 0 0 4px rgba(220,38,38,.1);
    }
    .help-text {
      color: var(--color-text-secondary, #64748B);
      font-size: .8125rem;
      margin-top: .375rem;
    }
    .info-box {
      display: flex;
      gap: .875rem;
      padding: 1rem;
      background: #EFF6FF;
      border: 1.5px solid #BFDBFE;
      border-radius: 10px;
      margin-bottom: 1.25rem;
      color: #1E40AF;
    }
    .info-box svg {
      flex-shrink: 0;
      width: 20px;
      height: 20px;
      color: #3B82F6;
    }
    .info-box strong {
      display: block;
      margin-bottom: .25rem;
      font-size: .9375rem;
    }
    .info-box p {
      margin: 0;
      font-size: .8125rem;
      line-height: 1.5;
    }
    .error-msg {
      color: var(--color-error, #DC2626);
      font-size: .8125rem;
      margin-top: .375rem;
    }
    .alert-error {
      padding: .875rem 1.125rem;
      background: var(--color-error-bg, #FEF2F2);
      border: 1px solid var(--color-error-border, #FECACA);
      border-radius: 10px;
      color: var(--color-error, #DC2626);
      margin-bottom: 1.25rem;
      font-size: .875rem;
    }
    .subtitle {
      color: var(--color-text-secondary, #64748B);
      font-size: .875rem;
      margin: -.875rem 0 1.5rem;
    }
    .actions {
      display: flex; justify-content: flex-end;
      gap: .875rem; margin-top: 2rem;
    }
    button {
      padding: .7rem 1.5rem;
      border-radius: 10px;
      border: none;
      cursor: pointer;
      font-size: .9375rem;
      font-family: inherit;
      font-weight: 500;
      transition: all 0.2s ease;
    }
    button[type="submit"] {
      background: var(--color-accent, #0EA5A0);
      color: #fff;
    }
    button[type="submit"]:hover:not(:disabled) {
      background: var(--color-accent-hover, #0C9490);
      box-shadow: 0 6px 18px rgba(14,165,160,.35);
      transform: translateY(-1px);
    }
    button[type="button"] {
      background: #F1F5F9;
      color: var(--color-text-secondary, #374151);
      border: 1.5px solid var(--color-border, #E2E8F0);
    }
    button[type="button"]:hover:not(:disabled) {
      background: #E2E8F0;
      border-color: #CBD5E1;
    }
    button:disabled {
      opacity: .5;
      cursor: not-allowed;
      transform: none !important;
    }
  `]
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
    private http: HttpClient
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
        this.serverError = 'Failed to load specializations. Please refresh the page.';
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

    // Call the new endpoint that creates user AND profile atomically
    this.http.post<{ userDetails: UserCreationResponse; therapistProfile: any }>(
      '/api/v1/therapists/with-account', 
      payload
    ).subscribe({
      next: (response) => {
        this.saving = false;
        this.createdUser = response.userDetails;
        this.showCredentialsModal = true;
        // Don't emit created yet - wait for credentials modal close
      },
      error: (err: HttpErrorResponse) => {
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
    if (code === 'DUPLICATE_EMAIL') {
      return 'This email address is already registered.';
    }
    if (err.status === 400 && err.error?.message) {
      return err.error.message;
    }
    return 'Failed to create therapist account. Please try again.';
  }
}
