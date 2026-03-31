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
  }
] satisfies Routes;
