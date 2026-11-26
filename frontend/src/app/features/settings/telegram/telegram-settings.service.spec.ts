import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TelegramSettingsService } from './telegram-settings.service';

describe('TelegramSettingsService', () => {
  let service: TelegramSettingsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TelegramSettingsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('talks only to the self-service endpoints (no user id in the URL)', () => {
    let link = '';
    service.get().subscribe();
    http.expectOne({ method: 'GET', url: '/api/v1/telegram/settings' }).flush({});
    service.createLink().subscribe(r => (link = r.url));
    http.expectOne({ method: 'POST', url: '/api/v1/telegram/link' }).flush({ url: 'u' });
    expect(link).toBe('u');
    service.unlink().subscribe();
    http.expectOne({ method: 'DELETE', url: '/api/v1/telegram/link' }).flush(null);
    service.sendTest().subscribe();
    http.expectOne({ method: 'POST', url: '/api/v1/telegram/test' }).flush(null);
  });
});
