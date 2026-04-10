import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])]
    });
    authService = TestBed.inject(AuthService);
  });

  it('returns true when user is authenticated', () => {
    authService.setToken('fake-token');
    const mockState = { url: '/clients' } as RouterStateSnapshot;
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, mockState)
    );
    expect(result).toBeTrue();
  });

  it('redirects to /auth/login when unauthenticated', () => {
    const mockState = { url: '/clients' } as RouterStateSnapshot;
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, mockState)
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toContain('/auth/login');
  });

  it('includes returnUrl query param when redirecting', () => {
    const mockState = { url: '/clients/42/sessions' } as RouterStateSnapshot;
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, mockState)
    );
    expect(result).toBeInstanceOf(UrlTree);
    const serialized = (result as UrlTree).toString();
    expect(serialized).toContain('returnUrl=%2Fclients%2F42%2Fsessions');
  });
});
