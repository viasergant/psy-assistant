import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Schedule — coming soon</h1>`
})
export class SchedulePlaceholderComponent {}

export default [
  { path: '', component: SchedulePlaceholderComponent }
] satisfies Routes;
