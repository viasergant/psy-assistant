import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { GlobalUserListComponent } from './global-user-list.component';
import { GlobalUserService } from '../../services/global-user.service';
import { GlobalUserPage } from '../../models/global-user.model';

describe('GlobalUserListComponent', () => {
  let fixture: ComponentFixture<GlobalUserListComponent>;
  let component: GlobalUserListComponent;
  let service: jasmine.SpyObj<GlobalUserService>;

  const page: GlobalUserPage = {
    content: [
      {
        id: 'u1', email: 'a@example.com', fullName: 'Ann', roles: ['THERAPIST'], active: true,
        createdAt: '2026-01-01T00:00:00Z', orgId: 'o1', orgName: 'Acme', orgType: 'SOLO',
        presence: 'ONLINE', blockReason: null, lastSeenAt: '2026-01-02T10:00:00Z',
      },
    ],
    totalElements: 1, totalPages: 1, page: 0, size: 20,
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj('GlobalUserService', ['listUsers', 'getStats', 'suspendUser', 'reactivateUser', 'deleteUser']);
    service.listUsers.and.returnValue(of(page));
    service.getStats.and.returnValue(of({ total: 10, online: 3, blocked: 2 }));
    await TestBed.configureTestingModule({
      imports: [GlobalUserListComponent, TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })],
      providers: [{ provide: GlobalUserService, useValue: service }],
    }).compileComponents();
    fixture = TestBed.createComponent(GlobalUserListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads users on lazy load and stores them', () => {
    component.onLazyLoad({ first: 0, rows: 20 });
    expect(service.listUsers).toHaveBeenCalledWith(
      jasmine.objectContaining({ page: 0, size: 20, sort: 'createdAt,desc' })
    );
    expect(component.users.length).toBe(1);
    expect(component.totalElements).toBe(1);
  });

  it('resets to first page and passes filters when applying filters', () => {
    component.first = 40;
    component.roleFilter = 'THERAPIST';
    component.statusFilter = false;
    component.search = 'ann';
    component.applyFilters();
    expect(component.first).toBe(0);
    expect(service.listUsers).toHaveBeenCalledWith(
      jasmine.objectContaining({ page: 0, role: 'THERAPIST', active: false, search: 'ann' })
    );
  });

  it('shows an error message when loading fails', () => {
    service.listUsers.and.returnValue(throwError(() => new Error('boom')));
    component.applyFilters();
    expect(component.loadError).toBeTruthy();
    expect(component.loading).toBeFalse();
  });

  describe('presence and stats', () => {
    it('loads the summary stats on init and renders them', () => {
      expect(service.getStats).toHaveBeenCalled();
      expect(component.stats).toEqual({ total: 10, online: 3, blocked: 2 });
      const text = (fixture.nativeElement as HTMLElement).querySelector('.stats')?.textContent ?? '';
      expect(text).toContain('10');
      expect(text).toContain('3');
      expect(text).toContain('2');
    });

    it('keeps the previous stats when a refresh fails', () => {
      service.getStats.and.returnValue(throwError(() => new Error('boom')));
      component['loadStats']();
      expect(component.stats).toEqual({ total: 10, online: 3, blocked: 2 });
    });

    it('renders a marker class per presence value', () => {
      for (const [presence, cls] of [['ONLINE', 'presence-online'], ['OFFLINE', 'presence-offline'], ['BLOCKED', 'presence-blocked']] as const) {
        component.users = [{ ...page.content[0], presence, blockReason: presence === 'BLOCKED' ? 'LOCKED' : null }];
        fixture.detectChanges();
        expect((fixture.nativeElement as HTMLElement).querySelector(`.presence.${cls}`)).withContext(presence).not.toBeNull();
      }
    });
  });

  describe('row actions', () => {
    const user = page.content[0];

    beforeEach(() => {
      service.listUsers.calls.reset();
      spyOn(window, 'confirm').and.returnValue(true);
    });

    it('suspends an active user and reloads the list', () => {
      service.suspendUser.and.returnValue(of(undefined));
      component.toggleSuspension(user);
      expect(service.suspendUser).toHaveBeenCalledWith('u1');
      expect(service.listUsers).toHaveBeenCalled();
      expect(service.getStats).toHaveBeenCalledTimes(2);
    });

    it('reactivates an inactive user', () => {
      service.reactivateUser.and.returnValue(of(undefined));
      component.toggleSuspension({ ...user, active: false });
      expect(service.reactivateUser).toHaveBeenCalledWith('u1');
    });

    it('does nothing when the confirmation is declined', () => {
      (window.confirm as jasmine.Spy).and.returnValue(false);
      component.toggleSuspension(user);
      component.remove(user);
      expect(service.suspendUser).not.toHaveBeenCalled();
      expect(service.deleteUser).not.toHaveBeenCalled();
    });

    it('deletes a user and reloads the list', () => {
      service.deleteUser.and.returnValue(of(undefined));
      component.remove(user);
      expect(service.deleteUser).toHaveBeenCalledWith('u1');
      expect(service.listUsers).toHaveBeenCalled();
    });

    it('shows the history error on 409 USER_HAS_HISTORY and does not reload', () => {
      service.deleteUser.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 409, error: { code: 'USER_HAS_HISTORY' } }))
      );
      component.remove(user);
      expect(component.actionError).toContain('hasHistory');
      expect(component.busyUserId).toBeNull();
      expect(service.listUsers).not.toHaveBeenCalled();
    });
  });
});
