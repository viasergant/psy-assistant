import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from './i18n.service';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';

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
  imports: [CommonModule, DropdownModule, FormsModule],
  templateUrl: './language-switcher.component.html',
  styleUrls: ['./language-switcher.component.scss']
})
export class LanguageSwitcherComponent {
  private readonly i18nService = inject(I18nService);

  readonly languages: LanguageOption[] = [
    { code: 'en', label: 'English' },
    { code: 'uk', label: 'Українська' }
  ];

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
