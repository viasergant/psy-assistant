import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])]
    });
    service = TestBed.inject(AuthService);
    localStorage.removeItem('access_token');
  });

  afterEach(() => {
    localStorage.removeItem('access_token');
  });

  it('returns null when no token is stored', () => {
    expect(service.token).toBeNull();
  });

  it('isAuthenticated returns false when no token', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('isAuthenticated returns true when token is stored', () => {
    service.setToken('my-jwt');
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('setToken stores the token in localStorage', () => {
    service.setToken('test-token');
    expect(localStorage.getItem('access_token')).toBe('test-token');
  });

  it('clearToken removes the token from localStorage', () => {
    service.setToken('test-token');
    service.clearToken();
    expect(localStorage.getItem('access_token')).toBeNull();
  });
});
