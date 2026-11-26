import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClientTelegramService } from './client-telegram.service';

describe('ClientTelegramService', () => {
  let service: ClientTelegramService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ClientTelegramService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the per-client endpoints and sends language and timezone with the invite', () => {
    service.status('c1').subscribe();
    http.expectOne({ method: 'GET', url: '/api/v1/clients/c1/telegram' }).flush({ available: true, linked: false });

    let url = '';
    service.createInvite('c1', 'uk', 'Europe/Kyiv').subscribe(r => (url = r.url));
    const req = http.expectOne(r => r.method === 'POST' && r.url === '/api/v1/clients/c1/telegram/link');
    expect(req.request.params.get('language')).toBe('uk');
    expect(req.request.params.get('timezone')).toBe('Europe/Kyiv');
    req.flush({ url: 'https://t.me/bot?start=x' });
    expect(url).toBe('https://t.me/bot?start=x');

    service.unlink('c1').subscribe();
    http.expectOne({ method: 'DELETE', url: '/api/v1/clients/c1/telegram' }).flush(null);
  });
});
