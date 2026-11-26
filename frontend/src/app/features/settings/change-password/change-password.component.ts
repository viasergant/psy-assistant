import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

const ERROR_KEYS: Record<string, string> = {
  INVALID_CURRENT_PASSWORD: 'settings.password.errors.invalidCurrent',
  SAME_PASSWORD: 'settings.password.errors.samePassword',
  ACCOUNT_LOCKED: 'settings.password.errors.locked',
  PASSWORD_POLICY_VIOLATION: 'settings.password.errors.policy'
};

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  return group.get('newPassword')?.value === group.get('confirmPassword')?.value ? null : { passwordMismatch: true };
}

/** Self-service password change for the signed-in user (Settings page). */
@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [ReactiveFormsModule, TranslocoPipe],
  templateUrl: './change-password.component.html'
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly errorKey = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required, Validators.maxLength(72)]],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordsMatch }
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.saved.set(false);
    this.errorKey.set(null);

    const { currentPassword, newPassword } = this.form.getRawValue();
    this.auth
      .changePassword({ currentPassword, newPassword })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.form.reset();
          this.saved.set(true);
        },
        error: err => this.errorKey.set(ERROR_KEYS[err?.error?.code] ?? 'settings.password.errors.generic')
      });
  }
}
