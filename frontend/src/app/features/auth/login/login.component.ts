import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

/**
 * Login form component.
 *
 * Uses a reactive form with email and password fields.
 * On success, navigates to the default authenticated route (/clients).
 * On failure, displays a human-readable error message.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-wrapper">
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <h1>Sign in</h1>

        <div class="field">
          <label for="email">Email</label>
          <input
            id="email"
            type="email"
            formControlName="email"
            autocomplete="username"
            [attr.aria-invalid]="isInvalid('email')"
          />
          <span *ngIf="isInvalid('email')" class="error-msg">
            Enter a valid email address.
          </span>
        </div>

        <div class="field">
          <label for="password">Password</label>
          <input
            id="password"
            type="password"
            formControlName="password"
            autocomplete="current-password"
            [attr.aria-invalid]="isInvalid('password')"
          />
          <span *ngIf="isInvalid('password')" class="error-msg">
            Password is required (max 72 characters).
          </span>
        </div>

        <div *ngIf="errorMessage" class="alert alert-error" role="alert">
          {{ errorMessage }}
        </div>

        <button type="submit" [disabled]="loading">
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>
    </div>
  `,
  styles: [`
    .login-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
    }
    form {
      width: 360px;
      padding: 2rem;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
    }
    .field {
      display: flex;
      flex-direction: column;
      margin-bottom: 1rem;
    }
    .error-msg {
      color: #e53e3e;
      font-size: 0.85rem;
    }
    .alert-error {
      padding: 0.75rem;
      background: #fff5f5;
      border: 1px solid #fc8181;
      border-radius: 4px;
      color: #c53030;
      margin-bottom: 1rem;
    }
    button {
      width: 100%;
      padding: 0.6rem;
      background: #4299e1;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
    button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  `]
})
export class LoginComponent {
  form: FormGroup;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.maxLength(72)]]
    });
  }

  /** Returns true when the field is invalid and the form has been submitted or touched. */
  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  /** Submits the form and handles success / error responses. */
  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMessage = null;

    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/clients']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = this.mapError(err);
      }
    });
  }

  private mapError(err: HttpErrorResponse): string {
    const code = err.error?.code as string | undefined;
    switch (code) {
      case 'INVALID_CREDENTIALS':
        return 'Incorrect email or password.';
      case 'ACCOUNT_DISABLED':
        return 'Your account has been deactivated. Contact your administrator.';
      default:
        return 'Login failed. Please try again.';
    }
  }
}
