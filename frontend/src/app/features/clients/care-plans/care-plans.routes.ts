import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./care-plan-list/care-plan-list.component').then(
        (m) => m.CarePlanListComponent
      ),
  },
  {
    path: ':planId',
    loadComponent: () =>
      import('./care-plan-detail/care-plan-detail.component').then(
        (m) => m.CarePlanDetailComponent
      ),
  },
] satisfies Routes;
