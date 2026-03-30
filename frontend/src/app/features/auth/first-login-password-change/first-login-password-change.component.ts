import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/auth/auth.service';
import { finalize } from 'rxjs';

/**
 * Password change component shown to users on first login when mustChangePassword=true.
 * Validates password strength and updates the user's password before granting full access.
 */
@Component({
  selector: 'app-first-login-password-change',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    MessageModule
  ],
  templateUrl: './first-login-password-change.component.html',
  styleUrl: './first-login-password-change.component.scss'
})
export class FirstLoginPasswordChangeComponent {
  form: FormGroup;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(10)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  /**
   * Custom validator to ensure new password and confirmation match.
   */
  private passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }

  /**
   * Submits the password change request.
   */
  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    const request = {
      currentPassword: this.form.value.currentPassword,
      newPassword: this.form.value.newPassword
    };

    this.authService.changePasswordFirstLogin(request)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: () => {
          // Password changed successfully, navigate to main app
          // The route guard will check profile completion status for therapists
          this.router.navigate(['/']);
        },
        error: (err) => {
          console.error('Password change failed:', err);
          this.errorMessage = err.error?.message || 'Current password is incorrect. Please try again.';
        }
      });
  }

  /**
   * Form field accessors for template.
   */
  get currentPassword() { return this.form.get('currentPassword'); }
  get newPassword() { return this.form.get('newPassword'); }
  get confirmPassword() {return this.form.get('confirmPassword'); }
  get passwordMismatch() { return this.form.hasError('passwordMismatch') && this.confirmPassword?.touched; }
}
