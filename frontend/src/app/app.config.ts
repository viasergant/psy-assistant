import {
  APP_INITIALIZER,
  ApplicationConfig,
  isDevMode,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideTransloco } from '@jsverse/transloco';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

import { appRoutes } from './app.routes';
import { jwtInterceptor } from './core/auth/jwt.interceptor';
import { TranslocoHttpLoader } from './core/i18n/transloco-loader';
import { AuthService } from './core/auth/auth.service';

/**
 * Attempts a silent token refresh on application startup using the HttpOnly
 * refresh-token cookie. Returns a factory function compatible with APP_INITIALIZER.
 *
 * Errors are swallowed — an unauthenticated state is valid on the login page.
 */
function initAuth(authService: AuthService): () => Promise<void> {
  return () =>
    authService
      .refreshToken()
      .toPromise()
      .then(() => {})
      .catch(() => {});
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(appRoutes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideTransloco({
      config: {
        availableLangs: ['en'],
        defaultLang: 'en',
        reRenderOnLangChange: true,
        prodMode: !isDevMode()
      },
      loader: TranslocoHttpLoader
    }),
    providePrimeNG({ theme: { preset: Aura } }),
    {
      provide: APP_INITIALIZER,
      useFactory: initAuth,
      deps: [AuthService],
      multi: true
    }
  ]
};
