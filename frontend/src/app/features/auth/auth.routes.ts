import { Routes } from '@angular/router';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `<h1>Auth — coming soon</h1>`
})
export class AuthPlaceholderComponent {}

@Component({
  standalone: true,
  template: `<h1>Login — coming soon</h1>`
})
export class LoginPlaceholderComponent {}

export default [
  { path: '', component: AuthPlaceholderComponent },
  { path: 'login', component: LoginPlaceholderComponent }
] satisfies Routes;
