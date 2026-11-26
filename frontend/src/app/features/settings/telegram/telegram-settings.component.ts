import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { TelegramSettings, TelegramSettingsService, TelegramSettingsUpdate } from './telegram-settings.service';

const LINK_POLL_MS = 3000;
const LINK_POLL_MAX_MS = 10 * 60 * 1000;

@Component({
  selector: 'app-telegram-settings',
  standalone: true,
  imports: [FormsModule, TranslocoPipe],
  templateUrl: './telegram-settings.component.html',
  styleUrl: './telegram-settings.component.scss'
})
export class TelegramSettingsComponent implements OnInit, OnDestroy {
  private readonly service = inject(TelegramSettingsService);
  private pollTimer: ReturnType<typeof setInterval> | null = null;

  readonly settings = signal<TelegramSettings | null>(null);
  readonly waitingForLink = signal(false);
  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly error = signal<string | null>(null);
  readonly testSent = signal(false);

  /** Monday-first, matching the backend bit mask (bit 0 = Monday). */
  readonly days = [
    { bit: 0, key: 'mon' }, { bit: 1, key: 'tue' }, { bit: 2, key: 'wed' }, { bit: 3, key: 'thu' },
    { bit: 4, key: 'fri' }, { bit: 5, key: 'sat' }, { bit: 6, key: 'sun' }
  ];
  readonly languages = ['en', 'uk'];
  readonly timezones: string[] = (Intl as unknown as { supportedValuesOf?: (k: string) => string[] })
    .supportedValuesOf?.('timeZone') ?? [];

  /** Editable copy of the settings, bound to the form. */
  form: TelegramSettings | null = null;

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  isDayOn(bit: number): boolean {
    return !!this.form && (this.form.digestDays & (1 << bit)) !== 0;
  }

  toggleDay(bit: number): void {
    if (this.form) {
      this.form.digestDays ^= 1 << bit;
      this.saved.set(false);
    }
  }

  connect(): void {
    this.error.set(null);
    this.service.createLink().subscribe({
      next: ({ url }) => {
        window.open(url, '_blank', 'noopener');
        this.waitingForLink.set(true);
        this.startPolling();
      },
      error: () => this.error.set('settings.telegram.errors.link')
    });
  }

  disconnect(): void {
    this.service.unlink().subscribe({
      next: () => {
        this.stopPolling();
        this.load();
      },
      error: () => this.error.set('settings.telegram.errors.generic')
    });
  }

  save(): void {
    if (!this.form) {
      return;
    }
    const f = this.form;
    const payload: TelegramSettingsUpdate = {
      timezone: f.timezone, language: f.language, digestEnabled: f.digestEnabled, digestTime: f.digestTime,
      digestDays: f.digestDays, skipEmptyDigest: f.skipEmptyDigest, reminderEnabled: f.reminderEnabled,
      reminderMinutesBefore: f.reminderMinutesBefore, includeClientNames: f.includeClientNames
    };
    this.saving.set(true);
    this.saved.set(false);
    this.error.set(null);
    this.service.update(payload).subscribe({
      next: s => {
        this.apply(s);
        this.saving.set(false);
        this.saved.set(true);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('settings.telegram.errors.save');
      }
    });
  }

  sendTest(): void {
    this.testSent.set(false);
    this.service.sendTest().subscribe({
      next: () => this.testSent.set(true),
      error: () => this.error.set('settings.telegram.errors.generic')
    });
  }

  private load(): void {
    this.service.get().subscribe({
      next: s => {
        // Brand-new settings default to UTC; start from the browser's zone instead.
        if (!s.linked && s.timezone === 'UTC') {
          s = { ...s, timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC' };
        }
        this.apply(s);
      },
      error: () => this.error.set('settings.telegram.errors.generic')
    });
  }

  private apply(s: TelegramSettings): void {
    this.settings.set(s);
    this.form = { ...s };
    if (s.linked) {
      this.waitingForLink.set(false);
      this.stopPolling();
    }
  }

  private startPolling(): void {
    this.stopPolling();
    const startedAt = Date.now();
    this.pollTimer = setInterval(() => {
      if (Date.now() - startedAt > LINK_POLL_MAX_MS) {
        this.waitingForLink.set(false);
        this.stopPolling();
        return;
      }
      this.service.get().subscribe(s => {
        if (s.linked) {
          this.apply(s);
        }
      });
    }, LINK_POLL_MS);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }
}
