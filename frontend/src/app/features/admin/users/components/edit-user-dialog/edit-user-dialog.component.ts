import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { UserManagementService } from '../../services/user-management.service';
import { UserRole, UserSummary } from '../../models/user.model';

/**
 * Modal dialog for editing an existing user's full name, role, or active status.
 *
 * Emits `updated` with the server-returned UserSummary on success,
 * or `cancelled` when dismissed without saving.
 */
@Component({
  selector: 'app-edit-user-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="edit-user-title">
      <div class="dialog">
        <h2 id="edit-user-title">Edit User</h2>
        <p class="sub">{{ user.email }}</p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

          <div class="field">
            <label for="fullName">Full name</label>
            <input
              id="fullName"
              type="text"
              formControlName="fullName"
            />
          </div>

          <div class="field">
            <label for="role">Role</label>
            <select id="role" formControlName="role">
              <option value="ADMIN">Administrator</option>
              <option value="USER">User</option>
            </select>
          </div>

          <div class="field field-row">
            <input
              id="active"
              type="checkbox"
              formControlName="active"
            />
            <label for="active">Active account</label>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">
            {{ serverError }}
          </div>

          <div class="actions">
            <button type="button" (click)="cancel()" [disabled]="saving">Cancel</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? 'Saving…' : 'Save changes' }}
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
    h2 { margin: 0 0 .25rem; font-size: 1.25rem; }
    .sub { margin: 0 0 1.5rem; color: #718096; font-size: .9rem; }
    .field {
      display: flex; flex-direction: column; margin-bottom: 1rem;
    }
    .field-row {
      flex-direction: row; align-items: center; gap: .5rem;
    }
    label { font-weight: 500; margin-bottom: .25rem; }
    .field-row label { margin-bottom: 0; }
    input[type="text"] {
      appearance: none;
      padding: .6rem .875rem; border: 1.5px solid #D1D5DB;
      border-radius: 8px; font-size: .9375rem;
      font-family: inherit; color: #0F172A; background: #fff;
      outline: none; transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    input[type="text"]:hover { border-color: #9CA3AF; }
    input[type="text"]:focus { border-color: #0EA5A0; box-shadow: 0 0 0 3px rgba(14,165,160,.15); }
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
    button[type="submit"] { background: #0EA5A0; color: #fff; }
    button[type="submit"]:hover:not(:disabled) {
      background: #0C9490; box-shadow: 0 4px 12px rgba(14,165,160,.28);
    }
    button[type="button"] { background: #F1F5F9; color: #374151; border: 1.5px solid #E2E8F0; }
    button[type="button"]:hover:not(:disabled) { background: #E2E8F0; }
    button:disabled { opacity: .55; cursor: not-allowed; }
  `]
})
export class EditUserDialogComponent implements OnInit {
  /** The user being edited (pre-populates the form). */
  @Input() user!: UserSummary;

  @Output() updated = new EventEmitter<UserSummary>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  serverError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserManagementService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      fullName: [this.user.fullName ?? ''],
      role: [this.user.role as UserRole],
      active: [this.user.active]
    });
  }

  /** Submits only the changed fields to the PATCH endpoint. */
  submit(): void {
    this.saving = true;
    this.serverError = null;

    this.userService.updateUser(this.user.id, this.form.value).subscribe({
      next: (updated) => {
        this.saving = false;
        this.updated.emit(updated);
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
    if (code === 'SELF_DEACTIVATION_FORBIDDEN') {
      return 'You cannot deactivate your own account.';
    }
    if (code === 'NOT_FOUND') {
      return 'User not found.';
    }
    return 'Failed to update user. Please try again.';
  }
}
