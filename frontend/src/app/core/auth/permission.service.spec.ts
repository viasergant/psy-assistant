import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
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
      providers: [provideHttpClient(), provideRouter([])]
    });
    service     = TestBed.inject(PermissionService);
    authService = TestBed.inject(AuthService);
  });

  it('roles is empty when no token is set', () => {
    expect(service.roles()).toEqual([]);
  });

  it('roles returns [THERAPIST] for a THERAPIST JWT', () => {
    authService.setToken(makeFakeJwt({ sub: '1', role: 'THERAPIST', exp: 9999999999 }));
    expect(service.roles()).toEqual(['THERAPIST']);
  });

  it('roles returns [ADMIN] for an ADMIN JWT', () => {
    authService.setToken(makeFakeJwt({ sub: '2', role: 'ADMIN', exp: 9999999999 }));
    expect(service.roles()).toEqual(['ADMIN']);
  });

  it('roles is empty when token is invalid', () => {
    authService.setToken('not.a.valid.jwt');
    expect(service.roles()).toEqual([]);
  });

  it('hasAnyRole returns true when user role is in allowed list', () => {
    authService.setToken(makeFakeJwt({ sub: '1', role: 'THERAPIST', exp: 9999999999 }));
    expect(service.hasAnyRole(['THERAPIST', 'ADMIN'])).toBeTrue();
  });

  it('hasAnyRole returns false when user role is not in allowed list', () => {
    authService.setToken(makeFakeJwt({ sub: '1', role: 'FINANCE', exp: 9999999999 }));
    expect(service.hasAnyRole(['THERAPIST', 'ADMIN'])).toBeFalse();
  });

  it('hasPermission returns true for VIEW_SESSION_NOTES when THERAPIST', () => {
    authService.setToken(makeFakeJwt({ sub: '1', role: 'THERAPIST', exp: 9999999999 }));
    expect(service.hasPermission('VIEW_SESSION_NOTES')).toBeTrue();
  });

  it('hasPermission returns false for VIEW_BILLING_ACTIONS when THERAPIST', () => {
    authService.setToken(makeFakeJwt({ sub: '1', role: 'THERAPIST', exp: 9999999999 }));
    expect(service.hasPermission('VIEW_BILLING_ACTIONS')).toBeFalse();
  });

  it('hasPermission returns true for VIEW_BILLING_ACTIONS when FINANCE', () => {
    authService.setToken(makeFakeJwt({ sub: '2', role: 'FINANCE', exp: 9999999999 }));
    expect(service.hasPermission('VIEW_BILLING_ACTIONS')).toBeTrue();
  });

  it('hasPermission returns true for all permissions when ADMIN', () => {
    authService.setToken(makeFakeJwt({ sub: '3', role: 'ADMIN', exp: 9999999999 }));
    expect(service.hasPermission('VIEW_BILLING_ACTIONS')).toBeTrue();
    expect(service.hasPermission('VIEW_SESSION_NOTES')).toBeTrue();
    expect(service.hasPermission('BOOK_APPOINTMENT')).toBeTrue();
    expect(service.hasPermission('MANAGE_USERS')).toBeTrue();
    expect(service.hasPermission('VIEW_REPORTS')).toBeTrue();
  });

  it('roles updates reactively when token changes', () => {
    authService.setToken(makeFakeJwt({ sub: '1', role: 'THERAPIST', exp: 9999999999 }));
    expect(service.roles()).toEqual(['THERAPIST']);

    authService.setToken(makeFakeJwt({ sub: '1', role: 'FINANCE', exp: 9999999999 }));
    expect(service.roles()).toEqual(['FINANCE']);
  });
});
