import { Component, Pipe, PipeTransform } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { TranslocoPipe } from '@jsverse/transloco';
import { ShellComponent } from './shell.component';
import { AuthService } from '../../core/auth/auth.service';
import { PermissionService } from '../../core/auth/permission.service';
import { AppModeService, AppMode } from '../../core/mode/app-mode.service';
import { NAV_ITEMS } from '../../core/auth/permissions.config';
import { GlobalSearchComponent } from '../global-search/global-search.component';

/** Stub that replaces TranslocoPipe to avoid needing the full Transloco setup. */
@Pipe({ name: 'transloco', standalone: true })
class MockTranslocoPipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

/** Stub that replaces GlobalSearchComponent to remove its HttpClient dependency. */
@Component({ selector: 'app-global-search', standalone: true, template: '' })
class MockGlobalSearchComponent {}

function makeAuthService(): Pick<AuthService, 'logout'> {
  return { logout: jasmine.createSpy('logout') };
}

function makePermissionService(roles: string[]): Pick<PermissionService, 'hasAnyRole' | 'roles'> {
  return {
    roles: jasmine.createSpy('roles').and.returnValue(roles) as unknown as PermissionService['roles'],
    hasAnyRole: (required: string[]) => roles.some(r => required.includes(r)),
  };
}

function makeModeService(initialMode: AppMode | null): AppModeService & { _subject: BehaviorSubject<AppMode | null> } {
  const subject = new BehaviorSubject<AppMode | null>(initialMode);
  return {
    mode$: subject.asObservable(),
    get mode() { return subject.getValue(); },
    init: jasmine.createSpy('init').and.returnValue(Promise.resolve()),
    _subject: subject,
  } as unknown as AppModeService & { _subject: BehaviorSubject<AppMode | null> };
}

describe('ShellComponent.visibleNav', () => {
  const allRoles = ['SYSTEM_ADMINISTRATOR', 'THERAPIST', 'FINANCE', 'RECEPTION_ADMIN_STAFF', 'SUPERVISOR'];

  function buildComponent(mode: AppMode | null, roles = allRoles) {
    const modeService = makeModeService(mode);
    const permissionService = makePermissionService(roles);
    const authService = makeAuthService();

    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: PermissionService, useValue: permissionService },
        { provide: AppModeService, useValue: modeService },
      ],
    }).overrideComponent(ShellComponent, {
      remove: { imports: [TranslocoPipe, GlobalSearchComponent] },
      add: { imports: [MockTranslocoPipe, MockGlobalSearchComponent] },
    });

    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, modeService };
  }

  it('shows admin nav item when mode is CLINIC and user has SYSTEM_ADMINISTRATOR role', () => {
    const { component } = buildComponent('CLINIC');
    const nav = component.visibleNav();
    const adminItem = nav.find(item => item.route === '/admin');
    expect(adminItem).toBeTruthy();
  });

  it('hides admin nav item when mode is SOLO', () => {
    const { component } = buildComponent('SOLO');
    const nav = component.visibleNav();
    const adminItem = nav.find(item => item.route === '/admin');
    expect(adminItem).toBeUndefined();
  });

  it('hides admin nav item when mode is null (not yet initialised)', () => {
    const { component } = buildComponent(null);
    const nav = component.visibleNav();
    const adminItem = nav.find(item => item.route === '/admin');
    expect(adminItem).toBeUndefined();
  });

  it('shows all non-clinicOnly items in SOLO mode', () => {
    const { component } = buildComponent('SOLO');
    const nav = component.visibleNav();
    const clinicOnlyRoutes = NAV_ITEMS.filter(i => i.clinicOnly).map(i => i.route);
    const foundClinicOnly = nav.some(item => clinicOnlyRoutes.includes(item.route));
    expect(foundClinicOnly).toBeFalse();
  });

  it('shows all role-permitted items in CLINIC mode', () => {
    const { component } = buildComponent('CLINIC');
    const nav = component.visibleNav();
    // With all roles, all NAV_ITEMS should be visible in CLINIC mode
    expect(nav.length).toBe(NAV_ITEMS.length);
  });

  it('hides reports for a therapist in SOLO mode but shows it in CLINIC mode', () => {
    expect(buildComponent('SOLO', ['THERAPIST']).component.visibleNav()
      .some(i => i.route === '/reports')).toBeFalse();
    expect(buildComponent('CLINIC', ['THERAPIST']).component.visibleNav()
      .some(i => i.route === '/reports')).toBeTrue();
  });

  it('hides role-restricted items regardless of mode', () => {
    // Only FINANCE role — cannot see admin (SYSTEM_ADMINISTRATOR only)
    const { component } = buildComponent('CLINIC', ['FINANCE']);
    const nav = component.visibleNav();
    const adminItem = nav.find(item => item.route === '/admin');
    expect(adminItem).toBeUndefined();
  });

  it('reacts to mode$ change — hides clinicOnly items when mode switches to SOLO', async () => {
    const { fixture, component, modeService } = buildComponent('CLINIC');

    // Initially CLINIC — admin item visible
    expect(component.visibleNav().find(i => i.route === '/admin')).toBeTruthy();

    // Mode switches to SOLO
    modeService._subject.next('SOLO');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(component.visibleNav().find(i => i.route === '/admin')).toBeUndefined();
  });

  it('reacts to mode$ change — shows clinicOnly items when mode switches to CLINIC', async () => {
    const { fixture, component, modeService } = buildComponent('SOLO');

    // Initially SOLO — admin item hidden
    expect(component.visibleNav().find(i => i.route === '/admin')).toBeUndefined();

    // Mode switches to CLINIC
    modeService._subject.next('CLINIC');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(component.visibleNav().find(i => i.route === '/admin')).toBeTruthy();
  });
});
