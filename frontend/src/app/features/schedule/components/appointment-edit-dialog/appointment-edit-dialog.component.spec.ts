import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { AppointmentEditDialogComponent } from './appointment-edit-dialog.component';
import { AppointmentApiService } from '../../services/appointment-api.service';
import { Appointment, AppointmentStatus } from '../../models/schedule.model';

function buildAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: 'appt-1',
    therapistProfileId: 'therapist-1',
    clientId: 'client-1',
    sessionType: { id: 'st-1', code: 'INDIVIDUAL', name: 'Individual' },
    startTime: '2026-06-01T10:00:00Z',
    endTime: '2026-06-01T11:00:00Z',
    durationMinutes: 60,
    timezone: 'UTC',
    status: AppointmentStatus.SCHEDULED,
    isConflictOverride: false,
    version: 1,
    createdAt: '2026-05-01T00:00:00Z',
    updatedAt: '2026-05-01T00:00:00Z',
    activeRiskFlagTypes: [],
    ...overrides,
  };
}

describe('AppointmentEditDialogComponent', () => {
  let component: AppointmentEditDialogComponent;
  let fixture: ComponentFixture<AppointmentEditDialogComponent>;
  let appointmentApiServiceSpy: jasmine.SpyObj<AppointmentApiService>;

  beforeEach(async () => {
    appointmentApiServiceSpy = jasmine.createSpyObj<AppointmentApiService>(
      'AppointmentApiService',
      ['updateAppointmentStatus']
    );

    await TestBed.configureTestingModule({
      imports: [AppointmentEditDialogComponent, TranslocoModule],
      providers: [
        { provide: AppointmentApiService, useValue: appointmentApiServiceSpy },
      ],
    }).compileComponents();
  });

  function createComponent(appointment: Appointment): void {
    fixture = TestBed.createComponent(AppointmentEditDialogComponent);
    component = fixture.componentInstance;
    component.appointment = appointment;
    fixture.detectChanges();
  }

  // --- Risk flag chips: rendering ---

  it('shouldNotRenderRiskFlagsSection_whenActiveRiskFlagTypesIsEmpty', () => {
    // Arrange
    const appointment = buildAppointment({ activeRiskFlagTypes: [] });

    // Act
    createComponent(appointment);

    // Assert
    const section = fixture.debugElement.query(By.css('.risk-flag-chips'));
    expect(section).toBeNull();
  });

  it('shouldNotRenderRiskFlagsSection_whenActiveRiskFlagTypesIsUndefined', () => {
    // Arrange
    const appointment = buildAppointment({ activeRiskFlagTypes: undefined as any });

    // Act
    createComponent(appointment);

    // Assert
    const section = fixture.debugElement.query(By.css('.risk-flag-chips'));
    expect(section).toBeNull();
  });

  it('shouldRenderRiskFlagsSection_whenActiveRiskFlagTypesIsNonEmpty', () => {
    // Arrange
    const appointment = buildAppointment({
      activeRiskFlagTypes: ['Self-Harm Risk', 'Crisis History'],
    });

    // Act
    createComponent(appointment);

    // Assert
    const section = fixture.debugElement.query(By.css('.risk-flag-chips'));
    expect(section).not.toBeNull();
  });

  it('shouldRenderOneChipPerFlagType_whenMultipleFlagsPresent', () => {
    // Arrange
    const flagTypes = ['Self-Harm Risk', 'Crisis History', 'Suicidal Ideation'];
    const appointment = buildAppointment({ activeRiskFlagTypes: flagTypes });

    // Act
    createComponent(appointment);

    // Assert
    const chips = fixture.debugElement.queryAll(By.css('.risk-flag-chip'));
    expect(chips.length).toBe(3);
  });

  it('shouldDisplayFlagTypeTextInChip_whenFlagTypeProvided', () => {
    // Arrange
    const appointment = buildAppointment({
      activeRiskFlagTypes: ['Safeguarding Concern'],
    });

    // Act
    createComponent(appointment);

    // Assert
    const chip = fixture.debugElement.query(By.css('.risk-flag-chip'));
    expect(chip.nativeElement.textContent.trim()).toBe('Safeguarding Concern');
  });

  it('shouldRenderAllFlagTypeLabels_whenMultipleFlagsPresent', () => {
    // Arrange
    const flagTypes = ['Self-Harm Risk', 'Crisis History'];
    const appointment = buildAppointment({ activeRiskFlagTypes: flagTypes });

    // Act
    createComponent(appointment);

    // Assert
    const chips = fixture.debugElement.queryAll(By.css('.risk-flag-chip'));
    const labels = chips.map((c) => c.nativeElement.textContent.trim());
    expect(labels).toEqual(flagTypes);
  });

  it('shouldRenderSingleChip_whenExactlyOneFlagTypePresent', () => {
    // Arrange
    const appointment = buildAppointment({
      activeRiskFlagTypes: ['Domestic Abuse Concern'],
    });

    // Act
    createComponent(appointment);

    // Assert
    const chips = fixture.debugElement.queryAll(By.css('.risk-flag-chip'));
    expect(chips.length).toBe(1);
    expect(chips[0].nativeElement.textContent.trim()).toBe('Domestic Abuse Concern');
  });

  // --- Existing dialog behaviour: unchanged ---

  it('shouldInitialiseSelectedStatus_whenComponentInitialises', () => {
    // Arrange
    const appointment = buildAppointment({ status: AppointmentStatus.CONFIRMED });

    // Act
    createComponent(appointment);

    // Assert
    expect(component.selectedStatus).toBe(AppointmentStatus.CONFIRMED);
  });

  it('shouldEmitCancelledEvent_whenOnCancelCalled', () => {
    // Arrange
    const appointment = buildAppointment();
    createComponent(appointment);
    const cancelledSpy = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelledSpy);

    // Act
    component.onCancel();

    // Assert
    expect(cancelledSpy).toHaveBeenCalledTimes(1);
  });

  it('shouldNotEmitCancelledEvent_whenIsSubmittingIsTrue', () => {
    // Arrange
    const appointment = buildAppointment();
    createComponent(appointment);
    component.isSubmitting = true;
    const cancelledSpy = jasmine.createSpy('cancelled');
    component.cancelled.subscribe(cancelledSpy);

    // Act
    component.onCancel();

    // Assert
    expect(cancelledSpy).not.toHaveBeenCalled();
  });

  it('shouldReturnFalseFromIsFormValid_whenStatusUnchanged', () => {
    // Arrange
    const appointment = buildAppointment({ status: AppointmentStatus.SCHEDULED });
    createComponent(appointment);
    component.selectedStatus = AppointmentStatus.SCHEDULED;

    // Act & Assert
    expect(component.isFormValid()).toBeFalse();
  });

  it('shouldReturnTrueFromIsFormValid_whenStatusChanged', () => {
    // Arrange
    const appointment = buildAppointment({ status: AppointmentStatus.SCHEDULED });
    createComponent(appointment);
    component.selectedStatus = AppointmentStatus.CONFIRMED;

    // Act & Assert
    expect(component.isFormValid()).toBeTrue();
  });

  it('shouldCallUpdateAppointmentStatus_whenOnSubmitCalledWithValidForm', () => {
    // Arrange
    const appointment = buildAppointment({ status: AppointmentStatus.SCHEDULED });
    const updatedAppointment = buildAppointment({ status: AppointmentStatus.CONFIRMED });
    appointmentApiServiceSpy.updateAppointmentStatus.and.returnValue(of(updatedAppointment));
    createComponent(appointment);
    component.selectedStatus = AppointmentStatus.CONFIRMED;

    // Act
    component.onSubmit();

    // Assert
    expect(appointmentApiServiceSpy.updateAppointmentStatus).toHaveBeenCalledWith(
      'appt-1',
      jasmine.objectContaining({ status: AppointmentStatus.CONFIRMED })
    );
  });
});
