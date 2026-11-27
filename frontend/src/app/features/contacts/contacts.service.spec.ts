import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ContactsService } from './contacts.service';

describe('ContactsService', () => {
  let service: ContactsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ContactsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the public contacts', () => {
    const expected = { phone: '+1 555 0100', email: 'a@b.co', telegramChannels: ['https://t.me/psy_help'] };
    let result: unknown;

    service.getContacts().subscribe(c => (result = c));
    const req = httpMock.expectOne('/api/v1/contacts');
    req.flush(expected);

    expect(req.request.method).toBe('GET');
    expect(result).toEqual(expected);
  });
});
