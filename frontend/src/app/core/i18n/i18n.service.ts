import { inject, Injectable } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

/**
 * i18n service managing locale detection, cookie persistence, and backend synchronization.
 *
 * Locale resolution priority:
 * 1. Cookie (pa_locale)
 * 2. Browser language (navigator.language)
 * 3. Default (en)
 *
 * The service writes the selected locale to a cookie with 1-year expiry.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly transloco = inject(TranslocoService);
  private readonly http = inject(HttpClient);

  private readonly COOKIE_NAME = 'pa_locale';
  private readonly SUPPORTED_LOCALES = ['en', 'uk'];
  private readonly DEFAULT_LOCALE = 'en';
  private readonly COOKIE_MAX_AGE_SECONDS = 31536000; // 1 year

  /**
   * Initializes i18n on app startup.
   * Reads cookie → falls back to browser language → defaults to 'en'.
   * Sets Transloco active language and writes cookie if not present.
   *
   * Called from APP_INITIALIZER to ensure locale is set before app renders.
   */
  async initialize(): Promise<void> {
    const detectedLocale = this.detectLocale();
    this.transloco.setActiveLang(detectedLocale);
    this.writeCookie(detectedLocale);

    // Load the translation file asynchronously
    await firstValueFrom(this.transloco.load(detectedLocale));
  }

  /**
   * Changes the active language, updates cookie, and syncs with backend.
   *
   * @param locale 'en' or 'uk'
   */
  setLanguage(locale: string): void {
    if (!this.SUPPORTED_LOCALES.includes(locale)) {
      console.warn(`[I18nService] Unsupported locale: ${locale}. Falling back to ${this.DEFAULT_LOCALE}`);
      locale = this.DEFAULT_LOCALE;
    }

    this.transloco.setActiveLang(locale);
    this.writeCookie(locale);
  }

  /**
   * Syncs the current locale with the backend user profile (for authenticated users).
   * Called after login to persist language preference to the database.
   */
  async syncWithBackend(): Promise<void> {
    const currentLocale = this.transloco.getActiveLang();
    try {
      await firstValueFrom(
        this.http.patch('/api/v1/users/me/language', { language: currentLocale })
      );
    } catch (error) {
      console.error('[I18nService] Failed to sync language with backend:', error);
    }
  }

  /**
   * Returns the current active locale.
   */
  getCurrentLocale(): string {
    return this.transloco.getActiveLang();
  }

  /**
   * Returns all supported locales.
   */
  getSupportedLocales(): string[] {
    return [...this.SUPPORTED_LOCALES];
  }

  /**
   * Detects locale from cookie → browser → default.
   */
  private detectLocale(): string {
    // 1. Check cookie
    const cookieLocale = this.readCookie();
    if (cookieLocale && this.SUPPORTED_LOCALES.includes(cookieLocale)) {
      return cookieLocale;
    }

    // 2. Check browser language
    const browserLang = navigator.language.split('-')[0]; // e.g., 'uk-UA' → 'uk'
    if (this.SUPPORTED_LOCALES.includes(browserLang)) {
      return browserLang;
    }

    // 3. Default
    return this.DEFAULT_LOCALE;
  }

  /**
   * Reads the pa_locale cookie.
   */
  private readCookie(): string | null {
    const name = this.COOKIE_NAME + '=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const cookies = decodedCookie.split(';');

    for (let cookie of cookies) {
      cookie = cookie.trim();
      if (cookie.startsWith(name)) {
        return cookie.substring(name.length);
      }
    }
    return null;
  }

  /**
   * Writes the pa_locale cookie with 1-year expiry, SameSite=Lax, Secure in production.
   */
  private writeCookie(locale: string): void {
    const isProduction = window.location.protocol === 'https:';
    const secureFlag = isProduction ? '; Secure' : '';
    const expires = new Date(Date.now() + this.COOKIE_MAX_AGE_SECONDS * 1000).toUTCString();

    document.cookie = `${this.COOKIE_NAME}=${locale}; expires=${expires}; path=/; SameSite=Lax${secureFlag}`;
  }
}
