import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/guards/role.guard';

/** Roles that can access the operational/financial reports (PA-55). */
const REPORTS_ROLES = ['SUPERVISOR', 'FINANCE', 'SYSTEM_ADMINISTRATOR'] as const;

export default [
  {
    path: '',
    redirectTo: 'caseload',
    pathMatch: 'full',
  },
  {
    path: 'caseload',
    loadComponent: () =>
      import('./caseload-overview/caseload-overview/caseload-overview.component').then(
        (m) => m.CaseloadOverviewComponent
      ),
  },
  {
    path: 'lead-conversion',
    canActivate: [roleGuard([...REPORTS_ROLES])],
    loadComponent: () =>
      import('./lead-conversion/lead-conversion.component').then(
        (m) => m.LeadConversionComponent
      ),
  },
  {
    path: 'therapist-utilization',
    canActivate: [roleGuard([...REPORTS_ROLES])],
    loadComponent: () =>
      import('./therapist-utilization/therapist-utilization.component').then(
        (m) => m.TherapistUtilizationComponent
      ),
  },
  {
    path: 'revenue',
    canActivate: [roleGuard([...REPORTS_ROLES])],
    loadComponent: () =>
      import('./revenue/revenue.component').then((m) => m.RevenueComponent),
  },
  {
    path: 'client-retention',
    canActivate: [roleGuard([...REPORTS_ROLES])],
    loadComponent: () =>
      import('./client-retention/client-retention.component').then(
        (m) => m.ClientRetentionComponent
      ),
  },
  {
    path: 'no-show-cancellation',
    canActivate: [roleGuard([...REPORTS_ROLES])],
    loadComponent: () =>
      import('./no-show-cancellation/no-show-cancellation.component').then(
        (m) => m.NoShowCancellationComponent
      ),
  },
] satisfies Routes;
