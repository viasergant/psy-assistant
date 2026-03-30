import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./components/therapist-list/therapist-list.component').then(
        m => m.TherapistListComponent
      )
  }
] satisfies Routes;
