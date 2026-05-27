import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AppMode, AppModeService } from './app-mode.service';

/**
 * Route guard factory that blocks navigation to routes reserved for a
 * specific deployment mode.
 *
 * Usage in route config:
 *   canActivate: [modeGuard('CLINIC')]
 *
 * Redirects to /dashboard when the current mode does not match the required mode.
 */
export function modeGuard(requiredMode: AppMode): CanActivateFn {
  return () => {
    const modeService = inject(AppModeService);
    const router = inject(Router);

    if (modeService.mode === requiredMode) return true;

    return router.createUrlTree(['/']);
  };
}
