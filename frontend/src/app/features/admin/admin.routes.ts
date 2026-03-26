import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Admin — coming soon</h1>`
})
export class AdminPlaceholderComponent {}

export default [
  { path: '', component: AdminPlaceholderComponent }
] satisfies Routes;
