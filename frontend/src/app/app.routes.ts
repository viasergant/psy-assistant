import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';

export const appRoutes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', redirectTo: 'clients', pathMatch: 'full' },
      {
        path: 'leads',
        loadChildren: () => import('./features/leads/leads.routes'),
        canActivate: [authGuard]
      },
      {
        path: 'clients',
        loadChildren: () => import('./features/clients/clients.routes'),
        canActivate: [authGuard]
      },
      {
        path: 'schedule',
        loadChildren: () => import('./features/schedule/schedule.routes'),
        canActivate: [authGuard]
      },
      {
        path: 'sessions',
        loadChildren: () => import('./features/sessions/sessions.routes'),
        canActivate: [authGuard]
      },
      {
        path: 'billing',
        loadChildren: () => import('./features/billing/billing.routes'),
        canActivate: [authGuard]
      },
      {
        path: 'reports',
        loadChildren: () => import('./features/reports/reports.routes'),
        canActivate: [authGuard]
      },
      {
        path: 'admin',
        loadChildren: () => import('./features/admin/admin.routes'),
        canActivate: [authGuard]
      }
    ]
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes')
  }
];
