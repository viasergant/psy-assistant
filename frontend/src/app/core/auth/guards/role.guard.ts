import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { TranslocoService } from '@jsverse/transloco';
import { AppRole } from '../permissions.config';
import { PermissionService } from '../permission.service';

/**
 * Role guard factory. Returns a functional CanActivateFn that allows access
 * only when the authenticated user holds one of the specified roles.
 * On failure shows a toast and redirects to /.
 */
export function roleGuard(allowedRoles: AppRole[]): CanActivateFn {
  return () => {
    const permissions = inject(PermissionService);
    const router = inject(Router);
    const messageService = inject(MessageService);
    const transloco = inject(TranslocoService);

    if (permissions.hasAnyRole(allowedRoles)) return true;

    messageService.add({
      severity: 'warn',
      summary: transloco.translate('auth.accessDenied'),
      life: 4000,
    });

    return router.createUrlTree(['/']);
  };
}
