import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  role: string;
  therapistProfileId?: string;
  exp: number;
}

/**
 * Route guard that checks if user has permission to access schedule management
 * Allowed roles: SYSTEM_ADMINISTRATOR, THERAPIST (with or without RECEPTION_ADMIN_STAFF)
 */
export const scheduleGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.token;
  if (!token) {
    return router.createUrlTree(['/auth/login']);
  }

  try {
    const decoded = jwtDecode<JwtPayload>(token);
    const role = decoded.role;

    // Allow SYSTEM_ADMINISTRATOR, THERAPIST, RECEPTION_ADMIN_STAFF, SUPERVISOR
    const allowedRoles = [
      'SYSTEM_ADMINISTRATOR',
      'THERAPIST',
      'RECEPTION_ADMIN_STAFF',
      'SUPERVISOR'
    ];

    if (allowedRoles.includes(role)) {
      return true;
    }

    return router.createUrlTree(['/']);
  } catch (error) {
    console.error('Error decoding JWT:', error);
    return router.createUrlTree(['/auth/login']);
  }
};

/**
 * Helper to get current user role from JWT
 */
export function getCurrentUserRole(authService: AuthService): string | null {
  const token = authService.token;
  if (!token) return null;

  try {
    const decoded = jwtDecode<JwtPayload>(token);
    return decoded.role;
  } catch (error) {
    console.error('Error decoding JWT:', error);
    return null;
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
  const role = getCurrentUserRole(authService);
  return role === 'SYSTEM_ADMINISTRATOR';
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
  const role = getCurrentUserRole(authService);
  const currentTherapistId = getCurrentTherapistProfileId(authService);

  if (role === 'SYSTEM_ADMINISTRATOR') {
    return true;
  }

  if (role === 'RECEPTION_ADMIN_STAFF' && targetTherapistId) {
    return currentTherapistId === targetTherapistId;
  }

  return false;
}
