import { Component, ElementRef, inject, Input, OnDestroy, AfterViewInit, output } from '@angular/core';

interface TurnstileApi {
  render(el: HTMLElement, opts: Record<string, unknown>): string;
  reset(widgetId: string): void;
  remove(widgetId: string): void;
}

declare global {
  interface Window {
    turnstile?: TurnstileApi;
  }
}

const SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
let scriptPromise: Promise<void> | null = null;

function loadScript(): Promise<void> {
  if (window.turnstile) return Promise.resolve();
  scriptPromise ??= new Promise<void>((resolve, reject) => {
    const s = document.createElement('script');
    s.src = SCRIPT_SRC;
    s.async = true;
    s.onload = () => resolve();
    s.onerror = () => {
      scriptPromise = null;
      reject(new Error('turnstile script failed'));
    };
    document.head.appendChild(s);
  });
  return scriptPromise;
}

/**
 * Cloudflare Turnstile widget. Emits the response token, or null when it expires/errors.
 * Renders nothing when no site key is supplied (CAPTCHA disabled).
 */
@Component({
  selector: 'app-turnstile',
  standalone: true,
  template: ''
})
export class TurnstileComponent implements AfterViewInit, OnDestroy {
  @Input() siteKey: string | null = null;
  readonly token = output<string | null>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private widgetId: string | null = null;

  async ngAfterViewInit(): Promise<void> {
    if (!this.siteKey) return;
    try {
      await loadScript();
    } catch {
      this.token.emit(null);
      return;
    }
    this.widgetId = window.turnstile!.render(this.host.nativeElement, {
      sitekey: this.siteKey,
      callback: (t: string) => this.token.emit(t),
      'expired-callback': () => this.token.emit(null),
      'error-callback': () => this.token.emit(null)
    });
  }

  /** Resets the challenge (tokens are single-use). */
  reset(): void {
    if (this.widgetId) window.turnstile?.reset(this.widgetId);
    this.token.emit(null);
  }

  ngOnDestroy(): void {
    if (this.widgetId) window.turnstile?.remove(this.widgetId);
  }
}
