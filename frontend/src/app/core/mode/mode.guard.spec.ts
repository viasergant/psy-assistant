import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { modeGuard } from './mode.guard';
import { AppMode, AppModeService } from './app-mode.service';

function makeModeService(mode: AppMode | null): AppModeService {
  const subject = new BehaviorSubject<AppMode | null>(mode);
  return {
    mode$: subject.asObservable(),
    get mode() { return subject.getValue(); },
  } as unknown as AppModeService;
}

describe('modeGuard', () => {
  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = {} as RouterStateSnapshot;

  function runGuard(required: AppMode, currentMode: AppMode | null): boolean | UrlTree {
    const modeService = makeModeService(currentMode);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AppModeService, useValue: modeService }
      ]
    });
    const guard = modeGuard(required);
    return TestBed.runInInjectionContext(() => guard(mockRoute, mockState)) as boolean | UrlTree;
  }

  it('returns true when current mode matches the required CLINIC mode', () => {
    const result = runGuard('CLINIC', 'CLINIC');
    expect(result).toBeTrue();
  });

  it('returns true when current mode matches the required SOLO mode', () => {
    const result = runGuard('SOLO', 'SOLO');
    expect(result).toBeTrue();
  });

  it('returns a UrlTree redirecting to / when mode is SOLO and CLINIC is required', () => {
    const result = runGuard('CLINIC', 'SOLO');
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('returns a UrlTree redirecting to / when mode is CLINIC and SOLO is required', () => {
    const result = runGuard('SOLO', 'CLINIC');
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('returns a UrlTree redirecting to / when mode is null (uninitialised)', () => {
    const result = runGuard('CLINIC', null);
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });
});
