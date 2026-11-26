import { ComponentFixture, fakeAsync, TestBed, tick, discardPeriodicTasks } from '@angular/core/testing';
import { of } from 'rxjs';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { ClientTelegramComponent } from './client-telegram.component';
import { ClientTelegramService } from './client-telegram.service';

describe('ClientTelegramComponent', () => {
  let fixture: ComponentFixture<ClientTelegramComponent>;
  let component: ClientTelegramComponent;
  let service: jasmine.SpyObj<ClientTelegramService>;

  function create(status: { available: boolean; linked: boolean }): void {
    service.status.and.returnValue(of(status));
    fixture = TestBed.createComponent(ClientTelegramComponent);
    component = fixture.componentInstance;
    component.clientId = 'c1';
    component.ngOnChanges();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    service = jasmine.createSpyObj<ClientTelegramService>('ClientTelegramService', ['status', 'createInvite', 'unlink']);
    await TestBed.configureTestingModule({
      imports: [ClientTelegramComponent, TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })],
      providers: [{ provide: ClientTelegramService, useValue: service }]
    }).compileComponents();
  });

  it('shows the invite link after inviting, with the chosen language and the browser timezone', () => {
    create({ available: true, linked: false });
    service.createInvite.and.returnValue(of({ url: 'https://t.me/bot?start=tok' }));
    component.language = 'en';

    component.invite();
    fixture.detectChanges();

    expect(service.createInvite).toHaveBeenCalledWith('c1', 'en', Intl.DateTimeFormat().resolvedOptions().timeZone);
    expect((fixture.nativeElement.querySelector('input[readonly]') as HTMLInputElement).value)
      .toBe('https://t.me/bot?start=tok');
  });

  it('polls after inviting and shows "connected" once the client presses Start', fakeAsync(() => {
    create({ available: true, linked: false });
    service.createInvite.and.returnValue(of({ url: 'https://t.me/bot?start=tok' }));
    service.status.calls.reset();
    service.status.and.returnValues(of({ available: true, linked: false }), of({ available: true, linked: true }));

    component.invite();
    tick(3000);
    expect(component.status()?.linked).toBeFalse();
    expect(component.inviteUrl()).not.toBeNull();
    tick(3000);

    expect(component.status()?.linked).toBeTrue();
    expect(component.inviteUrl()).toBeNull();
    tick(30000);
    expect(service.status).toHaveBeenCalledTimes(2); // polling stopped
    discardPeriodicTasks();
  }));

  it('stops polling when the component is destroyed', fakeAsync(() => {
    create({ available: true, linked: false });
    service.createInvite.and.returnValue(of({ url: 'u' }));
    service.status.calls.reset();

    component.invite();
    fixture.destroy();
    tick(10000);

    expect(service.status).not.toHaveBeenCalled();
  }));

  it('hides the invite controls when Telegram is not configured', () => {
    create({ available: false, linked: false });

    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });

  it('disconnects a linked client and offers to invite again', () => {
    create({ available: true, linked: true });
    service.unlink.and.returnValue(of(undefined));

    component.disconnect();
    fixture.detectChanges();

    expect(service.unlink).toHaveBeenCalledWith('c1');
    expect(component.status()?.linked).toBeFalse();
    expect(fixture.nativeElement.querySelector('#client-tg-language')).not.toBeNull();
  });
});
