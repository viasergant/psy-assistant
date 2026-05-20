import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { PermissionService } from './permission.service';
import { AuthService } from './auth.service';

/**
 * Builds a minimal unsigned JWT with the given payload for testing purposes.
 * The token is NOT cryptographically valid — it is only used to test the claim
 * decoding logic which does not verify signatures.
 */
function makeFakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).replace(/=/g, '');
  const body   = btoa(JSON.stringify(payload)).replace(/=/g, '');
  return `${header}.${body}.fake-sig`;
}

describe('PermissionService', () => {
  let service: PermissionService;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslocoTestingModule.forRoot({
          langs: { en: {} },
          translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
        })
      ],
      providers: [provideHttpClient(), provideRouter([])]
    });
    service     = TestBed.inject(PermissionService);
    authService = TestBed.inject(AuthService);
  });

  it('shouldReturnEmptyRoles_whenNoTokenIsSet', () => {
    expect(service.roles()).toEqual([]);
  });

  it('shouldReturnTherapistRole_whenJwtContainsRoleTherapist', () => {
    authService.setToken(makeFakeJwt({ sub: '1', roles: ['ROLE_THERAPIST', 'WRITE_SESSION_NOTE'], exp: 9999999999 }));
    expect(service.roles()).toEqual(['THERAPIST']);
  });

  it('shouldReturnSystemAdministratorRole_whenJwtContainsRoleSysAdmin', () => {
    authService.setToken(makeFakeJwt({ sub: '2', roles: ['ROLE_SYSTEM_ADMINISTRATOR', 'MANAGE_USERS'], exp: 9999999999 }));
    expect(service.roles()).toEqual(['SYSTEM_ADMINISTRATOR']);
  });

  it('shouldReturnEmptyRoles_whenTokenIsInvalid', () => {
    authService.setToken('not.a.valid.jwt');
    expect(service.roles()).toEqual([]);
  });

  it('shouldReturnAllRoles_whenJwtContainsMultipleRoleEntries', () => {
    authService.setToken(makeFakeJwt({
      sub: '3',
      roles: ['ROLE_THERAPIST', 'ROLE_SUPERVISOR', 'WRITE_SESSION_NOTE', 'READ_TEAM_WORKLOAD'],
      exp: 9999999999
    }));
    expect(service.roles()).toEqual(['THERAPIST', 'SUPERVISOR']);
  });

  it('shouldReturnEmptyRoles_whenJwtHasNoRoleXEntries', () => {
    authService.setToken(makeFakeJwt({ sub: '4', roles: ['WRITE_SESSION_NOTE'], exp: 9999999999 }));
    expect(service.roles()).toEqual([]);
  });

  it('shouldReturnEmptyRoles_whenJwtHasNoRolesClaim', () => {
    authService.setToken(makeFakeJwt({ sub: '5', exp: 9999999999 }));
    expect(service.roles()).toEqual([]);
  });

  it('shouldReturnTrue_whenHasAnyRoleAndUserRoleIsInAllowedList', () => {
    authService.setToken(makeFakeJwt({ sub: '1', roles: ['ROLE_THERAPIST'], exp: 9999999999 }));
    expect(service.hasAnyRole(['THERAPIST', 'SYSTEM_ADMINISTRATOR'])).toBeTrue();
  });

  it('shouldReturnFalse_whenHasAnyRoleAndUserRoleIsNotInAllowedList', () => {
    authService.setToken(makeFakeJwt({ sub: '1', roles: ['ROLE_FINANCE'], exp: 9999999999 }));
    expect(service.hasAnyRole(['THERAPIST', 'SYSTEM_ADMINISTRATOR'])).toBeFalse();
  });

  it('shouldReturnTrue_whenHasAnyRoleAndOneOfMultipleRolesMatchesAllowedList', () => {
    authService.setToken(makeFakeJwt({
      sub: '1',
      roles: ['ROLE_THERAPIST', 'ROLE_SUPERVISOR'],
      exp: 9999999999
    }));
    expect(service.hasAnyRole(['SUPERVISOR', 'FINANCE'])).toBeTrue();
  });

  it('shouldReturnTrue_whenHasPermissionViewSessionNotesAndUserIsTherapist', () => {
    authService.setToken(makeFakeJwt({ sub: '1', roles: ['ROLE_THERAPIST'], exp: 9999999999 }));
    expect(service.hasPermission('VIEW_SESSION_NOTES')).toBeTrue();
  });

  it('shouldReturnFalse_whenHasPermissionViewBillingActionsAndUserIsTherapist', () => {
    authService.setToken(makeFakeJwt({ sub: '1', roles: ['ROLE_THERAPIST'], exp: 9999999999 }));
    expect(service.hasPermission('VIEW_BILLING_ACTIONS')).toBeFalse();
  });

  it('shouldReturnTrue_whenHasPermissionViewBillingActionsAndUserIsFinance', () => {
    authService.setToken(makeFakeJwt({ sub: '2', roles: ['ROLE_FINANCE'], exp: 9999999999 }));
    expect(service.hasPermission('VIEW_BILLING_ACTIONS')).toBeTrue();
  });

  it('shouldReturnTrue_whenHasPermissionAllAndUserIsSysAdmin', () => {
    authService.setToken(makeFakeJwt({ sub: '3', roles: ['ROLE_SYSTEM_ADMINISTRATOR'], exp: 9999999999 }));
    expect(service.hasPermission('VIEW_BILLING_ACTIONS')).toBeTrue();
    expect(service.hasPermission('VIEW_SESSION_NOTES')).toBeTrue();
    expect(service.hasPermission('BOOK_APPOINTMENT')).toBeTrue();
    expect(service.hasPermission('MANAGE_USERS')).toBeTrue();
    expect(service.hasPermission('VIEW_REPORTS')).toBeTrue();
  });

  it('shouldGrantViewReportsPermission_whenUserHasTherapistAndSupervisorRoles', () => {
    authService.setToken(makeFakeJwt({
      sub: '4',
      roles: ['ROLE_THERAPIST', 'ROLE_SUPERVISOR'],
      exp: 9999999999
    }));
    expect(service.hasPermission('VIEW_REPORTS')).toBeTrue();
  });

  it('shouldDenyAdminRouteAccess_whenUserHasTherapistAndSupervisorRoles', () => {
    authService.setToken(makeFakeJwt({
      sub: '4',
      roles: ['ROLE_THERAPIST', 'ROLE_SUPERVISOR'],
      exp: 9999999999
    }));
    expect(service.hasAnyRole(['SYSTEM_ADMINISTRATOR'])).toBeFalse();
  });

  it('shouldUpdateRolesReactively_whenTokenChanges', () => {
    authService.setToken(makeFakeJwt({ sub: '1', roles: ['ROLE_THERAPIST'], exp: 9999999999 }));
    expect(service.roles()).toEqual(['THERAPIST']);

    authService.setToken(makeFakeJwt({ sub: '1', roles: ['ROLE_FINANCE'], exp: 9999999999 }));
    expect(service.roles()).toEqual(['FINANCE']);
  });
});
