import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SessionService } from './session.service';
import {
  SessionRecord,
  SessionStatus,
  SessionType,
  CompleteSessionRequest,
  SessionFilters,
} from '../models/session.model';

describe('SessionService', () => {
  let service: SessionService;
  let httpMock: HttpTestingController;

  const mockSession: SessionRecord = {
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
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SessionService],
    });
    service = TestBed.inject(SessionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getSessions', () => {
    it('should retrieve sessions without filters', () => {
      const mockSessions: SessionRecord[] = [mockSession];

      service.getSessions().subscribe((sessions) => {
        expect(sessions).toEqual(mockSessions);
        expect(sessions.length).toBe(1);
      });

      const req = httpMock.expectOne('/api/sessions');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);
      req.flush(mockSessions);
    });

    it('should retrieve sessions with client filter', () => {
      const filters: SessionFilters = { clientId: 201 };

      service.getSessions(filters).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/sessions' && request.params.has('clientId')
      );
      expect(req.request.params.get('clientId')).toBe('201');
      req.flush([mockSession]);
    });

    it('should retrieve sessions with date range filter', () => {
      const filters: SessionFilters = {
        startDate: '2026-04-01',
        endDate: '2026-04-30',
      };

      service.getSessions(filters).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/sessions' &&
        request.params.has('startDate') &&
        request.params.has('endDate')
      );
      expect(req.request.params.get('startDate')).toBe('2026-04-01');
      expect(req.request.params.get('endDate')).toBe('2026-04-30');
      req.flush([mockSession]);
    });

    it('should retrieve sessions with status filter', () => {
      const filters: SessionFilters = {
        status: [SessionStatus.PENDING, SessionStatus.IN_PROGRESS],
      };

      service.getSessions(filters).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url === '/api/sessions' && request.params.has('status')
      );
      expect(req.request.params.get('status')).toBe('PENDING,IN_PROGRESS');
      req.flush([mockSession]);
    });

    it('should handle empty status array', () => {
      const filters: SessionFilters = { status: [] };

      service.getSessions(filters).subscribe();

      const req = httpMock.expectOne('/api/sessions');
      expect(req.request.params.has('status')).toBe(false);
      req.flush([mockSession]);
    });
  });

  describe('startSession', () => {
    it('should start a pending session', () => {
      const startedSession: SessionRecord = {
        ...mockSession,
        status: SessionStatus.IN_PROGRESS,
      };

      service.startSession(1).subscribe((session) => {
        expect(session.status).toBe(SessionStatus.IN_PROGRESS);
        expect(session.id).toBe(1);
      });

      const req = httpMock.expectOne('/api/sessions/1/start');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(startedSession);
    });
  });

  describe('completeSession', () => {
    it('should complete a session with notes only', () => {
      const request: CompleteSessionRequest = {
        sessionNotes: 'Patient showed improvement',
      };
      const completedSession: SessionRecord = {
        ...mockSession,
        status: SessionStatus.COMPLETED,
      };

      service.completeSession(1, request).subscribe((session) => {
        expect(session.status).toBe(SessionStatus.COMPLETED);
      });

      const req = httpMock.expectOne('/api/sessions/1/complete');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(completedSession);
    });

    it('should complete a session with notes and actual end time', () => {
      const request: CompleteSessionRequest = {
        sessionNotes: 'Session completed successfully',
        actualEndTime: '11:05:00',
      };
      const completedSession: SessionRecord = {
        ...mockSession,
        status: SessionStatus.COMPLETED,
      };

      service.completeSession(1, request).subscribe((session) => {
        expect(session.status).toBe(SessionStatus.COMPLETED);
      });

      const req = httpMock.expectOne('/api/sessions/1/complete');
      expect(req.request.body).toEqual(request);
      req.flush(completedSession);
    });
  });

  describe('cancelSession', () => {
    it('should cancel a session with reason', () => {
      const reason = 'Client requested cancellation';
      const cancelledSession: SessionRecord = {
        ...mockSession,
        status: SessionStatus.CANCELLED,
        cancellationReason: reason,
      };

      service.cancelSession(1, reason).subscribe((session) => {
        expect(session.status).toBe(SessionStatus.CANCELLED);
        expect(session.cancellationReason).toBe(reason);
      });

      const req = httpMock.expectOne('/api/sessions/1/cancel');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ reason });
      req.flush(cancelledSession);
    });
  });
});
