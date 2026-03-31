import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ScheduleApiService } from './schedule-api.service';
import {
  RecurringSchedule,
  RecurringScheduleRequest,
  ScheduleOverride,
  ScheduleOverrideRequest,
  ScheduleSummary,
  DayOfWeek
} from '../models/schedule.model';

describe('ScheduleApiService', () => {
  let service: ScheduleApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ScheduleApiService]
    });

    service = TestBed.inject(ScheduleApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get schedule summary', () => {
    const mockSummary: ScheduleSummary = {
      therapistProfileId: 'test-id',
      therapistName: 'Test Therapist',
      timezone: 'America/New_York',
      recurringSchedule: [],
      overrides: [],
      leavePeriodsup: []
    };

    service.getScheduleSummary('test-id').subscribe(summary => {
      expect(summary).toEqual(mockSummary);
    });

    const req = httpMock.expectOne('/api/v1/therapists/test-id/schedule');
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('should get my schedule', () => {
    const mockSummary: ScheduleSummary = {
      therapistProfileId: 'my-id',
      therapistName: 'My Name',
      timezone: 'America/New_York',
      recurringSchedule: [],
      overrides: [],
      leavePeriodsup: []
    };

    service.getMySchedule().subscribe(summary => {
      expect(summary).toEqual(mockSummary);
    });

    const req = httpMock.expectOne('/api/v1/therapists/me/schedule');
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('should create recurring schedule', () => {
    const request: RecurringScheduleRequest = {
      dayOfWeek: DayOfWeek.MONDAY,
      startTime: '09:00',
      endTime: '17:00',
      timezone: 'America/New_York'
    };

    const mockResponse: RecurringSchedule = {
      id: 'schedule-id',
      therapistProfileId: 'test-id',
      ...request
    };

    service.createRecurringSchedule('test-id', request).subscribe(schedule => {
      expect(schedule).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/v1/therapists/test-id/schedule/recurring');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should create schedule override', () => {
    const request: ScheduleOverrideRequest = {
      date: '2026-04-15',
      available: false,
      reason: 'Conference'
    };

    const mockResponse: ScheduleOverride = {
      id: 'override-id',
      therapistProfileId: 'test-id',
      ...request
    };

    service.createScheduleOverride('test-id', request).subscribe(override => {
      expect(override).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/v1/therapists/test-id/schedule/overrides');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should delete recurring schedule', () => {
    service.deleteRecurringSchedule('test-id', 'schedule-id').subscribe();

    const req = httpMock.expectOne(
      '/api/v1/therapists/test-id/schedule/recurring/schedule-id'
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should delete schedule override', () => {
    service.deleteScheduleOverride('test-id', 'override-id').subscribe();

    const req = httpMock.expectOne(
      '/api/v1/therapists/test-id/schedule/overrides/override-id'
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
