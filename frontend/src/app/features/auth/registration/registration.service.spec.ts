import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RegistrationService } from './registration.service';

describe('RegistrationService', () => {
  let service: RegistrationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(RegistrationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts registration data', () => {
    const payload = { orgName: 'Org', ownerEmail: 'a@b.co', password: 'Passw0rd!' };
    service.register(payload).subscribe();
    const req = http.expectOne('/api/v1/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(null, { status: 202, statusText: 'Accepted' });
  });

  it('posts the confirmation token', () => {
    service.confirm('tok').subscribe();
    const req = http.expectOne('/api/v1/register/confirm');
    expect(req.request.body).toEqual({ token: 'tok' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('posts a resend request', () => {
    service.resend('a@b.co', 'cap').subscribe();
    const req = http.expectOne('/api/v1/register/resend');
    expect(req.request.body).toEqual({ email: 'a@b.co', captchaToken: 'cap' });
    req.flush(null, { status: 202, statusText: 'Accepted' });
  });
});
