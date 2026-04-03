import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { TranslocoModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { CancelSessionDialogComponent } from './cancel-session-dialog.component';
import { SessionService } from '../../services/session.service';
import { SessionRecord, SessionStatus } from '../../models/session.model';

describe('CancelSessionDialogComponent', () => {
  let component: CancelSessionDialogComponent;
  let fixture: ComponentFixture<CancelSessionDialogComponent>;
  let sessionService: jasmine.SpyObj<SessionService>;
  let messageService: jasmine.SpyObj<MessageService>;

  const mockSession: SessionRecord = {
    id: 1,
    appointmentId: 101,
    clientId: 201,
    clientName: 'John Doe',
    therapistId: 301,
    sessionDate: '2026-04-15',
    scheduledStartTime: '10:00:00',
    sessionType: {
      id: 'uuid-1',
      code: 'FOLLOW_UP',
      name: 'Follow-Up Session',
      description: 'Regular therapeutic session'
    },
    plannedDuration: 60,
    status: SessionStatus.PENDING,
    createdAt: '2026-04-01T08:00:00Z',
    updatedAt: '2026-04-01T08:00:00Z',
  };

  beforeEach(async () => {
    const sessionServiceSpy = jasmine.createSpyObj('SessionService', ['cancelSession']);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [
        CancelSessionDialogComponent,
        HttpClientTestingModule,
        ReactiveFormsModule,
        TranslocoModule,
      ],
      providers: [
        { provide: SessionService, useValue: sessionServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
      ],
    }).compileComponents();

    sessionService = TestBed.inject(SessionService) as jasmine.SpyObj<SessionService>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;

    fixture = TestBed.createComponent(CancelSessionDialogComponent);
    component = fixture.componentInstance;
    component.session = mockSession;
    component.visible = true;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should mark form as invalid when reason is empty', () => {
    component.cancelForm.patchValue({ reason: '' });

    expect(component.cancelForm.invalid).toBe(true);
  });

  it('should mark form as invalid when reason is too short', () => {
    component.cancelForm.patchValue({ reason: 'No' });

    expect(component.cancelForm.invalid).toBe(true);
  });

  it('should mark form as valid with sufficient reason', () => {
    component.cancelForm.patchValue({ reason: 'Client requested cancellation' });

    expect(component.cancelForm.valid).toBe(true);
  });

  it('should cancel session successfully', () => {
    const cancelledSession = { ...mockSession, status: SessionStatus.CANCELLED };
    sessionService.cancelSession.and.returnValue(of(cancelledSession));

    component.cancelForm.patchValue({ reason: 'Client requested cancellation' });

    spyOn(component.cancelled, 'emit');

    component.submit();

    expect(sessionService.cancelSession).toHaveBeenCalledWith(
      mockSession.id,
      'Client requested cancellation'
    );
    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'info' })
    );
    expect(component.cancelled.emit).toHaveBeenCalled();
  });

  it('should handle cancellation error', () => {
    sessionService.cancelSession.and.returnValue(
      throwError(() => new Error('Cancellation failed'))
    );

    component.cancelForm.patchValue({ reason: 'Valid cancellation reason' });

    component.submit();

    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
    expect(component.loading).toBe(false);
  });

  it('should not submit when form is invalid', () => {
    component.cancelForm.patchValue({ reason: '' });

    component.submit();

    expect(sessionService.cancelSession).not.toHaveBeenCalled();
  });

  it('should emit closed event when close is clicked', () => {
    spyOn(component.closed, 'emit');

    component.close();

    expect(component.closed.emit).toHaveBeenCalled();
  });

  it('should correctly determine if field is invalid', () => {
    const reasonControl = component.cancelForm.get('reason');
    reasonControl?.markAsTouched();
    reasonControl?.setValue('');

    expect(component.isInvalid('reason')).toBe(true);
  });

  it('should return reason length', () => {
    component.cancelForm.patchValue({ reason: 'Test reason' });

    expect(component.reasonLength).toBe(11);
  });

  it('should trim whitespace from reason before submission', () => {
    const cancelledSession = { ...mockSession, status: SessionStatus.CANCELLED };
    sessionService.cancelSession.and.returnValue(of(cancelledSession));

    component.cancelForm.patchValue({ reason: '  Trimmed reason  ' });

    component.submit();

    expect(sessionService.cancelSession).toHaveBeenCalledWith(
      mockSession.id,
      'Trimmed reason'
    );
  });
});
