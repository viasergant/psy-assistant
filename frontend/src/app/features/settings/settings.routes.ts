import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./settings.component').then(m => m.SettingsComponent)
  }
] satisfies Routes;
