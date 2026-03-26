import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Billing — coming soon</h1>`
})
export class BillingPlaceholderComponent {}

export default [
  { path: '', component: BillingPlaceholderComponent }
] satisfies Routes;
