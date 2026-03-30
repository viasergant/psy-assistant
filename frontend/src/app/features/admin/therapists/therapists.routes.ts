import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./components/therapist-list/therapist-list.component').then(
        m => m.TherapistListComponent
      )
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./components/therapist-detail/therapist-detail.component').then(
        m => m.TherapistDetailComponent
      )
  }
] satisfies Routes;
