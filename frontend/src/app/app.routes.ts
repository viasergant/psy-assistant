import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { mustChangePasswordGuard } from './core/auth/guards/must-change-password.guard';
import { profileCompletionGuard } from './core/auth/guards/profile-completion.guard';
import { roleGuard } from './core/auth/guards/role.guard';
import { NAV_ITEMS, ROUTE_ROLES } from './core/auth/permissions.config';
import { PermissionService } from './core/auth/permission.service';
import { ShellComponent } from './layout/shell/shell.component';

export const appRoutes: Routes = [
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard, mustChangePasswordGuard, profileCompletionGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => {
          const permissions = inject(PermissionService);
          const first = NAV_ITEMS.find(item => permissions.hasAnyRole(item.roles));
          return first?.route ?? '/auth/login';
        }
      },
      {
        path: 'leads',
        canActivate: [roleGuard(ROUTE_ROLES['leads'])],
        loadChildren: () => import('./features/leads/leads.routes')
      },
      {
        path: 'clients',
        canActivate: [roleGuard(ROUTE_ROLES['clients'])],
        loadChildren: () => import('./features/clients/clients.routes')
      },
      {
        path: 'schedule',
        canActivate: [roleGuard(ROUTE_ROLES['schedule'])],
        loadChildren: () => import('./features/schedule/schedule.routes')
      },
      {
        path: 'sessions',
        canActivate: [roleGuard(ROUTE_ROLES['sessions'])],
        loadChildren: () => import('./features/sessions/sessions.routes')
      },
      {
        path: 'billing',
        canActivate: [roleGuard(ROUTE_ROLES['billing'])],
        loadChildren: () => import('./features/billing/billing.routes')
      },
      {
        path: 'reports',
        canActivate: [roleGuard(ROUTE_ROLES['reports'])],
        loadChildren: () => import('./features/reports/reports.routes')
      },
      {
        path: 'admin',
        canActivate: [roleGuard(ROUTE_ROLES['admin'])],
        loadChildren: () => import('./features/admin/admin.routes')
      }
    ]
  },
  {
    path: 'therapist/profile/complete',
    canActivate: [authGuard],
    loadComponent: () => 
      import('./features/therapists/profile-wizard/therapist-profile-wizard.component')
        .then(m => m.TherapistProfileWizardComponent)
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes')
  }
];
