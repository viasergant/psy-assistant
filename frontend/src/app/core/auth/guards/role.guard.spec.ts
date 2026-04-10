import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { roleGuard } from './role.guard';
import { AuthService } from '../auth.service';
import { MessageService } from 'primeng/api';
import { TranslocoService } from '@jsverse/transloco';

function makeFakeJwt(role: string): string {
  const body = btoa(JSON.stringify({ sub: '1', role, exp: 9999999999 })).replace(/=/g, '');
  return `header.${body}.sig`;
}

describe('roleGuard', () => {
  let authService: AuthService;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let translocoSpy: jasmine.SpyObj<TranslocoService>;

  beforeEach(() => {
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);
    translocoSpy      = jasmine.createSpyObj('TranslocoService', ['translate']);
    translocoSpy.translate.and.returnValue('Access restricted.');

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: MessageService,    useValue: messageServiceSpy },
        { provide: TranslocoService,  useValue: translocoSpy },
      ]
    });
    authService = TestBed.inject(AuthService);
  });

  it('allows access when user has the required role', () => {
    authService.setToken(makeFakeJwt('ADMIN'));
    const guard = roleGuard(['ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
    expect(messageServiceSpy.add).not.toHaveBeenCalled();
  });

  it('allows access when user role is one of several allowed roles', () => {
    authService.setToken(makeFakeJwt('THERAPIST'));
    const guard = roleGuard(['THERAPIST', 'ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });

  it('redirects to / when user lacks required role', () => {
    authService.setToken(makeFakeJwt('FINANCE'));
    const guard = roleGuard(['THERAPIST', 'ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('shows a toast when user is blocked', () => {
    authService.setToken(makeFakeJwt('FINANCE'));
    const guard = roleGuard(['ADMIN']);
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'warn' })
    );
  });

  it('redirects to / when there is no token', () => {
    // no token set — roles() returns []
    const guard = roleGuard(['ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeInstanceOf(UrlTree);
  });
});
