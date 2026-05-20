import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { DefaultTranspiler, TRANSLOCO_TRANSPILER, TranslocoService } from '@jsverse/transloco';
import { roleGuard } from './role.guard';
import { AuthService } from '../auth.service';
import { MessageService } from 'primeng/api';
import { AppRole } from '../permissions.config';

function makeFakeJwt(roles: string[]): string {
  const prefixed = roles.map(r => 'ROLE_' + r);
  const body = btoa(JSON.stringify({ sub: '1', roles: prefixed, exp: 9999999999 })).replace(/=/g, '');
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
        { provide: MessageService,         useValue: messageServiceSpy },
        { provide: TranslocoService,       useValue: translocoSpy },
        { provide: TRANSLOCO_TRANSPILER,   useClass: DefaultTranspiler },
      ]
    });
    authService = TestBed.inject(AuthService);
  });

  it('allows access when user has the required role', () => {
    authService.setToken(makeFakeJwt(['SYSTEM_ADMINISTRATOR']));
    const guard = roleGuard(['SYSTEM_ADMINISTRATOR'] as AppRole[]);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
    expect(messageServiceSpy.add).not.toHaveBeenCalled();
  });

  it('allows access when user role is one of several allowed roles', () => {
    authService.setToken(makeFakeJwt(['THERAPIST']));
    const guard = roleGuard(['THERAPIST', 'SYSTEM_ADMINISTRATOR'] as AppRole[]);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
  });

  it('redirects to / when user lacks required role', () => {
    authService.setToken(makeFakeJwt(['FINANCE']));
    const guard = roleGuard(['THERAPIST', 'SYSTEM_ADMINISTRATOR'] as AppRole[]);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('shows a toast when user is blocked', () => {
    authService.setToken(makeFakeJwt(['FINANCE']));
    const guard = roleGuard(['SYSTEM_ADMINISTRATOR'] as AppRole[]);
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'warn' })
    );
  });

  it('redirects to / when there is no token', () => {
    // no token set — roles() returns []
    const guard = roleGuard(['SYSTEM_ADMINISTRATOR'] as AppRole[]);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeInstanceOf(UrlTree);
  });

  it('should grant access when user has [THERAPIST, SUPERVISOR] and route requires [SUPERVISOR]', () => {
    authService.setToken(makeFakeJwt(['THERAPIST', 'SUPERVISOR']));
    const guard = roleGuard(['SUPERVISOR'] as AppRole[]);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeTrue();
    expect(messageServiceSpy.add).not.toHaveBeenCalled();
  });

  it('should deny access when user has [THERAPIST, SUPERVISOR] and route requires [SYSTEM_ADMINISTRATOR]', () => {
    authService.setToken(makeFakeJwt(['THERAPIST', 'SUPERVISOR']));
    const guard = roleGuard(['SYSTEM_ADMINISTRATOR'] as AppRole[]);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
    expect(result).toBeInstanceOf(UrlTree);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'warn' })
    );
  });
});
