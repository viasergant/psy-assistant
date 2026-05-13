import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./components/risk-flag-type-list/risk-flag-type-list.component').then(
        (m) => m.RiskFlagTypeListComponent
      )
  }
] satisfies Routes;
