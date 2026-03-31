import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from './i18n.service';
import { Select } from 'primeng/select';
import { FormsModule } from '@angular/forms';
import { TranslocoService, TranslocoModule } from '@jsverse/transloco';
import { map, Observable } from 'rxjs';

interface LanguageOption {
  code: string;
  label: string;
}

/**
 * Language switcher dropdown component.
 *
 * Displays a dropdown with supported languages (English, Ukrainian).
 * Changing the language updates the cookie and Transloco active language.
 *
 * Reusable across authentication pages and application shell.
 */
@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [CommonModule, Select, FormsModule, TranslocoModule],
  templateUrl: './language-switcher.component.html',
  styleUrls: ['./language-switcher.component.scss']
})
export class LanguageSwitcherComponent {
  private readonly i18nService = inject(I18nService);
  private readonly transloco = inject(TranslocoService);

  // Reactive language options that update on language change
  readonly languages$: Observable<LanguageOption[]> = this.transloco.langChanges$.pipe(
    map(() => [
      { code: 'en', label: this.transloco.getActiveLang() === 'en' ? 'English' : 'Англійська' },
      { code: 'uk', label: this.transloco.getActiveLang() === 'uk' ? 'Українська' : 'Ukrainian' }
    ])
  );

  selectedLanguage: string = this.i18nService.getCurrentLocale();

  /**
   * Handles language change from dropdown.
   * Updates cookie and active Transloco language.
   */
  onLanguageChange(languageCode: string): void {
    this.i18nService.setLanguage(languageCode);
    this.selectedLanguage = languageCode;
  }
}
