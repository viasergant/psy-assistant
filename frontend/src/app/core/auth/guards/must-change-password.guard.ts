import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { jwtDecode } from 'jwt-decode';

/**
 * Route guard that checks if the authenticated user has mustChangePassword flag.
 * If true, redirects to the password change page.
 * 
 * Apply this guard to all authenticated routes except:
 * - /auth/login
 * - /auth/first-login-password-change
 * - /auth/logout
 */
export const mustChangePasswordGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.token;

  // Not authenticated - let auth guard handle it
  if (!token) {
    return true;
  }

  try {
    const decoded: any = jwtDecode(token);
    
    // Check if mustChangePassword flag is present and true
    if (decoded.mustChangePassword === true) {
      // Don't redirect if already on password change page
      if (state.url === '/auth/first-login-password-change') {
        return true;
      }
      
      // Redirect to password change page
      return router.parseUrl('/auth/first-login-password-change');
    }

    return true;
  } catch (error) {
    console.error('Error decoding JWT:', error);
    // If token is invalid, let it through - auth guard will handle logout
    return true;
  }
};
