import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./components/session-list/session-list.component').then(
        (m) => m.SessionListComponent
      ),
  },
] satisfies Routes;
