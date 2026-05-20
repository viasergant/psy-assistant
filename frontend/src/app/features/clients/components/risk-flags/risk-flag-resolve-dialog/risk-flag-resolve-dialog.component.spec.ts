import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { RiskFlagResolveDialogComponent } from './risk-flag-resolve-dialog.component';
import { RiskFlagService } from '../services/risk-flag.service';
import { RiskFlag } from '../models/risk-flag.model';
import { TranslocoModule } from '@jsverse/transloco';

const MOCK_RESOLVED_FLAG: RiskFlag = {
  id: 'flag-1',
  clientId: 'client-1',
  flagTypeId: 'type-1',
  flagTypeName: 'Self-Harm Risk',
  status: 'RESOLVED',
  clinicalNote: null,
  reviewDate: '2026-06-01',
  createdByUserId: 'user-1',
  createdAt: '2026-05-13T10:00:00Z',
  resolvedByUserId: 'user-1',
  resolvedAt: '2026-05-13T11:00:00Z',
  resolutionNote: 'Resolved after follow-up',
};

describe('RiskFlagResolveDialogComponent', () => {
  let component: RiskFlagResolveDialogComponent;
  let fixture: ComponentFixture<RiskFlagResolveDialogComponent>;
  let riskFlagServiceSpy: jasmine.SpyObj<RiskFlagService>;

  beforeEach(async () => {
    riskFlagServiceSpy = jasmine.createSpyObj<RiskFlagService>('RiskFlagService', ['resolve']);

    await TestBed.configureTestingModule({
      imports: [RiskFlagResolveDialogComponent, ReactiveFormsModule, TranslocoModule],
      providers: [{ provide: RiskFlagService, useValue: riskFlagServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RiskFlagResolveDialogComponent);
    component = fixture.componentInstance;
    component.clientId = 'client-1';
    component.flagId = 'flag-1';
    fixture.detectChanges();
  });

  // --- Initialisation ---

  it('shouldInitialiseFormWithEmptyResolutionNote_whenComponentInitialises', () => {
    // Assert
    expect(component.form.get('resolutionNote')!.value).toBe('');
  });

  it('shouldBeInvalid_whenResolutionNoteIsEmpty', () => {
    // Assert
    expect(component.form.invalid).toBeTrue();
  });

  // --- Validation ---

  it('shouldBeValid_whenResolutionNoteIsProvided', () => {
    // Arrange
    component.form.patchValue({ resolutionNote: 'Discussed in session and safety plan agreed.' });
    // Assert
    expect(component.form.valid).toBeTrue();
  });

  // --- Submit: happy path ---

  it('shouldCallResolveAndEmitResolved_whenFormIsValidAndSubmitted', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.resolve.and.returnValue(of(MOCK_RESOLVED_FLAG));
    component.form.patchValue({ resolutionNote: 'Safety plan in place.' });
    const resolvedSpy = jasmine.createSpy('resolved');
    component.resolved.subscribe(resolvedSpy);

    // Act
    component.submit();
    tick();

    // Assert
    expect(riskFlagServiceSpy.resolve).toHaveBeenCalledOnceWith(
      'client-1',
      'flag-1',
      { resolutionNote: 'Safety plan in place.' }
    );
    expect(resolvedSpy).toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  }));

  it('shouldTrimResolutionNote_whenSubmittingWithSurroundingWhitespace', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.resolve.and.returnValue(of(MOCK_RESOLVED_FLAG));
    component.form.patchValue({ resolutionNote: '  trimmed note  ' });

    // Act
    component.submit();
    tick();

    // Assert
    const args = riskFlagServiceSpy.resolve.calls.mostRecent().args;
    expect(args[2].resolutionNote).toBe('trimmed note');
  }));

  // --- Submit: error path ---

  it('shouldSetSubmitError_whenResolveCallFails', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.resolve.and.returnValue(throwError(() => new Error('server error')));
    component.form.patchValue({ resolutionNote: 'Some note' });

    // Act
    component.submit();
    tick();

    // Assert
    expect(component.submitError).toBeTruthy();
    expect(component.saving).toBeFalse();
  }));

  it('shouldNotCallResolve_whenFormIsInvalid', () => {
    // Arrange — resolutionNote is empty / invalid
    // Act
    component.submit();
    // Assert
    expect(riskFlagServiceSpy.resolve).not.toHaveBeenCalled();
  });

  // --- Overlay click ---

  it('shouldEmitCancelled_whenOverlayBackdropIsClicked', () => {
    // Arrange
    const cancelledSpy = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelledSpy);
    const mockEvent = {
      target: { classList: { contains: (c: string) => c === 'dialog-overlay' } }
    } as unknown as MouseEvent;

    // Act
    component.onOverlayClick(mockEvent);

    // Assert
    expect(cancelledSpy).toHaveBeenCalled();
  });

  it('shouldNotEmitCancelled_whenInnerDialogElementIsClicked', () => {
    // Arrange
    const cancelledSpy = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelledSpy);
    const mockEvent = { target: { classList: { contains: () => false } } } as unknown as MouseEvent;

    // Act
    component.onOverlayClick(mockEvent);

    // Assert
    expect(cancelledSpy).not.toHaveBeenCalled();
  });
});
