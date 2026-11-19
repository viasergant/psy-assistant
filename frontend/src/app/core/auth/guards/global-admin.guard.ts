import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../permission.service';

/**
 * Route guard that allows access only to the configured global admin account.
 * On failure shows a toast and redirects to /.
 */
export const globalAdminGuard: CanActivateFn = () => {
  const permissions = inject(PermissionService);
  const router = inject(Router);
  const messageService = inject(MessageService);
  const transloco = inject(TranslocoService);

  if (permissions.isGlobalAdmin()) return true;

  messageService.add({
    severity: 'warn',
    summary: transloco.translate('auth.accessDenied'),
    life: 4000,
  });

  return router.createUrlTree(['/']);
};
