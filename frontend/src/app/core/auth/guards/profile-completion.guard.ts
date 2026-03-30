import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { HttpClient } from '@angular/common/http';
import { jwtDecode } from 'jwt-decode';
import { map, catchError, of } from 'rxjs';

/**
 * Route guard that checks if the authenticated therapist has completed their profile.
 * If incomplete, shows a banner or redirects to the profile completion wizard.
 * 
 * Apply this guard to therapist-only routes where a complete profile is required.
 */
export const profileCompletionGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const http = inject(HttpClient);
  const router = inject(Router);

  const token = authService.token;

  // Not authenticated - let auth guard handle it
  if (!token) {
    return true;
  }

  try {
    const decoded: any = jwtDecode(token);
    
    // Only apply to therapist role
    if (decoded.role !== 'THERAPIST') {
      return true;
    }

    // Check profile completion status via API
    return http.get<{ status: string }>(`/api/v1/therapists/profile/completion-status`)
      .pipe(
        map(response => {
          if (response.status === 'INCOMPLETE') {
            // Don't redirect if already on wizard page
            if (state.url.startsWith('/therapist/profile/complete')) {
              return true;
            }
            
            // Redirect to profile completion wizard
            return router.parseUrl('/therapist/profile/complete');
          }
          return true;
        }),
        catchError(error => {
          console.error('Error checking profile completion:', error);
          // Allow access on error to avoid blocking user
          return of(true);
        })
      );
  } catch (error) {
    console.error('Error decoding JWT:', error);
    return true;
  }
};
