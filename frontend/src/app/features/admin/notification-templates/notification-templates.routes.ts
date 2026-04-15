import { Routes } from '@angular/router';

export default [
  {
    path: '',
    loadComponent: () =>
      import('./components/template-list/template-list.component').then(
        (m) => m.TemplateListComponent
      )
  }
] satisfies Routes;
