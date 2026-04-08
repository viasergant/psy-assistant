import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./client-list/client-list.component').then(m => m.ClientListComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./client-detail/client-detail.component').then(
        m => m.ClientDetailComponent
      )
  },
  {
    path: ':clientId/care-plans',
    loadChildren: () =>
      import('./care-plans/care-plans.routes').then(m => m.default)
  }
] satisfies Routes;
