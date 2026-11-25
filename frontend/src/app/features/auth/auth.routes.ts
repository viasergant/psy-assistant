import { Routes } from '@angular/router';
import { modeGuard } from '../../core/mode/mode.guard';
import { LoginComponent } from './login/login.component';
import { FirstLoginPasswordChangeComponent } from './first-login-password-change/first-login-password-change.component';
import { RegisterComponent } from './registration/register.component';
import { ConfirmEmailComponent } from './registration/confirm-email.component';

export default [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'first-login-password-change', component: FirstLoginPasswordChangeComponent },
  { path: 'register', component: RegisterComponent, canActivate: [modeGuard('SAAS')] },
  { path: 'confirm-email', component: ConfirmEmailComponent, canActivate: [modeGuard('SAAS')] }
] satisfies Routes;
