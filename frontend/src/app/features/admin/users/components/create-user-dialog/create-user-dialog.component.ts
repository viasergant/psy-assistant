import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserManagementService } from '../../services/user-management.service';
import { ASSIGNABLE_ROLES, ROLE_LABELS, UserRole, UserSummary } from '../../models/user.model';

/**
 * Modal dialog for creating a new user account.
 *
 * Emits `created` with the server-returned UserSummary on success,
 * or `cancelled` when the user dismisses without saving.
 */
@Component({
  selector: 'app-create-user-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="create-user-title">
      <div class="dialog">
        <h2 id="create-user-title">Create User</h2>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

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
            <label for="fullName">Full name <span aria-hidden="true">*</span></label>
            <input
              id="fullName"
              type="text"
              formControlName="fullName"
              [attr.aria-invalid]="isInvalid('fullName')"
              aria-required="true"
            />
            <span *ngIf="isInvalid('fullName')" class="error-msg" role="alert">
              Full name is required.
            </span>
          </div>

          <div class="field">
            <label for="role">Role <span aria-hidden="true">*</span></label>
            <select
              id="role"
              formControlName="role"
              [attr.aria-invalid]="isInvalid('role')"
              aria-required="true">
              <option value="">— select —</option>
              <option *ngFor="let r of assignableRoles" [value]="r">{{ roleLabels[r] }}</option>
            </select>
            <span *ngIf="isInvalid('role')" class="error-msg" role="alert">
              Select a role.
            </span>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">
            {{ serverError }}
          </div>

          <div class="actions">
            <button type="button" (click)="cancel()" [disabled]="saving">Cancel</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? 'Creating…' : 'Create user' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,.45);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000;
    }
    .dialog {
      background: #fff; border-radius: 8px;
      padding: 2rem; width: 420px; max-width: 95vw;
      box-shadow: 0 4px 24px rgba(0,0,0,.15);
    }
    h2 { margin: 0 0 1.5rem; font-size: 1.25rem; }
    .field {
      display: flex; flex-direction: column; margin-bottom: 1rem;
    }
    label { font-weight: 500; margin-bottom: .25rem; }
    input {
      appearance: none;
      -webkit-appearance: none;
      padding: .6rem .875rem; border: 1.5px solid #D1D5DB;
      border-radius: 8px; font-size: .9375rem;
      font-family: inherit; color: #0F172A; background: #fff;
      outline: none; transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    input::placeholder { color: #9CA3AF; }
    input:hover { border-color: #9CA3AF; }
    input:focus { border-color: #0EA5A0; box-shadow: 0 0 0 3px rgba(14,165,160,.15); }
    input[aria-invalid="true"] { border-color: #DC2626; }
    input[aria-invalid="true"]:focus { box-shadow: 0 0 0 3px rgba(220,38,38,.12); }
    .error-msg { color: #DC2626; font-size: .8125rem; margin-top: .25rem; }
    .alert-error {
      padding: .75rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; margin-bottom: 1rem; font-size: .875rem;
    }
    .actions {
      display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.5rem;
    }
    button {
      padding: .6rem 1.25rem; border-radius: 8px;
      border: none; cursor: pointer; font-size: .9375rem;
      font-family: inherit; font-weight: 500;
      transition: background 0.15s ease, box-shadow 0.15s ease;
    }
    button[type="submit"] {
      background: #0EA5A0; color: #fff;
    }
    button[type="submit"]:hover:not(:disabled) {
      background: #0C9490; box-shadow: 0 4px 12px rgba(14,165,160,.28);
    }
    button[type="button"] { background: #F1F5F9; color: #374151; border: 1.5px solid #E2E8F0; }
    button[type="button"]:hover:not(:disabled) { background: #E2E8F0; }
    button:disabled { opacity: .55; cursor: not-allowed; }
  `]
})
export class CreateUserDialogComponent {
  readonly assignableRoles = ASSIGNABLE_ROLES;
  readonly roleLabels = ROLE_LABELS;

  @Output() created = new EventEmitter<UserSummary>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() redirectToTherapistWizard = new EventEmitter<{ fullName: string; email: string }>();

  form: FormGroup;
  saving = false;
  serverError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserManagementService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      fullName: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(255)]],
      role: ['' as UserRole | '', Validators.required]
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

    const formValue = this.form.value;

    // If THERAPIST role selected, redirect to therapist creation wizard
    if (formValue.role === 'THERAPIST') {
      this.redirectToTherapistWizard.emit({
        fullName: formValue.fullName,
        email: formValue.email
      });
      return;
    }

    // For non-therapist roles, proceed with standard user creation
    this.saving = true;
    this.serverError = null;

    this.userService.createUser(formValue).subscribe({
      next: (user) => {
        this.saving = false;
        this.created.emit(user);
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
    return 'Failed to create user. Please try again.';
  }
}
