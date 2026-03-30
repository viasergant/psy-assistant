import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserManagementService } from '../../services/user-management.service';
import { ASSIGNABLE_ROLES, ROLE_LABELS, UserRole, UserCreationResponse } from '../../models/user.model';

/**
 * Modal dialog for creating a new user account.
 *
 * Shows the generated temporary password after successful creation.
 * Emits `created` with the user summary when the admin closes the success screen,
 * or `cancelled` when dismissed without saving.
 */
@Component({
  selector: 'app-create-user-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" [attr.aria-labelledby]="createdUser ? 'success-title' : 'create-user-title'">
      <div class="dialog">
        <!-- Success screen after creation -->
        <div *ngIf="createdUser" class="success-view">
          <h2 id="success-title">User Created Successfully</h2>
          
          <div class="success-message">
            <p><strong>{{ createdUser.fullName || createdUser.email }}</strong> has been created.</p>
            <p class="info-text">Please share the temporary password below with the user. They will be required to change it on first login.</p>
          </div>

          <div class="password-display">
            <label>Temporary Password</label>
            <div class="password-box">
              <code>{{ createdUser.temporaryPassword }}</code>
              <button type="button" class="copy-btn" (click)="copyPassword()" [attr.aria-label]="'Copy password'">
                {{ passwordCopied ? 'Copied!' : 'Copy' }}
              </button>
            </div>
          </div>

          <div class="warning-box">
            ⚠️ Make sure to copy and share this password securely. You won't be able to see it again.
          </div>

          <div class="actions">
            <button type="button" class="btn-primary" (click)="closeSuccess()">Done</button>
          </div>
        </div>

        <!-- Creation form -->
        <div *ngIf="!createdUser">
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
    label { font-weight: 500; margin-bottom: .25rem; font-size: .9375rem; }
    input, select {
      appearance: none;
      -webkit-appearance: none;
      padding: .6rem .875rem; border: 1.5px solid #D1D5DB;
      border-radius: 8px; font-size: .9375rem;
      font-family: inherit; color: #0F172A; background: #fff;
      outline: none; transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    input::placeholder { color: #9CA3AF; }
    input:hover, select:hover { border-color: #9CA3AF; }
    input:focus, select:focus { border-color: #0EA5A0; box-shadow: 0 0 0 3px rgba(14,165,160,.15); }
    input[aria-invalid="true"], select[aria-invalid="true"] { border-color: #DC2626; }
    input[aria-invalid="true"]:focus, select[aria-invalid="true"]:focus { box-shadow: 0 0 0 3px rgba(220,38,38,.12); }
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
    button[type="submit"], .btn-primary {
      background: #0EA5A0; color: #fff;
    }
    button[type="submit"]:hover:not(:disabled), .btn-primary:hover:not(:disabled) {
      background: #0C9490; box-shadow: 0 4px 12px rgba(14,165,160,.28);
    }
    button[type="button"]:not(.copy-btn):not(.btn-primary) { 
      background: #F1F5F9; color: #374151; border: 1.5px solid #E2E8F0; 
    }
    button[type="button"]:not(.copy-btn):not(.btn-primary):hover:not(:disabled) { 
      background: #E2E8F0; 
    }
    button:disabled { opacity: .55; cursor: not-allowed; }
    
    /* Success screen styles */
    .success-view {
      text-align: center;
    }
    .success-message {
      margin-bottom: 1.5rem;
    }
    .success-message p {
      margin: .5rem 0;
      color: #374151;
    }
    .success-message strong {
      color: #0F172A;
    }
    .info-text {
      font-size: .875rem;
      color: #64748B;
    }
    .password-display {
      margin-bottom: 1.5rem;
      text-align: left;
    }
    .password-display label {
      display: block;
      font-weight: 600;
      margin-bottom: .5rem;
      color: #0F172A;
    }
    .password-box {
      display: flex;
      gap: .5rem;
      align-items: center;
      padding: .75rem 1rem;
      background: #F9FAFB;
      border: 1.5px solid #E2E8F0;
      border-radius: 8px;
    }
    .password-box code {
      flex: 1;
      font-family: 'Monaco', 'Courier New', monospace;
      font-size: 1rem;
      font-weight: 600;
      color: #0EA5A0;
      letter-spacing: .05em;
    }
    .copy-btn {
      padding: .4rem .875rem;
      background: #fff;
      border: 1.5px solid #D1D5DB;
      border-radius: 6px;
      font-size: .8125rem;
      color: #374151;
      cursor: pointer;
      transition: background 0.15s ease, border-color 0.15s ease;
    }
    .copy-btn:hover {
      background: #F9FAFB;
      border-color: #9CA3AF;
    }
    .copy-btn:active {
      background: #F3F4F6;
    }
    .warning-box {
      padding: .875rem 1rem;
      background: #FEF3C7;
      border: 1px solid #FCD34D;
      border-radius: 8px;
      color: #92400E;
      font-size: .875rem;
      text-align: left;
      line-height: 1.5;
    }
  `]
})
export class CreateUserDialogComponent {
  readonly assignableRoles = ASSIGNABLE_ROLES;
  readonly roleLabels = ROLE_LABELS;

  @Output() created = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() redirectToTherapistWizard = new EventEmitter<{ fullName: string; email: string }>();

  form: FormGroup;
  saving = false;
  serverError: string | null = null;
  createdUser: UserCreationResponse | null = null;
  passwordCopied = false;

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
      next: (response) => {
        this.saving = false;
        this.createdUser = response;
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.serverError = this.mapError(err);
      }
    });
  }

  /** Copies the temporary password to clipboard. */
  copyPassword(): void {
    if (this.createdUser?.temporaryPassword) {
      navigator.clipboard.writeText(this.createdUser.temporaryPassword).then(() => {
        this.passwordCopied = true;
        setTimeout(() => this.passwordCopied = false, 2000);
      });
    }
  }

  /** Closes the success screen and notifies parent. */
  closeSuccess(): void {
    this.created.emit();
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
