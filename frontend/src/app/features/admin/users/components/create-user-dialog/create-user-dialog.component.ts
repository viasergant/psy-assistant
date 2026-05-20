import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { UserManagementService } from '../../services/user-management.service';
import { ASSIGNABLE_ROLES, ROLE_LABELS, UserRole, UserCreationResponse } from '../../models/user.model';

/** Validates that at least one role checkbox is selected. */
const rolesRequiredValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value as UserRole[];
  return Array.isArray(value) && value.length >= 1 ? null : { rolesRequired: true };
};

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
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" [attr.aria-labelledby]="createdUser ? 'success-title' : 'create-user-title'">
      <div class="dialog">
        <!-- Success screen after creation -->
        <div *ngIf="createdUser" class="success-view">
          <h2 id="success-title">{{ 'admin.users.create.successTitle' | transloco }}</h2>
          
          <div class="success-message">
            <p>{{ 'admin.users.create.successMessage' | transloco: { name: (createdUser.fullName || createdUser.email) } }}</p>
            <p class="info-text">{{ 'admin.users.create.successInfoText' | transloco }}</p>
          </div>

          <div class="password-display">
            <label>{{ 'admin.users.create.tempPasswordLabel' | transloco }}</label>
            <div class="password-box">
              <code>{{ createdUser.temporaryPassword }}</code>
              <button type="button" class="copy-btn" (click)="copyPassword()" [attr.aria-label]="'admin.users.create.copyButton' | transloco">
                {{ passwordCopied ? ('admin.users.create.copiedButton' | transloco) : ('admin.users.create.copyButton' | transloco) }}
              </button>
            </div>
          </div>

          <div class="warning-box">
            ⚠️ {{ 'admin.users.create.warningText' | transloco }}
          </div>

          <div class="actions">
            <button type="button" class="btn-primary" (click)="closeSuccess()">{{ 'admin.users.create.done' | transloco }}</button>
          </div>
        </div>

        <!-- Creation form -->
        <div *ngIf="!createdUser">
          <h2 id="create-user-title">{{ 'admin.users.create.title' | transloco }}</h2>

          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

            <div class="field">
              <label for="email">{{ 'admin.users.create.emailLabel' | transloco }} <span aria-hidden="true">*</span></label>
              <input
                id="email"
                type="email"
                formControlName="email"
                autocomplete="off"
                [attr.aria-invalid]="isInvalid('email')"
                aria-required="true"
              />
              <span *ngIf="isInvalid('email')" class="error-msg" role="alert">
                {{ 'admin.users.create.emailInvalid' | transloco }}
              </span>
            </div>

            <div class="field">
              <label for="fullName">{{ 'admin.users.create.fullNameLabel' | transloco }} <span aria-hidden="true">*</span></label>
              <input
                id="fullName"
                type="text"
                formControlName="fullName"
                [attr.aria-invalid]="isInvalid('fullName')"
                aria-required="true"
              />
              <span *ngIf="isInvalid('fullName')" class="error-msg" role="alert">
                {{ 'admin.users.create.fullNameRequired' | transloco }}
              </span>
            </div>

            <div class="field">
              <fieldset class="roles-fieldset" [class.roles-invalid]="isInvalid('roles')">
                <legend>{{ 'admin.users.roles.label' | transloco }} <span aria-hidden="true">*</span></legend>
                <div class="roles-checkboxes">
                  <label *ngFor="let r of assignableRoles" class="role-checkbox-label">
                    <input
                      type="checkbox"
                      [value]="r"
                      [checked]="isRoleSelected(r)"
                      (change)="toggleRole(r, $event)"
                    />
                    {{ 'roles.' + r | transloco }}
                  </label>
                </div>
                <span *ngIf="isInvalid('roles')" class="error-msg" role="alert">
                  {{ 'admin.users.roles.required' | transloco }}
                </span>
              </fieldset>
            </div>

            <div *ngIf="serverError" class="alert-error" role="alert">
              {{ serverError }}
            </div>

            <div class="actions">
              <button type="button" (click)="cancel()" [disabled]="saving">{{ 'common.actions.cancel' | transloco }}</button>
              <button type="submit" [disabled]="saving">
                {{ saving ? ('admin.users.create.creatingLabel' | transloco) : ('admin.users.create.createButton' | transloco) }}
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
    .roles-fieldset {
      border: 1.5px solid #D1D5DB;
      border-radius: 8px;
      padding: .75rem 1rem;
      margin: 0;
    }
    .roles-fieldset.roles-invalid {
      border-color: #DC2626;
    }
    .roles-fieldset legend {
      font-weight: 500;
      font-size: .9375rem;
      padding: 0 .25rem;
    }
    .roles-checkboxes {
      display: flex;
      flex-direction: column;
      gap: .5rem;
      margin-top: .5rem;
    }
    .role-checkbox-label {
      display: flex;
      align-items: center;
      gap: .5rem;
      font-weight: 400;
      font-size: .9375rem;
      cursor: pointer;
    }
    .role-checkbox-label input[type="checkbox"] {
      width: 1rem;
      height: 1rem;
      cursor: pointer;
      accent-color: #0EA5A0;
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
      roles: [[] as UserRole[], rolesRequiredValidator]
    });
  }

  /** Returns true when the field is invalid and touched. */
  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  /** Returns true when the given role is in the current selection. */
  isRoleSelected(role: UserRole): boolean {
    const current = this.form.get('roles')?.value as UserRole[];
    return Array.isArray(current) && current.includes(role);
  }

  /** Adds or removes a role from the `roles` form control value on checkbox change. */
  toggleRole(role: UserRole, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const ctrl = this.form.get('roles')!;
    const current: UserRole[] = Array.isArray(ctrl.value) ? [...ctrl.value] : [];
    if (checked) {
      if (!current.includes(role)) {
        ctrl.setValue([...current, role]);
      }
    } else {
      ctrl.setValue(current.filter(r => r !== role));
    }
    ctrl.markAsTouched();
  }

  /** Submits the form to the API. */
  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const formValue = this.form.value as { email: string; fullName: string; roles: UserRole[] };

    // If THERAPIST is among the selected roles, redirect to therapist creation wizard
    if (formValue.roles.includes('THERAPIST')) {
      this.redirectToTherapistWizard.emit({
        fullName: formValue.fullName,
        email: formValue.email
      });
      return;
    }

    // For non-therapist roles, proceed with standard user creation
    this.saving = true;
    this.serverError = null;

    this.userService.createUser({
      email: formValue.email,
      fullName: formValue.fullName,
      roles: formValue.roles
    }).subscribe({
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
