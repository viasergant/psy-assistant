import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegistrationService } from './registration.service';

type State = 'pending' | 'success' | 'expired' | 'invalid';

/** Landing page for the emailed confirmation link: /auth/confirm-email?token=... */
@Component({
  selector: 'app-confirm-email',
  standalone: true,
  imports: [RouterLink, TranslocoPipe],
  styleUrl: './auth-page.scss',
  template: `
    <div class="auth-card">
      @switch (state()) {
        @case ('pending') {
          <h1>{{ 'auth.confirmEmail.pendingTitle' | transloco }}</h1>
        }
        @case ('success') {
          <h1>{{ 'auth.confirmEmail.successTitle' | transloco }}</h1>
          <p class="subtitle">{{ 'auth.confirmEmail.successBody' | transloco }}</p>
          <a class="btn-primary" routerLink="/auth/login">
            {{ 'auth.confirmEmail.goToLogin' | transloco }}
          </a>
        }
        @case ('expired') {
          <h1>{{ 'auth.confirmEmail.expiredTitle' | transloco }}</h1>
          <p class="subtitle">{{ 'auth.confirmEmail.expiredBody' | transloco }}</p>
          <a class="btn-secondary" routerLink="/auth/register">
            {{ 'auth.confirmEmail.registerAgain' | transloco }}
          </a>
        }
        @case ('invalid') {
          <h1>{{ 'auth.confirmEmail.invalidTitle' | transloco }}</h1>
          <p class="subtitle">{{ 'auth.confirmEmail.invalidBody' | transloco }}</p>
          <a class="btn-secondary" routerLink="/auth/login">
            {{ 'auth.confirmEmail.goToLogin' | transloco }}
          </a>
        }
      }
    </div>
  `
})
export class ConfirmEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly registration = inject(RegistrationService);

  readonly state = signal<State>('pending');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('invalid');
      return;
    }
    this.registration.confirm(token).subscribe({
      next: () => this.state.set('success'),
      error: (err: HttpErrorResponse) =>
        this.state.set(err.error?.code === 'TOKEN_EXPIRED' ? 'expired' : 'invalid')
    });
  }
}
