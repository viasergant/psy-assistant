import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { FirstLoginPasswordChangeComponent } from './first-login-password-change/first-login-password-change.component';

export default [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'first-login-password-change', component: FirstLoginPasswordChangeComponent }
] satisfies Routes;
