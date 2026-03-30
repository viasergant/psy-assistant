import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TherapistManagementService } from '../../services/therapist-management.service';
import {
  TherapistProfile,
  EMPLOYMENT_STATUS_OPTIONS,
  EMPLOYMENT_STATUS_LABELS,
  IdNamePair
} from '../../models/therapist.model';

/**
 * Modal dialog for creating a new therapist profile.
 *
 * Emits `created` with the server-returned TherapistProfile on success,
 * or `cancelled` when the user dismisses without saving.
 */
@Component({
  selector: 'app-create-therapist-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="create-therapist-title">
      <div class="dialog">
        <h2 id="create-therapist-title">Create Therapist Profile</h2>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

          <div class="field">
            <label for="name">Full name <span aria-hidden="true">*</span></label>
            <input
              id="name"
              type="text"
              formControlName="name"
              [attr.aria-invalid]="isInvalid('name')"
              aria-required="true"
            />
            <span *ngIf="isInvalid('name')" class="error-msg" role="alert">
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
            <label for="specializations">Specializations <span aria-hidden="true">*</span></label>
            <div class="checkbox-group" [attr.aria-invalid]="isInvalid('specializationIds')">
              <label *ngFor="let spec of availableSpecializations" class="checkbox-label">
                <input
                  type="checkbox"
                  [value]="spec.id"
                  (change)="onSpecializationChange($event, spec.id)"
                />
                {{ spec.name }}
              </label>
            </div>
            <span *ngIf="isInvalid('specializationIds')" class="error-msg" role="alert">
              Select at least one specialization.
            </span>
          </div>

          <div class="field">
            <label for="languages">Languages <span aria-hidden="true">*</span></label>
            <div class="checkbox-group" [attr.aria-invalid]="isInvalid('languageIds')">
              <label *ngFor="let lang of availableLanguages" class="checkbox-label">
                <input
                  type="checkbox"
                  [value]="lang.id"
                  (change)="onLanguageChange($event, lang.id)"
                />
                {{ lang.name }}
              </label>
            </div>
            <span *ngIf="isInvalid('languageIds')" class="error-msg" role="alert">
              Select at least one language.
            </span>
          </div>

          <div class="field">
            <label for="bio">Biography / About</label>
            <textarea
              id="bio"
              formControlName="bio"
              rows="4"
              placeholder="Brief professional biography..."
            ></textarea>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">
            {{ serverError }}
          </div>

          <div class="actions">
            <button type="button" (click)="cancel()" [disabled]="saving">Cancel</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? 'Creating…' : 'Create therapist' }}
            </button>
          </div>
        </form>
      </div>
    </div>
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
    .checkbox-group {
      display: flex;
      flex-direction: column;
      gap: .625rem;
      padding: .875rem;
      border: 1.5px solid var(--color-border, #E2E8F0);
      border-radius: 10px;
      background: #FAFBFC;
      max-height: 180px;
      overflow-y: auto;
    }
    .checkbox-label {
      display: flex;
      align-items: center;
      gap: .625rem;
      cursor: pointer;
      padding: .5rem;
      border-radius: 6px;
      transition: background 0.15s;
      font-weight: 400;
      font-size: .9375rem;
    }
    .checkbox-label:hover {
      background: #F1F5F9;
    }
    .checkbox-label input[type="checkbox"] {
      width: 18px;
      height: 18px;
      cursor: pointer;
      margin: 0;
      padding: 0;
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

  @Output() created = new EventEmitter<TherapistProfile>();
  @Output() cancelled = new EventEmitter<void>();

  form: FormGroup;
  saving = false;
  serverError: string | null = null;

  availableSpecializations: IdNamePair[] = [];
  availableLanguages: IdNamePair[] = [];

  selectedSpecializationIds: string[] = [];
  selectedLanguageIds: string[] = [];

  constructor(
    private fb: FormBuilder,
    private therapistService: TherapistManagementService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      phone: ['', Validators.maxLength(50)],
      employmentStatus: ['', Validators.required],
      specializationIds: [[], Validators.required],
      languageIds: [[], Validators.required],
      bio: ['', Validators.maxLength(2000)]
    });
  }

  ngOnInit(): void {
    this.loadDropdownOptions();
  }

  loadDropdownOptions(): void {
    this.therapistService.getSpecializations().subscribe({
      next: (data) => {
        this.availableSpecializations = data;
      },
      error: () => {
        this.serverError = 'Failed to load specializations. Please refresh the page.';
      }
    });

    this.therapistService.getLanguages().subscribe({
      next: (data) => {
        this.availableLanguages = data;
      },
      error: () => {
        this.serverError = 'Failed to load languages. Please refresh the page.';
      }
    });
  }

  onSpecializationChange(event: Event, id: string): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedSpecializationIds.push(id);
    } else {
      this.selectedSpecializationIds = this.selectedSpecializationIds.filter(sid => sid !== id);
    }
    this.form.patchValue({ specializationIds: this.selectedSpecializationIds });
  }

  onLanguageChange(event: Event, id: string): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedLanguageIds.push(id);
    } else {
      this.selectedLanguageIds = this.selectedLanguageIds.filter(lid => lid !== id);
    }
    this.form.patchValue({ languageIds: this.selectedLanguageIds });
  }

  /** Returns true when the field is invalid and touched. */
  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  /** Submits the form to the API. */
  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.saving = true;
    this.serverError = null;

    const payload = {
      ...this.form.value,
      specializationIds: this.selectedSpecializationIds,
      languageIds: this.selectedLanguageIds
    };

    this.therapistService.createTherapist(payload).subscribe({
      next: (therapist) => {
        this.saving = false;
        this.created.emit(therapist);
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.serverError = this.mapError(err);
      }
    });
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
    return 'Failed to create therapist. Please try again.';
  }
}
