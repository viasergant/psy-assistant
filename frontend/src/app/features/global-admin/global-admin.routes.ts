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
        path: 'users',
        loadChildren: () => import('./users/global-users.routes')
      }
    ]
  }
] satisfies Routes;
