import { inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { jwtDecode } from 'jwt-decode';
import { map } from 'rxjs';
import { Signal } from '@angular/core';
import { AuthService } from './auth.service';
import { JwtClaims } from './jwt-claims.model';
import { AppRole, PERMISSIONS, PermissionKey } from './permissions.config';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly authService = inject(AuthService);

  readonly roles: Signal<AppRole[]> = toSignal(
    this.authService.token$.pipe(
      map(token => {
        if (!token) return [];
        try {
          const claims = jwtDecode<JwtClaims>(token);
          // Backend emits roles as ["ROLE_THERAPIST", ...permissions]
          const roleEntry = claims.roles?.find(r => r.startsWith('ROLE_'));
          const role = roleEntry?.replace('ROLE_', '') as AppRole | undefined;
          return role ? [role] : [];
        } catch {
          return [];
        }
      })
    ),
    { initialValue: [] }
  );

  hasAnyRole(required: AppRole[]): boolean {
    return this.roles().some(r => required.includes(r));
  }

  hasPermission(key: PermissionKey): boolean {
    return this.hasAnyRole(PERMISSIONS[key]);
  }
}
