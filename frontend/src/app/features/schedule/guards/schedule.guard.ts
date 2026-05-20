import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  roles: string[]; // Array of authorities including "ROLE_X" and permissions
  therapistProfileId?: string;
  exp: number;
}

/**
 * Route guard that checks if user has permission to access schedule management
 * Allowed roles: SYSTEM_ADMINISTRATOR, THERAPIST, RECEPTION_ADMIN_STAFF, SUPERVISOR
 */
export const scheduleGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.token;
  if (!token) {
    console.warn('Schedule guard: No token found, redirecting to login');
    return router.createUrlTree(['/auth/login']);
  }

  try {
    const decoded = jwtDecode<JwtPayload>(token);
    const roles = decoded.roles || [];

    console.log('Schedule guard: User roles:', roles);

    // Check if user has any of the allowed roles (with ROLE_ prefix)
    const allowedRoles = [
      'ROLE_SYSTEM_ADMINISTRATOR',
      'ROLE_THERAPIST',
      'ROLE_RECEPTION_ADMIN_STAFF',
      'ROLE_SUPERVISOR'
    ];

    const hasAccess = roles.some(role => allowedRoles.includes(role));

    if (hasAccess) {
      console.log('Schedule guard: Access granted');
      return true;
    }

    // User doesn't have permission - redirect to clients with a console warning
    console.warn(`Schedule guard: Access denied. User roles: ${roles.join(', ')}. Required one of:`, allowedRoles);
    return router.createUrlTree(['/clients']);
  } catch (error) {
    console.error('Schedule guard: Error decoding JWT:', error);
    return router.createUrlTree(['/auth/login']);
  }
};

/**
 * Helper to get all current user roles from JWT
 * Returns all roles stripped of the ROLE_ prefix
 */
export function getCurrentUserRole(authService: AuthService): string[] {
  const token = authService.token;
  if (!token) return [];

  try {
    const decoded = jwtDecode<JwtPayload>(token);
    return (decoded.roles ?? [])
      .filter(r => r.startsWith('ROLE_'))
      .map(r => r.replace('ROLE_', ''));
  } catch (error) {
    console.error('Error decoding JWT:', error);
    return [];
  }
}

/**
 * Helper to get current therapist profile ID from JWT
 */
export function getCurrentTherapistProfileId(
  authService: AuthService
): string | null {
  const token = authService.token;
  if (!token) return null;

  try {
    const decoded = jwtDecode<JwtPayload>(token);
    return decoded.therapistProfileId || null;
  } catch (error) {
    console.error('Error decoding JWT:', error);
    return null;
  }
}

/**
 * Helper to check if current user is system administrator
 */
export function isSystemAdmin(authService: AuthService): boolean {
  return getCurrentUserRole(authService).includes('SYSTEM_ADMINISTRATOR');
}

/**
 * Helper to check if current user has edit permissions for schedule
 * SYSTEM_ADMINISTRATOR: can edit all schedules
 * RECEPTION_ADMIN_STAFF: can edit own schedule
 * THERAPIST: read-only (can only submit leave requests)
 */
export function canEditSchedule(
  authService: AuthService,
  targetTherapistId?: string
): boolean {
  const roles = getCurrentUserRole(authService);
  const currentTherapistId = getCurrentTherapistProfileId(authService);

  if (roles.includes('SYSTEM_ADMINISTRATOR')) {
    return true;
  }

  if (roles.includes('RECEPTION_ADMIN_STAFF') && targetTherapistId) {
    return currentTherapistId === targetTherapistId;
  }

  return false;
}
