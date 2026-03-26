import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Reports — coming soon</h1>`
})
export class ReportsPlaceholderComponent {}

export default [
  { path: '', component: ReportsPlaceholderComponent }
] satisfies Routes;
