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
    path: 'calendar',
    canActivate: [scheduleGuard],
    loadComponent: () =>
      import('./components/calendar/calendar-shell/calendar-shell.component').then(
        m => m.CalendarShellComponent
      )
  }
] satisfies Routes;
