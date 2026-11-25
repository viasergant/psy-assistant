import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./components/global-user-list/global-user-list.component').then(
        (m) => m.GlobalUserListComponent
      )
  }
] satisfies Routes;
