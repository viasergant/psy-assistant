import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, ViewChild } from '@angular/core';
import {
  AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppModeService } from '../../../core/mode/app-mode.service';
import { RegistrationService } from './registration.service';
import { TurnstileComponent } from './turnstile.component';

const RESEND_COOLDOWN_SECONDS = 60;

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  return group.get('password')?.value === group.get('confirmPassword')?.value
    ? null
    : { passwordMismatch: true };
}

/**
 * Self-service registration (saas mode). After a successful submit the same page shows
 * a "check your email" state with a rate-limited resend button.
 */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, TurnstileComponent],
  styleUrl: './auth-page.scss',
  template: `
    <div class="auth-card">
      @if (!sentTo()) {
        <h1>{{ 'auth.register.title' | transloco }}</h1>
        <p class="subtitle">{{ 'auth.register.subtitle' | transloco }}</p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="orgName">{{ 'auth.register.orgNameLabel' | transloco }}</label>
            <input id="orgName" formControlName="orgName" autocomplete="organization"
                   [attr.aria-invalid]="isInvalid('orgName')" />
            @if (isInvalid('orgName')) {
              <span class="error-msg">{{ 'auth.register.orgNameRequired' | transloco }}</span>
            }
          </div>

          <div class="field">
            <label for="ownerEmail">{{ 'auth.register.emailLabel' | transloco }}</label>
            <input id="ownerEmail" type="email" formControlName="ownerEmail" autocomplete="email"
                   [attr.aria-invalid]="isInvalid('ownerEmail')" />
            @if (isInvalid('ownerEmail')) {
              <span class="error-msg">{{ 'auth.register.emailInvalid' | transloco }}</span>
            }
          </div>

          <div class="field">
            <label for="password">{{ 'auth.register.passwordLabel' | transloco }}</label>
            <input id="password" type="password" formControlName="password"
                   autocomplete="new-password" [attr.aria-invalid]="isInvalid('password')" />
            @if (isInvalid('password')) {
              <span class="error-msg">{{ 'auth.register.passwordTooShort' | transloco }}</span>
            }
          </div>

          <div class="field">
            <label for="confirmPassword">{{ 'auth.register.confirmPasswordLabel' | transloco }}</label>
            <input id="confirmPassword" type="password" formControlName="confirmPassword"
                   autocomplete="new-password"
                   [attr.aria-invalid]="form.touched && form.hasError('passwordMismatch')" />
            @if (form.touched && form.hasError('passwordMismatch')) {
              <span class="error-msg">{{ 'auth.register.passwordMismatch' | transloco }}</span>
            }
          </div>

          @if (captchaSiteKey) {
            <div class="field">
              <app-turnstile [siteKey]="captchaSiteKey" (token)="captchaToken = $event" />
            </div>
          }

          @if (errorKey()) {
            <div class="alert-error" role="alert">{{ errorKey()! | transloco }}</div>
          }

          <button type="submit" class="btn-primary" [disabled]="loading()">
            {{ 'auth.register.submitButton' | transloco }}
          </button>
        </form>

        <p class="footer-link">
          {{ 'auth.register.haveAccount' | transloco }}
          <a routerLink="/auth/login">{{ 'auth.register.signIn' | transloco }}</a>
        </p>
      } @else {
        <h1>{{ 'auth.register.checkEmailTitle' | transloco }}</h1>
        <p class="subtitle">
          {{ 'auth.register.checkEmailBody' | transloco: { email: sentTo() } }}
        </p>

        @if (errorKey()) {
          <div class="alert-error" role="alert">{{ errorKey()! | transloco }}</div>
        }
        @if (resendDone()) {
          <p class="subtitle">{{ 'auth.register.resendDone' | transloco }}</p>
        }

        @if (captchaSiteKey) {
          <div class="field">
            <app-turnstile [siteKey]="captchaSiteKey" (token)="captchaToken = $event" />
          </div>
        }

        <button type="button" class="btn-secondary" (click)="resend()"
                [disabled]="loading() || cooldown() > 0">
          @if (cooldown() > 0) {
            {{ 'auth.register.resendCooldown' | transloco: { seconds: cooldown() } }}
          } @else {
            {{ 'auth.register.resendButton' | transloco }}
          }
        </button>
      }
    </div>
  `
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly registration = inject(RegistrationService);
  readonly captchaSiteKey = inject(AppModeService).captchaSiteKey;

  @ViewChild(TurnstileComponent) private turnstile?: TurnstileComponent;

  captchaToken: string | null = null;

  readonly loading = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly sentTo = signal<string | null>(null);
  readonly resendDone = signal(false);
  readonly cooldown = signal(0);

  readonly form = this.fb.nonNullable.group({
    orgName: ['', [Validators.required, Validators.maxLength(255)]],
    ownerEmail: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(255)]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordsMatch });

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.loading()) return;
    if (this.captchaSiteKey && !this.captchaToken) {
      this.errorKey.set('auth.register.errors.captchaRequired');
      return;
    }

    this.loading.set(true);
    this.errorKey.set(null);
    const { orgName, ownerEmail, password } = this.form.getRawValue();

    this.registration.register({
      orgName, ownerEmail, password, captchaToken: this.captchaToken ?? undefined
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.sentTo.set(ownerEmail);
        this.startCooldown();
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorKey.set(this.mapError(err));
        this.turnstile?.reset();
      }
    });
  }

  resend(): void {
    const email = this.sentTo();
    if (!email || this.loading() || this.cooldown() > 0) return;
    if (this.captchaSiteKey && !this.captchaToken) {
      this.errorKey.set('auth.register.errors.captchaRequired');
      return;
    }

    this.loading.set(true);
    this.errorKey.set(null);
    this.resendDone.set(false);
    this.registration.resend(email, this.captchaToken ?? undefined).subscribe({
      next: () => {
        this.loading.set(false);
        this.resendDone.set(true);
        this.startCooldown();
        this.turnstile?.reset();
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorKey.set(this.mapError(err));
        this.turnstile?.reset();
      }
    });
  }

  private startCooldown(): void {
    this.cooldown.set(RESEND_COOLDOWN_SECONDS);
    const timer = setInterval(() => {
      this.cooldown.update(v => v - 1);
      if (this.cooldown() <= 0) clearInterval(timer);
    }, 1000);
  }

  private mapError(err: HttpErrorResponse): string {
    switch (err.error?.code as string | undefined) {
      case 'CAPTCHA_FAILED':
        return 'auth.register.errors.captchaFailed';
      case 'TOO_MANY_REQUESTS':
        return 'auth.register.errors.tooManyRequests';
      default:
        return 'auth.register.errors.generic';
    }
  }
}
