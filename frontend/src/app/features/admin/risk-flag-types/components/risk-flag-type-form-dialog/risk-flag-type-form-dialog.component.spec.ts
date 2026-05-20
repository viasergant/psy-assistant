import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { RiskFlagTypeFormDialogComponent } from './risk-flag-type-form-dialog.component';
import { RiskFlagTypeAdminService } from '../../services/risk-flag-type-admin.service';
import { RiskFlagType } from '../../models/risk-flag-type-admin.model';

const CREATED_FLAG_TYPE: RiskFlagType = {
  id: 'type-uuid-new',
  name: 'Safeguarding Concern',
  displayOrder: 3,
  active: true,
};

describe('RiskFlagTypeFormDialogComponent', () => {
  let component: RiskFlagTypeFormDialogComponent;
  let fixture: ComponentFixture<RiskFlagTypeFormDialogComponent>;
  let svcSpy: jasmine.SpyObj<RiskFlagTypeAdminService>;

  beforeEach(async () => {
    svcSpy = jasmine.createSpyObj<RiskFlagTypeAdminService>('RiskFlagTypeAdminService', ['create']);

    await TestBed.configureTestingModule({
      imports: [RiskFlagTypeFormDialogComponent, TranslocoModule],
      providers: [{ provide: RiskFlagTypeAdminService, useValue: svcSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RiskFlagTypeFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // --- Initialisation ---

  it('shouldInitialiseFormWithEmptyNameAndZeroDisplayOrder_whenCreated', () => {
    // Assert
    expect(component.form.value.name).toBe('');
    expect(component.form.value.displayOrder).toBe(0);
  });

  it('shouldBeInvalidInitially_whenNameIsEmpty', () => {
    // Assert
    expect(component.form.invalid).toBe(true);
    expect(component.form.get('name')?.valid).toBe(false);
  });

  it('shouldBeValid_whenNameIsProvidedAndDisplayOrderIsNonNegative', () => {
    // Act
    component.form.patchValue({ name: 'Self-Harm Risk', displayOrder: 1 });

    // Assert
    expect(component.form.valid).toBe(true);
  });

  // --- Validation ---

  it('shouldBeInvalid_whenNameExceedsMaxLength', () => {
    // Act
    component.form.patchValue({ name: 'A'.repeat(101) });

    // Assert
    expect(component.form.get('name')?.errors?.['maxlength']).toBeTruthy();
  });

  it('shouldBeInvalid_whenDisplayOrderIsNegative', () => {
    // Act
    component.form.patchValue({ name: 'Valid Name', displayOrder: -1 });

    // Assert
    expect(component.form.get('displayOrder')?.errors?.['min']).toBeTruthy();
  });

  it('shouldBeValid_whenDisplayOrderIsZero', () => {
    // Act
    component.form.patchValue({ name: 'Valid Name', displayOrder: 0 });

    // Assert
    expect(component.form.get('displayOrder')?.valid).toBe(true);
  });

  // --- Submit behaviour ---

  it('shouldNotCallCreate_whenFormIsInvalid', () => {
    // Arrange — name is empty (invalid)
    component.form.patchValue({ name: '', displayOrder: 0 });

    // Act
    component.submit();

    // Assert
    expect(svcSpy.create).not.toHaveBeenCalled();
  });

  it('shouldMarkAllFieldsTouched_whenSubmitCalledWithInvalidForm', () => {
    // Arrange
    component.form.patchValue({ name: '', displayOrder: 0 });

    // Act
    component.submit();

    // Assert
    expect(component.form.get('name')?.touched).toBe(true);
    expect(component.form.get('displayOrder')?.touched).toBe(true);
  });

  it('shouldCallCreateWithCorrectArguments_whenFormIsValid', () => {
    // Arrange
    svcSpy.create.and.returnValue(of(CREATED_FLAG_TYPE));
    component.form.patchValue({ name: 'Safeguarding Concern', displayOrder: 3 });

    // Act
    component.submit();

    // Assert
    expect(svcSpy.create).toHaveBeenCalledOnceWith('Safeguarding Concern', 3);
  });

  it('shouldEmitSaved_whenCreateSucceeds', () => {
    // Arrange
    svcSpy.create.and.returnValue(of(CREATED_FLAG_TYPE));
    component.form.patchValue({ name: 'Safeguarding Concern', displayOrder: 3 });
    let savedEmitted = false;
    component.saved.subscribe(() => (savedEmitted = true));

    // Act
    component.submit();

    // Assert
    expect(savedEmitted).toBe(true);
  });

  it('shouldSetSaveError_whenCreateFails', () => {
    // Arrange
    svcSpy.create.and.returnValue(throwError(() => new Error('server error')));
    component.form.patchValue({ name: 'Safeguarding Concern', displayOrder: 3 });

    // Act
    component.submit();

    // Assert
    expect(component.saveError).toBeTruthy();
    expect(component.saving).toBe(false);
  });

  it('shouldNotEmitSaved_whenCreateFails', () => {
    // Arrange
    svcSpy.create.and.returnValue(throwError(() => new Error('server error')));
    component.form.patchValue({ name: 'Safeguarding Concern', displayOrder: 3 });
    let savedEmitted = false;
    component.saved.subscribe(() => (savedEmitted = true));

    // Act
    component.submit();

    // Assert
    expect(savedEmitted).toBe(false);
  });

  // --- Cancel ---

  it('shouldEmitCancelled_whenCancelIsCalled', () => {
    // Arrange
    let cancelEmitted = false;
    component.cancelled.subscribe(() => (cancelEmitted = true));

    // Act
    component.cancel();

    // Assert
    expect(cancelEmitted).toBe(true);
  });

  it('shouldEmitCancelled_whenCloseButtonIsClicked', () => {
    // Arrange
    let cancelEmitted = false;
    component.cancelled.subscribe(() => (cancelEmitted = true));

    // Act
    const closeBtn = fixture.debugElement.query(By.css('.dialog-close'));
    closeBtn.nativeElement.click();

    // Assert
    expect(cancelEmitted).toBe(true);
  });
});
