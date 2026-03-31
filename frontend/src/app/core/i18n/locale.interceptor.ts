import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { TranslocoService } from '@jsverse/transloco';

/**
 * HTTP interceptor that adds the Accept-Language header to all backend requests.
 *
 * Excludes:
 * - /assets/i18n/* (translation files - would cause infinite loop)
 * - External URLs (not starting with /)
 *
 * The header value is set to the current active locale (e.g., 'en', 'uk').
 */
export const localeInterceptor: HttpInterceptorFn = (req, next) => {
  const transloco = inject(TranslocoService);

  // Skip interceptor for translation file requests and external URLs
  if (req.url.includes('/assets/i18n/') || !req.url.startsWith('/')) {
    return next(req);
  }

  // Add Accept-Language header with current locale
  const currentLocale = transloco.getActiveLang();
  const modifiedReq = req.clone({
    setHeaders: {
      'Accept-Language': currentLocale
    }
  });

  return next(modifiedReq);
};
