import { Routes } from '@angular/router';
import { scheduleGuard } from './guards/schedule.guard';

export default [
  {
    path: '',
    canActivate: [scheduleGuard],
    loadComponent: () =>
      import('./schedule-management.component').then(
        m => m.ScheduleManagementComponent
      )
  },
  {
    // Redirect legacy /schedule/calendar to /schedule (calendar is now integrated)
    path: 'calendar',
    redirectTo: '',
    pathMatch: 'full'
  }
] satisfies Routes;
