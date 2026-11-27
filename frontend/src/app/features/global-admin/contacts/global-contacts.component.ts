import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { GlobalContactsService } from './global-contacts.service';

const MAX_CHANNELS = 10;

/** Global admin form for the public platform contact details. */
@Component({
  selector: 'app-global-contacts',
  standalone: true,
  imports: [ReactiveFormsModule, TranslocoPipe],
  template: `
    <form class="contacts-form" [formGroup]="form" (ngSubmit)="save()" novalidate>
      <h2>{{ 'globalAdmin.contacts.title' | transloco }}</h2>

      <label for="phone">{{ 'contacts.phone' | transloco }}</label>
      <input id="phone" type="tel" formControlName="phone" maxlength="32" />
      @if (form.controls.phone.invalid && form.controls.phone.touched) {
        <small role="alert">{{ 'globalAdmin.contacts.invalidPhone' | transloco }}</small>
      }

      <label for="email">{{ 'contacts.email' | transloco }}</label>
      <input id="email" type="email" formControlName="email" maxlength="254" />
      @if (form.controls.email.invalid && form.controls.email.touched) {
        <small role="alert">{{ 'globalAdmin.contacts.invalidEmail' | transloco }}</small>
      }

      <label for="telegram">{{ 'globalAdmin.contacts.telegramLabel' | transloco }}</label>
      <textarea id="telegram" rows="4" formControlName="telegram"></textarea>
      <small>{{ 'globalAdmin.contacts.telegramHint' | transloco }}</small>
      @if (form.controls.telegram.invalid && form.controls.telegram.touched) {
        <small role="alert">{{ 'globalAdmin.contacts.invalidTelegram' | transloco }}</small>
      }

      @if (status() === 'saved') {
        <p role="status">{{ 'globalAdmin.contacts.saved' | transloco }}</p>
      } @else if (status() === 'error') {
        <p role="alert">{{ 'globalAdmin.contacts.saveError' | transloco }}</p>
      }

      <button type="submit" class="btn-primary" [disabled]="saving()">
        {{ 'globalAdmin.contacts.save' | transloco }}
      </button>
    </form>
  `,
  styles: [`
    .contacts-form { display: grid; gap: 0.5rem; max-width: 36rem; padding: 1.5rem 2rem; }
  `]
})
export class GlobalContactsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(GlobalContactsService);

  readonly saving = signal(false);
  readonly status = signal<'idle' | 'saved' | 'error'>('idle');

  readonly form = this.fb.nonNullable.group({
    phone: ['', [Validators.pattern(/^$|^\+?[0-9 ()-]{5,32}$/)]],
    email: ['', [Validators.email]],
    telegram: ['', [channelsValidator]]
  });

  ngOnInit(): void {
    this.service.getContacts().subscribe({
      next: c => this.form.patchValue({
        phone: c.phone ?? '',
        email: c.email ?? '',
        telegram: c.telegramChannels.join('\n')
      }),
      error: () => this.status.set('error')
    });
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const { phone, email, telegram } = this.form.getRawValue();
    this.saving.set(true);
    this.status.set('idle');
    this.service.updateContacts({ phone, email, telegramChannels: splitChannels(telegram) }).subscribe({
      next: c => {
        this.form.patchValue({ telegram: c.telegramChannels.join('\n') });
        this.saving.set(false);
        this.status.set('saved');
      },
      error: () => {
        this.saving.set(false);
        this.status.set('error');
      }
    });
  }
}

function splitChannels(value: string): string[] {
  return value.split('\n').map(s => s.trim()).filter(s => s.length > 0);
}

function channelsValidator(control: { value: string }): { channels: true } | null {
  const entries = splitChannels(control.value ?? '');
  const valid = /^(@[A-Za-z0-9_]{5,32}|https:\/\/t\.me\/[A-Za-z0-9_+/-]{3,64})$/;
  return entries.length > MAX_CHANNELS || entries.some(e => !valid.test(e)) ? { channels: true } : null;
}
