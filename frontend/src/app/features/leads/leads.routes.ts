import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Leads — coming soon</h1>`
})
export class LeadsPlaceholderComponent {}

export default [
  { path: '', component: LeadsPlaceholderComponent }
] satisfies Routes;
