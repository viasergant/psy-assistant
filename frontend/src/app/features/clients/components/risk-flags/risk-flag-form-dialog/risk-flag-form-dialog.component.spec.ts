import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { RiskFlagFormDialogComponent } from './risk-flag-form-dialog.component';
import { RiskFlagService } from '../services/risk-flag.service';
import { RiskFlag, RiskFlagType } from '../models/risk-flag.model';
import { TranslocoModule } from '@jsverse/transloco';

const MOCK_TYPES: RiskFlagType[] = [
  { id: 'type-1', name: 'Self-Harm Risk', displayOrder: 1, active: true },
  { id: 'type-2', name: 'Suicidal Ideation', displayOrder: 2, active: true },
];

const MOCK_FLAG: RiskFlag = {
  id: 'flag-1',
  clientId: 'client-1',
  flagTypeId: 'type-1',
  flagTypeName: 'Self-Harm Risk',
  status: 'ACTIVE',
  clinicalNote: null,
  reviewDate: '2026-06-01',
  createdByUserId: 'user-1',
  createdAt: '2026-05-13T10:00:00Z',
  resolvedByUserId: null,
  resolvedAt: null,
  resolutionNote: null,
};

describe('RiskFlagFormDialogComponent', () => {
  let component: RiskFlagFormDialogComponent;
  let fixture: ComponentFixture<RiskFlagFormDialogComponent>;
  let riskFlagServiceSpy: jasmine.SpyObj<RiskFlagService>;

  beforeEach(async () => {
    riskFlagServiceSpy = jasmine.createSpyObj<RiskFlagService>('RiskFlagService', ['listTypes', 'create']);
    riskFlagServiceSpy.listTypes.and.returnValue(of(MOCK_TYPES));

    await TestBed.configureTestingModule({
      imports: [RiskFlagFormDialogComponent, ReactiveFormsModule, TranslocoModule],
      providers: [{ provide: RiskFlagService, useValue: riskFlagServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RiskFlagFormDialogComponent);
    component = fixture.componentInstance;
    component.clientId = 'client-1';
    fixture.detectChanges();
  });

  // --- Initialisation ---

  it('shouldLoadFlagTypesOnInit_whenComponentInitialises', () => {
    // Assert
    expect(riskFlagServiceSpy.listTypes).toHaveBeenCalledOnceWith();
    expect(component.flagTypes).toEqual(MOCK_TYPES);
  });

  it('shouldInitialiseFormWithEmptyValues_whenComponentInitialises', () => {
    // Assert
    expect(component.form.get('flagTypeId')!.value).toBe('');
    expect(component.form.get('reviewDate')!.value).toBe('');
    expect(component.form.get('clinicalNote')!.value).toBe('');
  });

  it('shouldSetTodayAsMinDate_whenComponentInitialises', () => {
    // Assert
    const expected = new Date().toISOString().split('T')[0];
    expect(component.today).toBe(expected);
  });

  // --- Validation ---

  it('shouldBeInvalid_whenBothRequiredFieldsAreEmpty', () => {
    // Assert
    expect(component.form.invalid).toBeTrue();
  });

  it('shouldBeInvalid_whenFlagTypeIdIsMissing', () => {
    // Arrange
    component.form.patchValue({ flagTypeId: '', reviewDate: '2026-06-01' });
    // Assert
    expect(component.form.get('flagTypeId')!.invalid).toBeTrue();
  });

  it('shouldBeInvalid_whenReviewDateIsMissing', () => {
    // Arrange
    component.form.patchValue({ flagTypeId: 'type-1', reviewDate: '' });
    // Assert
    expect(component.form.get('reviewDate')!.invalid).toBeTrue();
  });

  it('shouldBeValid_whenRequiredFieldsAreProvided', () => {
    // Arrange
    component.form.patchValue({ flagTypeId: 'type-1', reviewDate: '2026-06-01' });
    // Assert
    expect(component.form.valid).toBeTrue();
  });

  // --- Submit: happy path ---

  it('shouldCallCreateAndEmitSaved_whenFormIsValidAndSubmitted', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.create.and.returnValue(of(MOCK_FLAG));
    component.form.patchValue({ flagTypeId: 'type-1', reviewDate: '2026-06-01', clinicalNote: 'note' });
    const savedSpy = jasmine.createSpy('saved');
    component.saved.subscribe(savedSpy);

    // Act
    component.submit();
    tick();

    // Assert
    expect(riskFlagServiceSpy.create).toHaveBeenCalledOnceWith('client-1', {
      flagTypeId: 'type-1',
      reviewDate: '2026-06-01',
      clinicalNote: 'note',
    });
    expect(savedSpy).toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  }));

  it('shouldCoerceClinicalNoteToNull_whenNoteIsBlankOnSubmit', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.create.and.returnValue(of(MOCK_FLAG));
    component.form.patchValue({ flagTypeId: 'type-1', reviewDate: '2026-06-01', clinicalNote: '   ' });

    // Act
    component.submit();
    tick();

    // Assert
    const payload = riskFlagServiceSpy.create.calls.mostRecent().args[1];
    expect(payload.clinicalNote).toBeNull();
  }));

  // --- Submit: error path ---

  it('shouldSetSubmitError_whenCreateCallFails', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.create.and.returnValue(throwError(() => new Error('server error')));
    component.form.patchValue({ flagTypeId: 'type-1', reviewDate: '2026-06-01' });

    // Act
    component.submit();
    tick();

    // Assert
    expect(component.submitError).toBeTruthy();
    expect(component.saving).toBeFalse();
  }));

  it('shouldNotCallCreate_whenFormIsInvalid', () => {
    // Arrange — form is empty / invalid
    // Act
    component.submit();
    // Assert
    expect(riskFlagServiceSpy.create).not.toHaveBeenCalled();
  });

  // --- Overlay click ---

  it('shouldEmitCancelled_whenOverlayBackdropIsClicked', () => {
    // Arrange
    const cancelledSpy = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelledSpy);
    const mockEvent = { target: { classList: { contains: (c: string) => c === 'dialog-overlay' } } } as unknown as MouseEvent;

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
