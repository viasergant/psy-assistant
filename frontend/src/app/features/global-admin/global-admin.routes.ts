import { Routes } from '@angular/router';
import { GlobalAdminLayoutComponent } from './global-admin-layout.component';

export default [
  {
    path: '',
    component: GlobalAdminLayoutComponent,
    children: [
      {
        path: '',
        redirectTo: 'risk-flag-types',
        pathMatch: 'full'
      },
      {
        path: 'risk-flag-types',
        loadChildren: () => import('../admin/risk-flag-types/risk-flag-types.routes')
      },
      {
        path: 'contacts',
        loadComponent: () =>
          import('./contacts/global-contacts.component').then(m => m.GlobalContactsComponent)
      },
      {
        path: 'users',
        loadChildren: () => import('./users/global-users.routes')
      }
    ]
  }
] satisfies Routes;
