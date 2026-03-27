import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserManagementService } from '../../services/user-management.service';
import { UserRole, UserSummary } from '../../models/user.model';

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
              <option value="ADMIN">Administrator</option>
              <option value="USER">User</option>
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
    input, select {
      padding: .5rem .75rem; border: 1px solid #cbd5e0;
      border-radius: 4px; font-size: 1rem;
    }
    input[aria-invalid="true"], select[aria-invalid="true"] {
      border-color: #e53e3e;
    }
    .error-msg { color: #e53e3e; font-size: .85rem; margin-top: .25rem; }
    .alert-error {
      padding: .75rem; background: #fff5f5;
      border: 1px solid #fc8181; border-radius: 4px;
      color: #c53030; margin-bottom: 1rem;
    }
    .actions {
      display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.5rem;
    }
    button {
      padding: .5rem 1.25rem; border-radius: 4px;
      border: none; cursor: pointer; font-size: 1rem;
    }
    button[type="submit"] { background: #4299e1; color: #fff; }
    button[type="button"] { background: #edf2f7; color: #2d3748; }
    button:disabled { opacity: .6; cursor: not-allowed; }
  `]
})
export class CreateUserDialogComponent {
  @Output() created = new EventEmitter<UserSummary>();
  @Output() cancelled = new EventEmitter<void>();

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

    this.saving = true;
    this.serverError = null;

    this.userService.createUser(this.form.value).subscribe({
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
