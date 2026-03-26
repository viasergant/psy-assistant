import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Sessions — coming soon</h1>`
})
export class SessionsPlaceholderComponent {}

export default [
  { path: '', component: SessionsPlaceholderComponent }
] satisfies Routes;
