import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Clients — coming soon</h1>`
})
export class ClientsPlaceholderComponent {}

export default [
  { path: '', component: ClientsPlaceholderComponent },
  {
    path: ':id',
    loadComponent: () =>
      import('./client-detail/client-detail.component').then(
        m => m.ClientDetailComponent
      )
  },
  {
    path: ':clientId/care-plans',
    loadChildren: () =>
      import('./care-plans/care-plans.routes').then(m => m.default)
  }
] satisfies Routes;
