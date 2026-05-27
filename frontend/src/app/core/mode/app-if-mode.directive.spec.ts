import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BehaviorSubject } from 'rxjs';
import { AppIfModeDirective } from './app-if-mode.directive';
import { AppMode, AppModeService } from './app-mode.service';

@Component({
  standalone: true,
  imports: [AppIfModeDirective],
  template: `
    <div id="solo-content" *appIfMode="'SOLO'">Solo content</div>
    <div id="clinic-content" *appIfMode="'CLINIC'">Clinic content</div>
  `
})
class TestHostComponent {}

function makeModeService(initialMode: AppMode | null): AppModeService {
  const modeSubject = new BehaviorSubject<AppMode | null>(initialMode);
  const stub = {
    mode$: modeSubject.asObservable(),
    get mode() { return modeSubject.getValue(); },
    _subject: modeSubject,
  } as unknown as AppModeService;
  return stub;
}

describe('AppIfModeDirective', () => {
  let modeService: AppModeService & { _subject: BehaviorSubject<AppMode | null> };

  describe('in SOLO mode', () => {
    beforeEach(() => {
      modeService = makeModeService('SOLO') as AppModeService & { _subject: BehaviorSubject<AppMode | null> };

      TestBed.configureTestingModule({
        imports: [TestHostComponent],
        providers: [{ provide: AppModeService, useValue: modeService }]
      });
    });

    it('renders SOLO-mode template when mode is SOLO', () => {
      const fixture = TestBed.createComponent(TestHostComponent);
      fixture.detectChanges();

      const soloEl = fixture.debugElement.query(By.css('#solo-content'));
      expect(soloEl).toBeTruthy();
    });

    it('hides CLINIC-mode template when mode is SOLO', () => {
      const fixture = TestBed.createComponent(TestHostComponent);
      fixture.detectChanges();

      const clinicEl = fixture.debugElement.query(By.css('#clinic-content'));
      expect(clinicEl).toBeNull();
    });
  });

  describe('in CLINIC mode', () => {
    beforeEach(() => {
      modeService = makeModeService('CLINIC') as AppModeService & { _subject: BehaviorSubject<AppMode | null> };

      TestBed.configureTestingModule({
        imports: [TestHostComponent],
        providers: [{ provide: AppModeService, useValue: modeService }]
      });
    });

    it('renders CLINIC-mode template when mode is CLINIC', () => {
      const fixture = TestBed.createComponent(TestHostComponent);
      fixture.detectChanges();

      const clinicEl = fixture.debugElement.query(By.css('#clinic-content'));
      expect(clinicEl).toBeTruthy();
    });

    it('hides SOLO-mode template when mode is CLINIC', () => {
      const fixture = TestBed.createComponent(TestHostComponent);
      fixture.detectChanges();

      const soloEl = fixture.debugElement.query(By.css('#solo-content'));
      expect(soloEl).toBeNull();
    });
  });

  describe('when mode is null (not yet initialised)', () => {
    beforeEach(() => {
      modeService = makeModeService(null) as AppModeService & { _subject: BehaviorSubject<AppMode | null> };

      TestBed.configureTestingModule({
        imports: [TestHostComponent],
        providers: [{ provide: AppModeService, useValue: modeService }]
      });
    });

    it('hides all mode-specific content when mode is null', () => {
      const fixture = TestBed.createComponent(TestHostComponent);
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('#solo-content'))).toBeNull();
      expect(fixture.debugElement.query(By.css('#clinic-content'))).toBeNull();
    });
  });

  describe('reactivity', () => {
    beforeEach(() => {
      modeService = makeModeService(null) as AppModeService & { _subject: BehaviorSubject<AppMode | null> };

      TestBed.configureTestingModule({
        imports: [TestHostComponent],
        providers: [{ provide: AppModeService, useValue: modeService }]
      });
    });

    it('shows content when mode$ emits a matching value', async () => {
      const fixture = TestBed.createComponent(TestHostComponent);
      fixture.detectChanges();

      // mode starts as null — nothing visible
      expect(fixture.debugElement.query(By.css('#solo-content'))).toBeNull();

      // mode$ emits SOLO
      modeService._subject.next('SOLO');
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('#solo-content'))).toBeTruthy();
    });

    it('hides content when mode$ emits a non-matching value', async () => {
      // start as SOLO
      modeService._subject.next('SOLO');
      const fixture = TestBed.createComponent(TestHostComponent);
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('#solo-content'))).toBeTruthy();

      // switch to CLINIC
      modeService._subject.next('CLINIC');
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.debugElement.query(By.css('#solo-content'))).toBeNull();
      expect(fixture.debugElement.query(By.css('#clinic-content'))).toBeTruthy();
    });
  });
});
