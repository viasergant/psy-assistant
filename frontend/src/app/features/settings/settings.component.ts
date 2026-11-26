import { Component, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { I18nService } from '../../core/i18n/i18n.service';
import { PermissionService } from '../../core/auth/permission.service';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { TelegramSettingsComponent } from './telegram/telegram-settings.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [TranslocoPipe, ChangePasswordComponent, TelegramSettingsComponent],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {
  private readonly i18nService = inject(I18nService);
  private readonly permissions = inject(PermissionService);

  /** Matches the backend rule for /api/v1/telegram/** (therapists and system administrators). */
  readonly canUseTelegram = (): boolean => this.permissions.hasAnyRole(['THERAPIST', 'SYSTEM_ADMINISTRATOR']);

  readonly currentLocale = signal(this.i18nService.getCurrentLocale());
  readonly savedLocale = signal(this.i18nService.getCurrentLocale());
  readonly saving = signal(false);
  readonly saved = signal(false);

  readonly languages = [
    { code: 'en', labelKey: 'settings.language.english' },
    { code: 'uk', labelKey: 'settings.language.ukrainian' }
  ];

  selectLanguage(code: string): void {
    this.currentLocale.set(code);
    this.saved.set(false);
  }

  async saveLanguage(): Promise<void> {
    const locale = this.currentLocale();
    if (locale === this.savedLocale() && this.saved()) {
      return;
    }

    this.saving.set(true);
    this.saved.set(false);

    this.i18nService.setLanguage(locale);
    await this.i18nService.syncWithBackend();

    this.savedLocale.set(locale);
    this.saving.set(false);
    this.saved.set(true);
  }
}
