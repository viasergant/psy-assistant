import { Pipe, PipeTransform } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

/**
 * Translates a specialization name (as returned by the API) using the i18n key
 * under `specializations.names.*`. Falls back to the original name if no key found.
 *
 * Key derivation: words split on spaces/hyphens, first word lowercased,
 * subsequent words capitalized (camelCase). E.g. "Anxiety Disorders" → "anxietyDisorders".
 */
@Pipe({
  name: 'translateSpecialization',
  standalone: true,
  pure: false,
})
export class TranslateSpecializationPipe implements PipeTransform {
  constructor(private readonly transloco: TranslocoService) {}

  transform(name: string): string {
    if (!name) return name;
    const key = `specializations.names.${this.toKey(name)}`;
    const translated = this.transloco.translate(key);
    return translated === key ? name : translated;
  }

  private toKey(name: string): string {
    return name
      .split(/[\s\-]+/)
      .map((word, index) =>
        index === 0
          ? word.toLowerCase()
          : word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
      )
      .join('');
  }
}
