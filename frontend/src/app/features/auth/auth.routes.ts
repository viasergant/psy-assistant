import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';

export default [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent }
] satisfies Routes;
