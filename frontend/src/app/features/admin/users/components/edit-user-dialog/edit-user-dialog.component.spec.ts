import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { EditUserDialogComponent } from './edit-user-dialog.component';
import { UserManagementService } from '../../services/user-management.service';
import { UserSummary } from '../../models/user.model';

const makeSummary = (overrides: Partial<UserSummary> = {}): UserSummary => ({
  id: 'uuid-1',
  email: 'user@example.com',
  fullName: 'Test User',
  role: 'FINANCE',
  roles: ['FINANCE'],
  active: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  ...overrides
});

describe('EditUserDialogComponent', () => {
  let component: EditUserDialogComponent;
  let fixture: ComponentFixture<EditUserDialogComponent>;
  let userServiceSpy: jasmine.SpyObj<UserManagementService>;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserManagementService', ['updateUser']);

    await TestBed.configureTestingModule({
      imports: [
        EditUserDialogComponent,
        ReactiveFormsModule,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: UserManagementService, useValue: userServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EditUserDialogComponent);
    component = fixture.componentInstance;
    component.user = makeSummary();
    fixture.detectChanges();
  });

  // ---- pre-population -----------------------------------------------------

  it('shouldPrePopulateRolesFromUserRolesArray_whenRolesPresent', () => {
    expect(component.form.get('roles')?.value).toEqual(['FINANCE']);
  });

  it('shouldFallBackToNormalisedSingleRole_whenRolesArrayAbsent', async () => {
    component.user = makeSummary({ role: 'ADMIN', roles: undefined });
    component.ngOnInit();
    expect(component.form.get('roles')?.value).toEqual(['SYSTEM_ADMINISTRATOR']);
  });

  it('shouldPrePopulateMultipleRoles_whenUserHasMultipleRoles', () => {
    component.user = makeSummary({ roles: ['THERAPIST', 'SUPERVISOR'] });
    component.ngOnInit();
    expect(component.form.get('roles')?.value).toEqual(['THERAPIST', 'SUPERVISOR']);
  });

  it('shouldPrePopulateFullName_whenUserHasFullName', () => {
    expect(component.form.get('fullName')?.value).toBe('Test User');
  });

  it('shouldPrePopulateActiveStatus_whenUserIsActive', () => {
    expect(component.form.get('active')?.value).toBeTrue();
  });

  // ---- isRoleSelected -----------------------------------------------------

  it('shouldReturnTrue_whenRoleIsInCurrentSelection', () => {
    expect(component.isRoleSelected('FINANCE')).toBeTrue();
  });

  it('shouldReturnFalse_whenRoleIsNotInCurrentSelection', () => {
    expect(component.isRoleSelected('SUPERVISOR')).toBeFalse();
  });

  // ---- toggleRole ---------------------------------------------------------

  it('shouldAddRole_whenCheckboxIsChecked', () => {
    const event = { target: { checked: true } } as unknown as Event;
    component.toggleRole('SUPERVISOR', event);
    expect(component.form.get('roles')?.value).toContain('SUPERVISOR');
    expect(component.form.get('roles')?.value).toContain('FINANCE');
  });

  it('shouldRemoveRole_whenCheckboxIsUncheckedAndOtherRolesRemain', () => {
    component.form.get('roles')?.setValue(['FINANCE', 'SUPERVISOR']);
    const event = { target: { checked: false } } as unknown as Event;
    component.toggleRole('FINANCE', event);
    expect(component.form.get('roles')?.value).not.toContain('FINANCE');
    expect(component.form.get('roles')?.value).toContain('SUPERVISOR');
  });

  // ---- validation ---------------------------------------------------------

  it('shouldBeInvalid_whenAllRolesRemoved', () => {
    component.form.get('roles')?.setValue([]);
    component.form.get('roles')?.markAsTouched();
    expect(component.form.invalid).toBeTrue();
    expect(component.isInvalid('roles')).toBeTrue();
  });

  it('shouldNotSubmit_whenFormIsInvalid', () => {
    component.form.get('roles')?.setValue([]);
    component.form.get('roles')?.markAsTouched();
    component.submit();
    expect(userServiceSpy.updateUser).not.toHaveBeenCalled();
  });

  // ---- submit -------------------------------------------------------------

  it('shouldCallUpdateUserWithRolesArray_whenFormIsValid', () => {
    const updated = makeSummary({ roles: ['FINANCE', 'SUPERVISOR'] });
    userServiceSpy.updateUser.and.returnValue(of(updated));

    component.form.get('roles')?.setValue(['FINANCE', 'SUPERVISOR']);
    component.submit();

    expect(userServiceSpy.updateUser).toHaveBeenCalledWith('uuid-1', jasmine.objectContaining({
      roles: ['FINANCE', 'SUPERVISOR']
    }));
  });

  it('shouldEmitUpdated_whenServiceSucceeds', () => {
    const updated = makeSummary({ fullName: 'Updated User' });
    userServiceSpy.updateUser.and.returnValue(of(updated));

    const spy = jasmine.createSpy('updatedSpy');
    component.updated.subscribe(spy);

    component.submit();

    expect(spy).toHaveBeenCalledWith(updated);
    expect(component.saving).toBeFalse();
  });

  // ---- error handling -----------------------------------------------------

  it('shouldShowSelfDeactivationError_whenServerReturnsSelfDeactivationCode', () => {
    const errResponse = new HttpErrorResponse({ error: { code: 'SELF_DEACTIVATION_FORBIDDEN' }, status: 400 });
    userServiceSpy.updateUser.and.returnValue(throwError(() => errResponse));

    component.submit();

    expect(component.serverError).toContain('deactivate your own account');
    expect(component.saving).toBeFalse();
  });

  it('shouldShowNotFoundError_whenServerReturnsNotFoundCode', () => {
    const errResponse = new HttpErrorResponse({ error: { code: 'NOT_FOUND' }, status: 404 });
    userServiceSpy.updateUser.and.returnValue(throwError(() => errResponse));

    component.submit();

    expect(component.serverError).toContain('not found');
    expect(component.saving).toBeFalse();
  });

  it('shouldShowGenericError_whenServerReturnsUnknownCode', () => {
    const errResponse = new HttpErrorResponse({ error: { code: 'UNKNOWN' }, status: 500 });
    userServiceSpy.updateUser.and.returnValue(throwError(() => errResponse));

    component.submit();

    expect(component.serverError).toBeTruthy();
    expect(component.saving).toBeFalse();
  });

  // ---- cancel -------------------------------------------------------------

  it('shouldEmitCancelled_whenCancelIsCalled', () => {
    const spy = jasmine.createSpy('cancelSpy');
    component.cancelled.subscribe(spy);
    component.cancel();
    expect(spy).toHaveBeenCalled();
  });
});
