import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout.component';

export default [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
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
      },
      {
        path: 'therapists',
        loadChildren: () => import('./therapists/therapists.routes')
      },
      {
        path: 'leave-requests',
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
      }
    ]
  }
] satisfies Routes;
