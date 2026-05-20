import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { CreateUserDialogComponent } from './create-user-dialog.component';
import { UserManagementService } from '../../services/user-management.service';
import { UserCreationResponse } from '../../models/user.model';

describe('CreateUserDialogComponent', () => {
  let component: CreateUserDialogComponent;
  let fixture: ComponentFixture<CreateUserDialogComponent>;
  let userServiceSpy: jasmine.SpyObj<UserManagementService>;

  const mockResponse: UserCreationResponse = {
    id: 'uuid-1',
    email: 'finance@example.com',
    fullName: 'Finance User',
    role: 'FINANCE',
    roles: ['FINANCE'],
    temporaryPassword: 'Temp1234!'
  };

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserManagementService', ['createUser']);

    await TestBed.configureTestingModule({
      imports: [
        CreateUserDialogComponent,
        ReactiveFormsModule,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: UserManagementService, useValue: userServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateUserDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ---- initial state ------------------------------------------------------

  it('shouldInitialiseWithEmptyForm_whenCreated', () => {
    expect(component.form.get('email')?.value).toBe('');
    expect(component.form.get('fullName')?.value).toBe('');
    expect(component.form.get('roles')?.value).toEqual([]);
  });

  it('shouldBeInvalid_whenNoRolesSelected', () => {
    component.form.get('email')?.setValue('a@b.com');
    component.form.get('fullName')?.setValue('Alice');
    expect(component.form.invalid).toBeTrue();
  });

  // ---- isRoleSelected -----------------------------------------------------

  it('shouldReturnFalse_whenRoleNotInCurrentSelection', () => {
    expect(component.isRoleSelected('FINANCE')).toBeFalse();
  });

  it('shouldReturnTrue_whenRoleIsInCurrentSelection', () => {
    component.form.get('roles')?.setValue(['FINANCE']);
    expect(component.isRoleSelected('FINANCE')).toBeTrue();
  });

  // ---- toggleRole ---------------------------------------------------------

  it('shouldAddRole_whenCheckboxIsChecked', () => {
    const event = { target: { checked: true } } as unknown as Event;
    component.toggleRole('SUPERVISOR', event);
    expect(component.form.get('roles')?.value).toContain('SUPERVISOR');
  });

  it('shouldRemoveRole_whenCheckboxIsUnchecked', () => {
    component.form.get('roles')?.setValue(['SUPERVISOR', 'FINANCE']);
    const event = { target: { checked: false } } as unknown as Event;
    component.toggleRole('SUPERVISOR', event);
    expect(component.form.get('roles')?.value).not.toContain('SUPERVISOR');
    expect(component.form.get('roles')?.value).toContain('FINANCE');
  });

  it('shouldNotDuplicateRole_whenRoleAlreadySelected', () => {
    component.form.get('roles')?.setValue(['FINANCE']);
    const event = { target: { checked: true } } as unknown as Event;
    component.toggleRole('FINANCE', event);
    expect(component.form.get('roles')?.value).toEqual(['FINANCE']);
  });

  it('shouldMarkRolesControlAsTouched_whenToggled', () => {
    const event = { target: { checked: true } } as unknown as Event;
    component.toggleRole('FINANCE', event);
    expect(component.form.get('roles')?.touched).toBeTrue();
  });

  // ---- submit validation --------------------------------------------------

  it('shouldNotCallService_whenFormIsInvalid', () => {
    component.submit();
    expect(userServiceSpy.createUser).not.toHaveBeenCalled();
  });

  it('shouldEmitRedirectToTherapistWizard_whenTherapistRoleIsIncluded', () => {
    component.form.get('email')?.setValue('t@example.com');
    component.form.get('fullName')?.setValue('T User');
    component.form.get('roles')?.setValue(['THERAPIST']);

    const spy = jasmine.createSpy('redirectSpy');
    component.redirectToTherapistWizard.subscribe(spy);

    component.submit();

    expect(userServiceSpy.createUser).not.toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith({ fullName: 'T User', email: 't@example.com' });
  });

  it('shouldEmitRedirectToTherapistWizard_whenTherapistIsAmongMultipleRoles', () => {
    component.form.get('email')?.setValue('t@example.com');
    component.form.get('fullName')?.setValue('T User');
    component.form.get('roles')?.setValue(['THERAPIST', 'SUPERVISOR']);

    const spy = jasmine.createSpy('redirectSpy');
    component.redirectToTherapistWizard.subscribe(spy);

    component.submit();

    expect(userServiceSpy.createUser).not.toHaveBeenCalled();
    expect(spy).toHaveBeenCalled();
  });

  // ---- successful creation ------------------------------------------------

  it('shouldCallServiceWithRolesArray_whenFormIsValidAndNoTherapistRole', () => {
    userServiceSpy.createUser.and.returnValue(of(mockResponse));

    component.form.get('email')?.setValue('finance@example.com');
    component.form.get('fullName')?.setValue('Finance User');
    component.form.get('roles')?.setValue(['FINANCE']);

    component.submit();

    expect(userServiceSpy.createUser).toHaveBeenCalledWith({
      email: 'finance@example.com',
      fullName: 'Finance User',
      roles: ['FINANCE']
    });
  });

  it('shouldSetCreatedUser_whenServiceReturnsSuccess', () => {
    userServiceSpy.createUser.and.returnValue(of(mockResponse));

    component.form.get('email')?.setValue('finance@example.com');
    component.form.get('fullName')?.setValue('Finance User');
    component.form.get('roles')?.setValue(['FINANCE']);

    component.submit();

    expect(component.createdUser).toEqual(mockResponse);
    expect(component.saving).toBeFalse();
  });

  // ---- error handling -----------------------------------------------------

  it('shouldShowDuplicateEmailError_whenServerReturnsDuplicateEmailCode', () => {
    const errResponse = new HttpErrorResponse({ error: { code: 'DUPLICATE_EMAIL' }, status: 409 });
    userServiceSpy.createUser.and.returnValue(throwError(() => errResponse));

    component.form.get('email')?.setValue('dup@example.com');
    component.form.get('fullName')?.setValue('Dup User');
    component.form.get('roles')?.setValue(['FINANCE']);

    component.submit();

    expect(component.serverError).toContain('already registered');
    expect(component.saving).toBeFalse();
  });

  it('shouldShowGenericError_whenServerReturnsUnknownCode', () => {
    const errResponse = new HttpErrorResponse({ error: { code: 'UNKNOWN' }, status: 500 });
    userServiceSpy.createUser.and.returnValue(throwError(() => errResponse));

    component.form.get('email')?.setValue('u@example.com');
    component.form.get('fullName')?.setValue('User');
    component.form.get('roles')?.setValue(['FINANCE']);

    component.submit();

    expect(component.serverError).toBeTruthy();
    expect(component.saving).toBeFalse();
  });

  // ---- cancel / closeSuccess ----------------------------------------------

  it('shouldEmitCancelled_whenCancelIsCalled', () => {
    const spy = jasmine.createSpy('cancelSpy');
    component.cancelled.subscribe(spy);
    component.cancel();
    expect(spy).toHaveBeenCalled();
  });

  it('shouldEmitCreated_whenCloseSuccessIsCalled', () => {
    const spy = jasmine.createSpy('createdSpy');
    component.created.subscribe(spy);
    component.closeSuccess();
    expect(spy).toHaveBeenCalled();
  });
});
