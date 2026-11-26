import { Component, inject, Input, OnChanges, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { ClientTelegramService, ClientTelegramStatus } from './client-telegram.service';

const LINK_POLL_MS = 3000;
const LINK_POLL_MAX_MS = 10 * 60 * 1000;

/** Lets a therapist invite a client to the Telegram bot and revoke the link. */
@Component({
  selector: 'app-client-telegram',
  standalone: true,
  imports: [FormsModule, TranslocoPipe],
  template: `
    @if (status(); as s) {
      <div class="section">
        <h2 class="section-title">{{ 'clients.telegram.title' | transloco }}</h2>
        <p class="field-hint">{{ 'clients.telegram.description' | transloco }}</p>

        @if (!s.available) {
          <p class="field-hint" role="status">{{ 'clients.telegram.unavailable' | transloco }}</p>
        } @else {
          <div class="telegram-row" role="status">
            @if (s.linked) {
              <span class="badge badge-success">{{ 'clients.telegram.connected' | transloco }}</span>
              <button type="button" class="btn-danger btn-sm" (click)="disconnect()" [disabled]="busy()">
                {{ 'clients.telegram.disconnect' | transloco }}
              </button>
            } @else {
              <span class="badge">{{ 'clients.telegram.notConnected' | transloco }}</span>
            }
          </div>

          @if (!s.linked) {
            <div class="telegram-row">
              <label for="client-tg-language">{{ 'clients.telegram.language' | transloco }}</label>
              <select id="client-tg-language" [(ngModel)]="language" name="clientTelegramLanguage">
                @for (l of languages; track l) {
                  <option [value]="l">{{ 'clients.telegram.languages.' + l | transloco }}</option>
                }
              </select>
              <button type="button" class="btn-primary btn-sm" (click)="invite()" [disabled]="busy()">
                {{ 'clients.telegram.invite' | transloco }}
              </button>
            </div>
          }

          @if (inviteUrl(); as url) {
            <div class="telegram-invite">
              <p class="field-hint">{{ 'clients.telegram.inviteHint' | transloco }}</p>
              <div class="telegram-row">
                <input type="text" readonly [value]="url" class="telegram-url"
                       [attr.aria-label]="'clients.telegram.linkLabel' | transloco" />
                <button type="button" class="btn-secondary btn-sm" (click)="copy(url)">
                  {{ 'clients.telegram.copy' | transloco }}
                </button>
                @if (copied()) {
                  <span class="save-confirmation">{{ 'clients.telegram.copied' | transloco }}</span>
                }
              </div>
            </div>
          }
        }

        @if (error(); as key) {
          <div class="alert-error" role="alert">{{ key | transloco }}</div>
        }
      </div>
    }
  `,
  styles: [`
    .telegram-row { display: flex; flex-wrap: wrap; align-items: center; gap: 0.75rem; margin: 0.5rem 0; }
    .telegram-url { flex: 1 1 22rem; min-width: 0; }
  `]
})
export class ClientTelegramComponent implements OnChanges, OnDestroy {
  private readonly service = inject(ClientTelegramService);
  private pollTimer: ReturnType<typeof setInterval> | null = null;

  @Input({ required: true }) clientId!: string;

  readonly status = signal<ClientTelegramStatus | null>(null);
  readonly inviteUrl = signal<string | null>(null);
  readonly busy = signal(false);
  readonly copied = signal(false);
  readonly error = signal<string | null>(null);

  readonly languages = ['en', 'uk'];
  /** Language of the bot's replies to the client. */
  language = 'uk';

  ngOnChanges(): void {
    this.stopPolling();
    this.inviteUrl.set(null);
    this.error.set(null);
    this.service.status(this.clientId).subscribe({
      next: s => this.status.set(s),
      error: () => this.error.set('clients.telegram.errors.generic')
    });
  }

  invite(): void {
    this.busy.set(true);
    this.error.set(null);
    this.copied.set(false);
    this.service.createInvite(this.clientId, this.language, this.browserTimezone()).subscribe({
      next: ({ url }) => {
        this.inviteUrl.set(url);
        this.busy.set(false);
        this.startPolling();
      },
      error: () => {
        this.busy.set(false);
        this.error.set('clients.telegram.errors.invite');
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  disconnect(): void {
    this.busy.set(true);
    this.error.set(null);
    this.service.unlink(this.clientId).subscribe({
      next: () => {
        this.busy.set(false);
        this.inviteUrl.set(null);
        this.status.update(s => (s ? { ...s, linked: false } : s));
      },
      error: () => {
        this.busy.set(false);
        this.error.set('clients.telegram.errors.generic');
      }
    });
  }

  copy(url: string): void {
    navigator.clipboard?.writeText(url).then(() => this.copied.set(true), () => this.copied.set(false));
  }

  /** Waits for the client to press Start in Telegram, then flips the section to "Connected". */
  private startPolling(): void {
    this.stopPolling();
    const startedAt = Date.now();
    this.pollTimer = setInterval(() => {
      if (Date.now() - startedAt > LINK_POLL_MAX_MS) {
        this.stopPolling();
        return;
      }
      this.service.status(this.clientId).subscribe(s => {
        if (s.linked) {
          this.status.set(s);
          this.inviteUrl.set(null);
          this.stopPolling();
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

  /** Session times are shown to the client in the therapist's browser zone, matching the therapist's own settings. */
  private browserTimezone(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  }
}
