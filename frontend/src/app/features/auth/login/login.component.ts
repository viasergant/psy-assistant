import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
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
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe],
  template: `
    <div class="login-wrapper">
      <aside class="login-brand">
        <div class="brand-content">
          <div class="brand-mark">PSY</div>
          <h2>PSY Assistant</h2>
          <p>Practice management for<br>psychology professionals</p>
        </div>
      </aside>

      <section class="login-panel">
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <h1>Welcome back</h1>
          <p class="subtitle">Sign in to your workspace</p>

          <div class="field">
            <label for="email">Email address</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              autocomplete="username"
              placeholder="you@example.com"
              [attr.aria-invalid]="isInvalid('email')"
            />
            <span *ngIf="isInvalid('email')" class="error-msg">
              Enter a valid email address.
            </span>
          </div>

          <div class="field">
            <label for="password">{{ 'auth.login.passwordLabel' | transloco }}</label>
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

          <div *ngIf="errorMessage" class="alert-error" role="alert">
            {{ errorMessage }}
          </div>

          <button type="submit" [disabled]="loading">
            {{ loading ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>
      </section>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100%;
    }
    .login-wrapper {
      display: flex;
      min-height: 100vh;
    }
    /* ── Brand panel (left, dark) ── */
    .login-brand {
      flex: 0 0 400px;
      background: #141E2B;
      background-image:
        radial-gradient(ellipse at 22% 78%, rgba(14,165,160,.20) 0%, transparent 52%),
        radial-gradient(ellipse at 78% 18%, rgba(14,165,160,.09) 0%, transparent 48%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 3rem 2.5rem;
    }
    .brand-content {
      text-align: center;
      max-width: 260px;
    }
    .brand-mark {
      font-size: 2.75rem;
      font-weight: 800;
      letter-spacing: -0.04em;
      color: #0EA5A0;
      line-height: 1;
      margin-bottom: 1.5rem;
    }
    .brand-content h2 {
      margin: 0 0 0.875rem;
      font-size: 1.5rem;
      font-weight: 700;
      letter-spacing: -0.025em;
      color: #F1F5F9;
    }
    .brand-content p {
      margin: 0;
      font-size: 0.9375rem;
      color: #64748B;
      line-height: 1.65;
    }
    /* ── Form panel (right) ── */
    .login-panel {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2.5rem 2rem;
      background: #F5F4F1;
    }
    form {
      width: 100%;
      max-width: 360px;
    }
    form h1 {
      margin: 0 0 0.375rem;
      font-size: 1.75rem;
      font-weight: 700;
      letter-spacing: -0.025em;
      color: #0F172A;
    }
    .subtitle {
      margin: 0 0 2rem;
      font-size: 0.9375rem;
      color: #64748B;
      line-height: 1.5;
    }
    .field {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
      margin-bottom: 1.125rem;
    }
    .field label {
      font-size: 0.875rem;
      font-weight: 500;
      color: #374151;
    }
    .field input {
      appearance: none;
      -webkit-appearance: none;
      background: #FFFFFF;
      border: 1.5px solid #D1D5DB;
      border-radius: 8px;
      padding: 0.65rem 0.875rem;
      font-size: 0.9375rem;
      font-family: inherit;
      color: #0F172A;
      outline: none;
      transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    .field input::placeholder {
      color: #9CA3AF;
    }
    .field input:hover {
      border-color: #9CA3AF;
    }
    .field input:focus {
      border-color: #0EA5A0;
      box-shadow: 0 0 0 3px rgba(14,165,160,.15);
    }
    .field input[aria-invalid="true"] {
      border-color: #DC2626;
    }
    .field input[aria-invalid="true"]:focus {
      box-shadow: 0 0 0 3px rgba(220,38,38,.12);
    }
    .error-msg {
      font-size: 0.8125rem;
      color: #DC2626;
      line-height: 1.4;
    }
    .alert-error {
      padding: 0.75rem 1rem;
      background: #FEF2F2;
      border: 1px solid #FECACA;
      border-radius: 8px;
      color: #DC2626;
      font-size: 0.875rem;
      line-height: 1.5;
      margin-bottom: 1.25rem;
    }
    button[type="submit"] {
      width: 100%;
      padding: 0.75rem 1.25rem;
      background: #0EA5A0;
      color: #FFFFFF;
      border: none;
      border-radius: 8px;
      font-size: 0.9375rem;
      font-weight: 600;
      font-family: inherit;
      cursor: pointer;
      letter-spacing: -0.01em;
      margin-top: 0.25rem;
      transition: background 0.15s ease, box-shadow 0.15s ease, transform 0.1s ease;
    }
    button[type="submit"]:hover:not(:disabled) {
      background: #0C9490;
      box-shadow: 0 4px 14px rgba(14,165,160,.32);
    }
    button[type="submit"]:active:not(:disabled) {
      transform: translateY(1px);
      background: #0A8480;
      box-shadow: none;
    }
    button[type="submit"]:focus-visible {
      outline: none;
      box-shadow: 0 0 0 3px rgba(14,165,160,.30);
    }
    button[type="submit"]:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    @media (max-width: 680px) {
      .login-brand { display: none; }
      .login-panel { background: #FFFFFF; }
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
    private router: Router,
    private route: ActivatedRoute
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
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
        // SECURITY: only navigate to same-origin relative paths
        const safeUrl = returnUrl.startsWith('/') ? returnUrl : '/';
        this.router.navigateByUrl(safeUrl);
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
