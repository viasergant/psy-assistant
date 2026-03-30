import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { mustChangePasswordGuard } from './core/auth/guards/must-change-password.guard';
import { profileCompletionGuard } from './core/auth/guards/profile-completion.guard';
import { ShellComponent } from './layout/shell/shell.component';

export const appRoutes: Routes = [
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard, mustChangePasswordGuard, profileCompletionGuard],
    children: [
      { path: '', redirectTo: 'clients', pathMatch: 'full' },
      {
        path: 'leads',
        loadChildren: () => import('./features/leads/leads.routes')
      },
      {
        path: 'clients',
        loadChildren: () => import('./features/clients/clients.routes')
      },
      {
        path: 'schedule',
        loadChildren: () => import('./features/schedule/schedule.routes')
      },
      {
        path: 'sessions',
        loadChildren: () => import('./features/sessions/sessions.routes')
      },
      {
        path: 'billing',
        loadChildren: () => import('./features/billing/billing.routes')
      },
      {
        path: 'reports',
        loadChildren: () => import('./features/reports/reports.routes')
      },
      {
        path: 'admin',
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
