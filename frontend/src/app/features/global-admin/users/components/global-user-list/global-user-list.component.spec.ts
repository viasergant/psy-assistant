import { ComponentFixture, TestBed } from '@angular/core/testing';
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
      },
    ],
    totalElements: 1, totalPages: 1, page: 0, size: 20,
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj('GlobalUserService', ['listUsers']);
    service.listUsers.and.returnValue(of(page));
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
});
