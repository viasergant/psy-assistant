import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { TranslocoModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { SessionListComponent } from './session-list.component';
import { SessionService } from '../../services/session.service';
import { SessionRecord, SessionStatus, SessionType } from '../../models/session.model';

describe('SessionListComponent', () => {
  let component: SessionListComponent;
  let fixture: ComponentFixture<SessionListComponent>;
  let sessionService: jasmine.SpyObj<SessionService>;
  let messageService: jasmine.SpyObj<MessageService>;

  const mockSessions: SessionRecord[] = [
    {
      id: 1,
      appointmentId: 101,
      clientId: 201,
      clientName: 'John Doe',
      therapistId: 301,
      sessionDate: '2026-04-15',
      scheduledStartTime: '10:00:00',
      sessionType: SessionType.FOLLOW_UP,
      plannedDuration: 60,
      status: SessionStatus.PENDING,
      createdAt: '2026-04-01T08:00:00Z',
      updatedAt: '2026-04-01T08:00:00Z',
    },
    {
      id: 2,
      appointmentId: 102,
      clientId: 202,
      clientName: 'Jane Smith',
      therapistId: 301,
      sessionDate: '2026-04-16',
      scheduledStartTime: '14:00:00',
      sessionType: SessionType.INITIAL_CONSULTATION,
      plannedDuration: 90,
      status: SessionStatus.IN_PROGRESS,
      createdAt: '2026-04-01T09:00:00Z',
      updatedAt: '2026-04-01T09:00:00Z',
    },
  ];

  beforeEach(async () => {
    const sessionServiceSpy = jasmine.createSpyObj('SessionService', [
      'getSessions',
      'startSession',
      'completeSession',
      'cancelSession',
    ]);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [
        SessionListComponent,
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
    sessionService.getSessions.and.returnValue(of(mockSessions));

    fixture = TestBed.createComponent(SessionListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load sessions on init', () => {
    fixture.detectChanges();

    expect(sessionService.getSessions).toHaveBeenCalled();
    expect(component.sessions).toEqual(mockSessions);
    expect(component.loading).toBe(false);
  });

  it('should handle load error', () => {
    sessionService.getSessions.and.returnValue(throwError(() => new Error('Load failed')));

    fixture.detectChanges();

    expect(component.error).toBeTruthy();
    expect(component.sessions).toEqual([]);
  });

  it('should apply client filter', () => {
    fixture.detectChanges();

    component.filterForm.patchValue({ clientSearch: 'john' });

    expect(component.filteredSessions.length).toBe(1);
    expect(component.filteredSessions[0].clientName).toBe('John Doe');
  });

  it('should clear all filters', () => {
    component.filterForm.patchValue({
      dateRange: 'last7Days',
      clientSearch: 'test',
      statusFilter: [SessionStatus.PENDING],
    });

    component.clearFilters();

    expect(component.filterForm.value.dateRange).toBe('today');
    expect(component.filterForm.value.clientSearch).toBe('');
    expect(component.filterForm.value.statusFilter).toEqual([]);
  });

  it('should determine correct status severity', () => {
    expect(component.getStatusSeverity(SessionStatus.PENDING)).toBe('secondary');
    expect(component.getStatusSeverity(SessionStatus.IN_PROGRESS)).toBe('success');
    expect(component.getStatusSeverity(SessionStatus.COMPLETED)).toBe('info');
    expect(component.getStatusSeverity(SessionStatus.CANCELLED)).toBe('danger');
  });

  it('should check if session can be started', () => {
    const pendingSession = mockSessions[0];
    const inProgressSession = mockSessions[1];

    expect(component.canStartSession(pendingSession)).toBe(true);
    expect(component.canStartSession(inProgressSession)).toBe(false);
  });

  it('should check if session can be completed', () => {
    const pendingSession = mockSessions[0];
    const inProgressSession = mockSessions[1];

    expect(component.canCompleteSession(pendingSession)).toBe(false);
    expect(component.canCompleteSession(inProgressSession)).toBe(true);
  });

  it('should start a session successfully', () => {
    const session = mockSessions[0];
    const startedSession = { ...session, status: SessionStatus.IN_PROGRESS };
    sessionService.startSession.and.returnValue(of(startedSession));

    fixture.detectChanges();
    component.startSession(session);

    expect(sessionService.startSession).toHaveBeenCalledWith(session.id);
    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'success' })
    );
  });

  it('should handle start session error', () => {
    const session = mockSessions[0];
    sessionService.startSession.and.returnValue(throwError(() => new Error('Start failed')));

    fixture.detectChanges();
    component.startSession(session);

    expect(messageService.add).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
  });

  it('should open complete dialog', () => {
    const session = mockSessions[1];

    component.openCompleteDialog(session);

    expect(component.showCompleteDialog).toBe(true);
    expect(component.selectedSession).toBe(session);
  });

  it('should open cancel dialog', () => {
    const session = mockSessions[0];

    component.openCancelDialog(session);

    expect(component.showCancelDialog).toBe(true);
    expect(component.selectedSession).toBe(session);
  });

  it('should handle session completed event', () => {
    component.showCompleteDialog = true;
    component.selectedSession = mockSessions[1];

    component.onSessionCompleted();

    expect(component.showCompleteDialog).toBe(false);
    expect(component.selectedSession).toBeNull();
    expect(sessionService.getSessions).toHaveBeenCalled();
  });
});
