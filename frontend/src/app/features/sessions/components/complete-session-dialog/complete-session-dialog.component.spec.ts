import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { TranslocoModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { CompleteSessionDialogComponent } from './complete-session-dialog.component';
import { SessionService } from '../../services/session.service';
import { SessionRecord, SessionStatus } from '../../models/session.model';

describe('CompleteSessionDialogComponent', () => {
  let component: CompleteSessionDialogComponent;
  let fixture: ComponentFixture<CompleteSessionDialogComponent>;
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
    status: SessionStatus.IN_PROGRESS,
    createdAt: '2026-04-01T08:00:00Z',
    updatedAt: '2026-04-01T08:00:00Z',
  };

  beforeEach(async () => {
    const sessionServiceSpy = jasmine.createSpyObj('SessionService', ['completeSession']);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [
        CompleteSessionDialogComponent,
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

    fixture = TestBed.createComponent(CompleteSessionDialogComponent);
    component = fixture.componentInstance;
    component.session = mockSession;
    component.visible = true;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with current time as actual end time', () => {
    fixture.detectChanges();

    const actualEndTime = component.completeForm.get('actualEndTime')?.value;
    expect(actualEndTime).toBeInstanceOf(Date);
  });

  it('should mark form as invalid when notes are empty', () => {
    component.completeForm.patchValue({ sessionNotes: '' });

    expect(component.completeForm.invalid).toBe(true);
  });

  it('should mark form as invalid when notes are too short', () => {
    component.completeForm.patchValue({ sessionNotes: 'Short' });

    expect(component.completeForm.invalid).toBe(true);
  });

  it('should mark form as valid with sufficient notes', () => {
    component.completeForm.patchValue({
      sessionNotes: 'This is a complete session note with sufficient length',
    });

    expect(component.completeForm.valid).toBe(true);
  });

  it('should complete session successfully with notes only', () => {
    const completedSession = { ...mockSession, status: SessionStatus.COMPLETED };
    sessionService.completeSession.and.returnValue(of(completedSession));

    component.completeForm.patchValue({
      sessionNotes: 'Patient showed improvement during the session',
      actualEndTime: null,
    });

    spyOn(component.completed, 'emit');

    component.submit();

    expect(sessionService.completeSession).toHaveBeenCalledWith(mockSession.id, {
      sessionNotes: 'Patient showed improvement during the session',
    });
    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'success' })
    );
    expect(component.completed.emit).toHaveBeenCalled();
  });

  it('should complete session with notes and actual end time', () => {
    const completedSession = { ...mockSession, status: SessionStatus.COMPLETED };
    sessionService.completeSession.and.returnValue(of(completedSession));

    const endTime = new Date('2026-04-15T11:05:00');
    component.completeForm.patchValue({
      sessionNotes: 'Session completed successfully',
      actualEndTime: endTime,
    });

    spyOn(component.completed, 'emit');

    component.submit();

    expect(sessionService.completeSession).toHaveBeenCalledWith(
      mockSession.id,
      jasmine.objectContaining({
        sessionNotes: 'Session completed successfully',
        actualEndTime: jasmine.any(String),
      })
    );
    expect(component.completed.emit).toHaveBeenCalled();
  });

  it('should handle completion error', () => {
    sessionService.completeSession.and.returnValue(
      throwError(() => new Error('Completion failed'))
    );

    component.completeForm.patchValue({
      sessionNotes: 'Valid session notes',
    });

    component.submit();

    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
    expect(component.loading).toBe(false);
  });

  it('should not submit when form is invalid', () => {
    component.completeForm.patchValue({ sessionNotes: '' });

    component.submit();

    expect(sessionService.completeSession).not.toHaveBeenCalled();
  });

  it('should emit cancelled event when cancel is clicked', () => {
    spyOn(component.cancelled, 'emit');

    component.cancel();

    expect(component.cancelled.emit).toHaveBeenCalled();
  });

  it('should correctly determine if field is invalid', () => {
    const notesControl = component.completeForm.get('sessionNotes');
    notesControl?.markAsTouched();
    notesControl?.setValue('');

    expect(component.isInvalid('sessionNotes')).toBe(true);
  });

  it('should return notes length', () => {
    component.completeForm.patchValue({ sessionNotes: 'Test notes' });

    expect(component.notesLength).toBe(10);
  });
});
