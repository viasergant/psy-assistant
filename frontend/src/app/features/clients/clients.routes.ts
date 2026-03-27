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
  }
] satisfies Routes;
