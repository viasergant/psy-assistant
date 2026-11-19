import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout.component';
import { modeGuard } from '../../core/mode/mode.guard';

export default [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        redirectTo: 'catalog',
        pathMatch: 'full'
      },
      {
        path: 'users',
        canActivate: [modeGuard('CLINIC')],
        loadComponent: () =>
          import('./users/components/user-list/user-list.component').then(
            m => m.UserListComponent
          )
      },
      {
        path: 'therapists',
        canActivate: [modeGuard('CLINIC')],
        loadChildren: () => import('./therapists/therapists.routes')
      },
      {
        path: 'leave-requests',
        canActivate: [modeGuard('CLINIC')],
        loadComponent: () =>
          import('./leave/components/pending-leave-requests/pending-leave-requests.component').then(
            m => m.PendingLeaveRequestsComponent
          )
      },
      {
        path: 'catalog',
        loadComponent: () =>
          import('../billing/components/service-catalog-list/service-catalog-list.component').then(
            m => m.ServiceCatalogListComponent
          )
      },
      {
        path: 'catalog/:id',
        loadComponent: () =>
          import('../billing/components/service-catalog-detail/service-catalog-detail.component').then(
            m => m.ServiceCatalogDetailComponent
          )
      },
      {
        path: 'notification-templates',
        loadChildren: () => import('./notification-templates/notification-templates.routes')
      },
    ]
  }
] satisfies Routes;
