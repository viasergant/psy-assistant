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
      }
    ]
  }
] satisfies Routes;
