import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LeaveRequestService } from './leave-request.service';
import {
  Leave,
  LeaveRequest,
  LeaveType,
  LeaveStatus,
  ConflictWarning
} from '../models/schedule.model';

describe('LeaveRequestService', () => {
  let service: LeaveRequestService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [LeaveRequestService]
    });

    service = TestBed.inject(LeaveRequestService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should submit leave request', () => {
    const request: LeaveRequest = {
      leaveType: LeaveType.ANNUAL,
      startDate: '2026-05-01',
      endDate: '2026-05-14',
      requestNotes: 'Summer vacation'
    };

    const mockResponse: Leave = {
      id: 'leave-id',
      therapistProfileId: 'test-id',
      status: LeaveStatus.PENDING,
      ...request
    };

    service.submitLeaveRequest('test-id', request).subscribe(leave => {
      expect(leave).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/v1/therapists/test-id/leave');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should submit my leave request', () => {
    const request: LeaveRequest = {
      leaveType: LeaveType.SICK,
      startDate: '2026-04-01',
      endDate: '2026-04-02'
    };

    const mockResponse: Leave = {
      id: 'leave-id',
      therapistProfileId: 'my-id',
      status: LeaveStatus.PENDING,
      ...request
    };

    service.submitMyLeaveRequest(request).subscribe(leave => {
      expect(leave).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/v1/therapists/me/leave');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should get leave requests with status filter', () => {
    const mockLeaves: Leave[] = [
      {
        id: 'leave-1',
        therapistProfileId: 'test-id',
        leaveType: LeaveType.ANNUAL,
        startDate: '2026-05-01',
        endDate: '2026-05-14',
        status: LeaveStatus.PENDING
      }
    ];

    service.getLeaveRequests('test-id', LeaveStatus.PENDING).subscribe(leaves => {
      expect(leaves).toEqual(mockLeaves);
    });

    const req = httpMock.expectOne(
      '/api/v1/therapists/test-id/leave?status=PENDING'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockLeaves);
  });

  it('should check conflicts', () => {
    const mockConflict: ConflictWarning = {
      hasConflicts: true,
      affectedAppointments: [
        {
          appointmentId: 'appt-1',
          clientName: 'John Doe',
          scheduledAt: '2026-05-01T10:00:00Z',
          duration: 60
        }
      ]
    };

    service.checkConflicts('test-id', '2026-05-01', '2026-05-14').subscribe(conflict => {
      expect(conflict).toEqual(mockConflict);
    });

    const req = httpMock.expectOne(
      '/api/v1/therapists/test-id/leave/conflicts?startDate=2026-05-01&endDate=2026-05-14'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockConflict);
  });

  it('should cancel leave request', () => {
    service.cancelLeaveRequest('test-id', 'leave-id').subscribe();

    const req = httpMock.expectOne('/api/v1/therapists/test-id/leave/leave-id');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
