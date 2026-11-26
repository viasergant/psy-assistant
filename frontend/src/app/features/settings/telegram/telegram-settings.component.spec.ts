import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { TelegramSettings } from './telegram-settings.service';
import { TelegramSettingsComponent } from './telegram-settings.component';

const BASE: TelegramSettings = {
  available: true, linked: true, linkedAt: '2026-11-26T10:00:00Z', timezone: 'Europe/Kyiv', language: 'en',
  digestEnabled: true, digestTime: '07:30', digestDays: 127, skipEmptyDigest: true,
  reminderEnabled: true, reminderMinutesBefore: 60, includeClientNames: false
};

describe('TelegramSettingsComponent', () => {
  let fixture: ComponentFixture<TelegramSettingsComponent>;
  let http: HttpTestingController;

  function create(settings: TelegramSettings): TelegramSettingsComponent {
    fixture = TestBed.createComponent(TelegramSettingsComponent);
    fixture.detectChanges();
    http.expectOne('/api/v1/telegram/settings').flush(settings);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        TelegramSettingsComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('hides the form and the connect button when Telegram is not configured', () => {
    create({ ...BASE, available: false, linked: false });
    expect(fixture.nativeElement.querySelector('.btn-primary')).toBeNull();
    expect(fixture.nativeElement.querySelector('fieldset')).toBeNull();
  });

  it('maps digest days to the Monday-first bit mask', () => {
    const c = create({ ...BASE, digestDays: 0b0000011 });
    expect(c.isDayOn(0)).toBeTrue();
    expect(c.isDayOn(1)).toBeTrue();
    expect(c.isDayOn(2)).toBeFalse();
    c.toggleDay(2);
    expect(c.form?.digestDays).toBe(0b0000111);
    c.toggleDay(0);
    expect(c.form?.digestDays).toBe(0b0000110);
  });

  it('saves only editable preferences, never link state', () => {
    const c = create(BASE);
    c.form!.reminderMinutesBefore = 30;
    c.save();
    const req = http.expectOne({ method: 'PUT', url: '/api/v1/telegram/settings' });
    expect(req.request.body).toEqual(jasmine.objectContaining({ reminderMinutesBefore: 30 }));
    expect(req.request.body.linked).toBeUndefined();
    expect(req.request.body.available).toBeUndefined();
    req.flush({ ...BASE, reminderMinutesBefore: 30 });
    expect(c.saved()).toBeTrue();
  });

  it('rejects a reminder lead time below 5 minutes without calling the API', () => {
    const c = create(BASE);
    c.form!.reminderEnabled = true;
    c.form!.reminderMinutesBefore = 3;
    c.save();
    http.expectNone('/api/v1/telegram/settings');
    expect(c.error()).toBe('settings.telegram.errors.reminderRange');
    expect(c.saved()).toBeFalse();
  });

  it('opens the deep link and waits for the bot to confirm', () => {
    const open = spyOn(window, 'open');
    const c = create({ ...BASE, linked: false, timezone: 'Europe/Kyiv' });
    c.connect();
    http.expectOne({ method: 'POST', url: '/api/v1/telegram/link' }).flush({ url: 'https://t.me/bot?start=abc' });
    expect(open).toHaveBeenCalledWith('https://t.me/bot?start=abc', '_blank', 'noopener');
    expect(c.waitingForLink()).toBeTrue();
    c.ngOnDestroy();
  });

  it('prefills the browser timezone when the server still has the UTC default', () => {
    spyOn(Intl, 'DateTimeFormat').and.returnValue(
      { resolvedOptions: () => ({ timeZone: 'Europe/Kyiv' }) } as unknown as Intl.DateTimeFormat);
    const c = create({ ...BASE, linked: false, timezone: 'UTC' });
    expect(c.form?.timezone).toBe('Europe/Kyiv');
  });
});
