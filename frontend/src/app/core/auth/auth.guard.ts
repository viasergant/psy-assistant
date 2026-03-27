import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Route guard that redirects unauthenticated users to /auth/login.
 * Uses the in-memory AuthService instead of localStorage.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  if (authService.isAuthenticated()) return true;
  return inject(Router).createUrlTree(['/auth/login']);
};
