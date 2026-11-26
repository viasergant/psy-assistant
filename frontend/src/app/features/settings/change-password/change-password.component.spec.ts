import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { ChangePasswordComponent } from './change-password.component';

describe('ChangePasswordComponent', () => {
  let fixture: ComponentFixture<ChangePasswordComponent>;
  let component: ChangePasswordComponent;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        ChangePasswordComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ChangePasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function fill(current: string, next: string, confirm: string): void {
    component.form.setValue({ currentPassword: current, newPassword: next, confirmPassword: confirm });
  }

  it('does not call the API when the form is invalid', () => {
    fill('', '', '');
    component.submit();
    http.expectNone('/api/v1/users/me/password');
  });

  it('rejects mismatching confirmation', () => {
    fill('OldPass1!', 'NewPass1!', 'Different1!');
    expect(component.form.hasError('passwordMismatch')).toBeTrue();
    component.submit();
    http.expectNone('/api/v1/users/me/password');
  });

  it('posts current and new password only, then resets the form', () => {
    fill('OldPass1!', 'NewPass1!', 'NewPass1!');
    component.submit();
    const req = http.expectOne({ method: 'POST', url: '/api/v1/users/me/password' });
    expect(req.request.body).toEqual({ currentPassword: 'OldPass1!', newPassword: 'NewPass1!' });
    req.flush({ accessToken: 'jwt', expiresAt: '2026-11-26T12:00:00Z', tokenType: 'Bearer', language: 'en' });
    expect(component.saved()).toBeTrue();
    expect(component.form.getRawValue().newPassword).toBe('');
    expect(component.saving()).toBeFalse();
  });

  it('maps backend error codes to translation keys', () => {
    fill('wrong', 'NewPass1!', 'NewPass1!');
    component.submit();
    http.expectOne('/api/v1/users/me/password')
      .flush({ code: 'INVALID_CURRENT_PASSWORD' }, { status: 400, statusText: 'Bad Request' });
    expect(component.errorKey()).toBe('settings.password.errors.invalidCurrent');
    expect(component.saved()).toBeFalse();
  });

  it('falls back to a generic error for unknown failures', () => {
    fill('OldPass1!', 'NewPass1!', 'NewPass1!');
    component.submit();
    http.expectOne('/api/v1/users/me/password').flush({}, { status: 500, statusText: 'Server Error' });
    expect(component.errorKey()).toBe('settings.password.errors.generic');
  });
});
