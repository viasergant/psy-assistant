import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])]
    });
    service = TestBed.inject(AuthService);
  });

  it('returns null when no token is set', () => {
    expect(service.token).toBeNull();
  });

  it('isAuthenticated returns false when no token', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('isAuthenticated returns true when token is set', () => {
    service.setToken('my-jwt');
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('setToken updates in-memory token', () => {
    service.setToken('test-token');
    expect(service.token).toBe('test-token');
  });

  it('token$ emits updated token value', () => {
    let latestToken: string | null = null;
    const sub = service.token$.subscribe((token) => {
      latestToken = token;
    });

    service.setToken('new-token');

    expect(latestToken).not.toBeNull();
    expect(service.token).toBe('new-token');
    sub.unsubscribe();
  });
});
