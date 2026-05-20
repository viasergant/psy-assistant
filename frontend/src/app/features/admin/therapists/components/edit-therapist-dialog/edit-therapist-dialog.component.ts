import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { TherapistManagementService } from '../../services/therapist-management.service';
import {
  TherapistProfile,
  EMPLOYMENT_STATUS_OPTIONS,
  EMPLOYMENT_STATUS_LABELS
} from '../../models/therapist.model';
import { TranslateSpecializationPipe } from '../../pipes/translate-specialization.pipe';

/**
 * Modal dialog for editing an existing therapist profile.
 *
 * Emits `updated` with the server-returned TherapistProfile on success,
 * or `cancelled` when the user dismisses without saving.
 */
@Component({
  selector: 'app-edit-therapist-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe, TranslateSpecializationPipe],
  styleUrls: ['./edit-therapist-dialog.component.scss'],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="edit-therapist-title">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="edit-therapist-title">{{ 'admin.therapists.edit.title' | transloco }}</h2>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

          <div class="field">
            <label for="name">{{ 'admin.therapists.edit.fullNameLabel' | transloco }} <span class="required" aria-hidden="true">*</span></label>
            <input
              id="name"
              type="text"
              formControlName="name"
              [attr.aria-invalid]="isInvalid('name')"
              aria-required="true"
            />
            <span *ngIf="isInvalid('name')" class="error-msg" role="alert">
              {{ 'admin.therapists.edit.fullNameRequired' | transloco }}
            </span>
          </div>

          <div class="field">
            <label for="email">{{ 'admin.therapists.edit.emailLabel' | transloco }} <span class="required" aria-hidden="true">*</span></label>
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="off"
              [attr.aria-invalid]="isInvalid('email')"
              aria-required="true"
            />
            <span *ngIf="isInvalid('email')" class="error-msg" role="alert">
              {{ 'admin.therapists.edit.emailInvalid' | transloco }}
            </span>
          </div>

          <div class="field">
            <label for="phone">{{ 'admin.therapists.edit.phoneLabel' | transloco }}</label>
            <input
              id="phone"
              type="tel"
              formControlName="phone"
              [attr.aria-invalid]="isInvalid('phone')"
            />
          </div>

          <div class="field">
            <label for="employmentStatus">{{ 'admin.therapists.edit.employmentStatusLabel' | transloco }} <span class="required" aria-hidden="true">*</span></label>
            <select
              id="employmentStatus"
              formControlName="employmentStatus"
              [attr.aria-invalid]="isInvalid('employmentStatus')"
              aria-required="true">
              <option value="">{{ 'admin.therapists.edit.selectPlaceholder' | transloco }}</option>
              <option *ngFor="let status of employmentStatuses" [value]="status">
                {{ 'admin.therapists.create.employmentStatuses.' + status | transloco }}
              </option>
            </select>
            <span *ngIf="isInvalid('employmentStatus')" class="error-msg" role="alert">
              {{ 'admin.therapists.edit.employmentStatusRequired' | transloco }}
            </span>
          </div>

          <div class="field">
            <label for="bio">{{ 'admin.therapists.edit.bioLabel' | transloco }}</label>
            <textarea
              id="bio"
              formControlName="bio"
              rows="4"
              [placeholder]="'admin.therapists.edit.bioPlaceholder' | transloco"
            ></textarea>
          </div>

          <div class="info-section">
            <h3>{{ 'admin.therapists.edit.specializationsTitle' | transloco }}</h3>
            <div class="chip-list">
              <span *ngFor="let spec of therapist.specializations" class="chip">
                {{ spec.name | translateSpecialization }}
              </span>
              <span *ngIf="therapist.specializations.length === 0" class="empty-text">
                {{ 'admin.therapists.edit.noneAssigned' | transloco }}
              </span>
            </div>
            <p class="help-text">
              {{ 'admin.therapists.edit.specializationsHelp' | transloco }}
            </p>
          </div>

          <div class="info-section">
            <h3>{{ 'admin.therapists.edit.languagesTitle' | transloco }}</h3>
            <div class="chip-list">
              <span *ngFor="let lang of therapist.languages" class="chip">
                {{ lang.name }}
              </span>
              <span *ngIf="therapist.languages.length === 0" class="empty-text">
                {{ 'admin.therapists.edit.noneAssigned' | transloco }}
              </span>
            </div>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">
            {{ serverError }}
          </div>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="cancel()" [disabled]="saving">
              {{ 'common.actions.cancel' | transloco }}
            </button>
            <button type="submit" class="btn-primary" [disabled]="saving">
              {{ saving ? ('admin.therapists.edit.savingLabel' | transloco) : ('admin.therapists.edit.saveButton' | transloco) }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class EditTherapistDialogComponent implements OnInit {
  readonly employmentStatuses = EMPLOYMENT_STATUS_OPTIONS;
  readonly statusLabels = EMPLOYMENT_STATUS_LABELS;

  @Input() therapist!: TherapistProfile;
  @Output() updated = new EventEmitter<TherapistProfile>();
  @Output() cancelled = new EventEmitter<void>();

  form: FormGroup;
  saving = false;
  serverError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private therapistService: TherapistManagementService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      phone: ['', Validators.maxLength(50)],
      employmentStatus: ['', Validators.required],
      bio: ['', Validators.maxLength(2000)]
    });
  }

  ngOnInit(): void {
    this.form.patchValue({
      name: this.therapist.name,
      email: this.therapist.email,
      phone: this.therapist.phone || '',
      employmentStatus: this.therapist.employmentStatus,
      bio: this.therapist.bio || ''
    });
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
      version: this.therapist.version
    };

    this.therapistService.updateTherapist(this.therapist.id, payload).subscribe({
      next: (therapist) => {
        this.saving = false;
        this.updated.emit(therapist);
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
    if (err.status === 409) {
      return 'The profile was updated by someone else. Please refresh and try again.';
    }
    const code = err.error?.code as string | undefined;
    if (code === 'DUPLICATE_EMAIL') {
      return 'This email address is already registered to another therapist.';
    }
    return 'Failed to update therapist. Please try again.';
  }
}
