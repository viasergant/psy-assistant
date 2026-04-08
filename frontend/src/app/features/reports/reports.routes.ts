import { Routes } from '@angular/router';

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
] satisfies Routes;
