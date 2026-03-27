import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { UserManagementService } from '../../services/user-management.service';
import { UserSummary } from '../../models/user.model';

/**
 * Confirmation dialog for admin-initiated password reset.
 *
 * Emits `confirmed` after the reset link has been successfully issued,
 * or `cancelled` when dismissed.
 */
@Component({
  selector: 'app-password-reset-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true"
         aria-labelledby="reset-title">
      <div class="dialog">
        <h2 id="reset-title">Reset Password</h2>

        <p>
          Send a 24-hour password reset link to
          <strong>{{ user.email }}</strong>?
        </p>

        <div *ngIf="serverError" class="alert-error" role="alert">
          {{ serverError }}
        </div>
        <div *ngIf="success" class="alert-success" role="status">
          Reset link sent successfully.
        </div>

        <div class="actions">
          <button type="button" (click)="cancel()" [disabled]="sending">
            {{ success ? 'Close' : 'Cancel' }}
          </button>
          <button *ngIf="!success" type="button" (click)="confirm()" [disabled]="sending">
            {{ sending ? 'Sending…' : 'Send reset link' }}
          </button>
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
      padding: 2rem; width: 400px; max-width: 95vw;
      box-shadow: 0 4px 24px rgba(0,0,0,.15);
    }
    h2 { margin: 0 0 1rem; font-size: 1.25rem; }
    p { margin: 0 0 1.5rem; line-height: 1.5; }
    .alert-error {
      padding: .75rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; margin-bottom: 1rem; font-size: .875rem;
    }
    .alert-success {
      padding: .75rem 1rem; background: #F0FDF4;
      border: 1px solid #86EFAC; border-radius: 8px;
      color: #166534; margin-bottom: 1rem; font-size: .875rem;
    }
    .actions {
      display: flex; justify-content: flex-end; gap: .75rem;
    }
    button {
      padding: .6rem 1.25rem; border-radius: 8px;
      border: none; cursor: pointer; font-size: .9375rem;
      font-family: inherit; font-weight: 500;
      transition: background 0.15s ease, box-shadow 0.15s ease;
    }
    button:first-child { background: #F1F5F9; color: #374151; border: 1.5px solid #E2E8F0; }
    button:first-child:hover:not(:disabled) { background: #E2E8F0; }
    button:last-child { background: #DC2626; color: #fff; }
    button:last-child:hover:not(:disabled) { background: #B91C1C; }
    button:disabled { opacity: .55; cursor: not-allowed; }
  `]
})
export class PasswordResetDialogComponent {
  /** The user for whom the reset will be initiated. */
  @Input() user!: UserSummary;

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  sending = false;
  success = false;
  serverError: string | null = null;

  constructor(private userService: UserManagementService) {}

  /** Calls the password reset endpoint. */
  confirm(): void {
    this.sending = true;
    this.serverError = null;

    this.userService.initiatePasswordReset(this.user.id).subscribe({
      next: () => {
        this.sending = false;
        this.success = true;
        this.confirmed.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.sending = false;
        this.serverError = err.error?.code === 'NOT_FOUND'
            ? 'User not found.'
            : 'Failed to send reset link. Please try again.';
      }
    });
  }

  /** Closes the dialog. */
  cancel(): void {
    this.cancelled.emit();
  }
}
