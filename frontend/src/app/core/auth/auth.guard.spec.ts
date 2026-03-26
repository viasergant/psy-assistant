import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { RunHelpers, TestScheduler } from 'rxjs/testing';

describe('authGuard', () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])]
    });
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    localStorage.removeItem('access_token');
  });

  it('returns true when token is in localStorage', () => {
    localStorage.setItem('access_token', 'some-token');

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(result).toBeTrue();
  });

  it('returns a UrlTree redirecting to /auth/login when no token', () => {
    localStorage.removeItem('access_token');

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, {} as never)
    );

    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/auth/login');
  });
});
