import { Routes } from '@angular/router';

export default [
  {
    path: '',
    redirectTo: 'users',
    pathMatch: 'full'
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./users/components/user-list/user-list.component').then(
        m => m.UserListComponent
      )
  }
] satisfies Routes;
