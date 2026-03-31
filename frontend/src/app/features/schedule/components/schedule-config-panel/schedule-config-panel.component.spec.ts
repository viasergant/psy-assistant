import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ScheduleConfigPanelComponent } from './schedule-config-panel.component';
import { ScheduleApiService } from '../../services/schedule-api.service';
import { DayOfWeek, ScheduleSummary } from '../../models/schedule.model';

describe('ScheduleConfigPanelComponent', () => {
  let component: ScheduleConfigPanelComponent;
  let fixture: ComponentFixture<ScheduleConfigPanelComponent>;
  let mockScheduleApiService: jasmine.SpyObj<ScheduleApiService>;

  const mockScheduleSummary: ScheduleSummary = {
    therapistProfileId: 'test-id-123',
    therapistName: 'Dr. Test',
    timezone: 'America/New_York',
    recurringSchedule: [
      {
        id: 'schedule-1',
        therapistProfileId: 'test-id-123',
        dayOfWeek: 1, // MONDAY
        startTime: '09:00',
        endTime: '17:00',
        timezone: 'America/New_York'
      },
      {
        id: 'schedule-2',
        therapistProfileId: 'test-id-123',
        dayOfWeek: 3, // WEDNESDAY
        startTime: '10:00',
        endTime: '18:00',
        timezone: 'America/New_York'
      }
    ],
    overrides: [
      {
        id: 'override-1',
        therapistProfileId: 'test-id-123',
        date: '2024-04-15',
        available: false,
        reason: 'Conference'
      }
    ],
    leavePeriods: []
  };

  beforeEach(async () => {
    mockScheduleApiService = jasmine.createSpyObj('ScheduleApiService', [
      'getScheduleSummary',
      'createRecurringSchedule',
      'updateRecurringSchedule',
      'deleteRecurringSchedule',
      'deleteScheduleOverride'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        ScheduleConfigPanelComponent,
        CommonModule,
        FormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: ScheduleApiService, useValue: mockScheduleApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ScheduleConfigPanelComponent);
    component = fixture.componentInstance;
    component.therapistProfileId = 'test-id-123';
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should generate time options from 6:00 to 22:30', () => {
    mockScheduleApiService.getScheduleSummary.and.returnValue(of(mockScheduleSummary));
    
    fixture.detectChanges();

    expect(component.timeOptions.length).toBeGreaterThan(0);
    expect(component.timeOptions[0]).toBe('06:00');
    expect(component.timeOptions[component.timeOptions.length - 1]).toBe('22:30');
  });

  it('should load schedule summary on init', () => {
    mockScheduleApiService.getScheduleSummary.and.returnValue(of(mockScheduleSummary));

    fixture.detectChanges();

    expect(mockScheduleApiService.getScheduleSummary).toHaveBeenCalledWith('test-id-123');
    expect(component.overrides.length).toBe(1);
    expect(component.timezone).toBe('America/New_York');
  });

  it('should populate weekdays from recurring schedule', () => {
    mockScheduleApiService.getScheduleSummary.and.returnValue(of(mockScheduleSummary));

    fixture.detectChanges();

    const mondaySchedule = component.weekDays.find(d => d.dayOfWeek === DayOfWeek.MONDAY);
    const wednesdaySchedule = component.weekDays.find(d => d.dayOfWeek === DayOfWeek.WEDNESDAY);
    const tuesdaySchedule = component.weekDays.find(d => d.dayOfWeek === DayOfWeek.TUESDAY);

    expect(mondaySchedule?.enabled).toBe(true);
    expect(mondaySchedule?.startTime).toBe('09:00');
    expect(mondaySchedule?.endTime).toBe('17:00');
    expect(mondaySchedule?.scheduleId).toBe('schedule-1');

    expect(wednesdaySchedule?.enabled).toBe(true);
    expect(wednesdaySchedule?.startTime).toBe('10:00');
    expect(wednesdaySchedule?.endTime).toBe('18:00');

    expect(tuesdaySchedule?.enabled).toBe(false);
  });

  it('should emit close event when onClose is called', () => {
    mockScheduleApiService.getScheduleSummary.and.returnValue(of(mockScheduleSummary));
    spyOn(component.close, 'emit');

    fixture.detectChanges();
    component.onClose();

    expect(component.close.emit).toHaveBeenCalled();
  });

  it('should get override description correctly', () => {
    mockScheduleApiService.getScheduleSummary.and.returnValue(of(mockScheduleSummary));
    
    fixture.detectChanges();

    const unavailableOverride = {
      id: 'override-1',
      therapistProfileId: 'test-id-123',
      date: '2024-04-15',
      available: false,
      reason: 'Conference'
    };

    const availableOverride = {
      id: 'override-2',
      therapistProfileId: 'test-id-123',
      date: '2024-04-16',
      available: true,
      startTime: '14:00',
      endTime: '18:00'
    };

    expect(component.getOverrideDescription(unavailableOverride)).toBe('Conference');
    expect(component.getOverrideDescription(availableOverride)).toBe('14:00 — 18:00');
  });

  it('should get override type badge correctly', () => {
    mockScheduleApiService.getScheduleSummary.and.returnValue(of(mockScheduleSummary));
    
    fixture.detectChanges();

    const unavailableOverride = { available: false } as any;
    const availableOverride = { available: true } as any;

    expect(component.getOverrideType(unavailableOverride)).toBe('Unavailable');
    expect(component.getOverrideType(availableOverride)).toBe('Available');
  });

  it('should reset times when day is disabled', () => {
    mockScheduleApiService.getScheduleSummary.and.returnValue(of(mockScheduleSummary));
    
    fixture.detectChanges();

    const day = component.weekDays[0];
    day.enabled = true;
    day.startTime = '10:00';
    day.endTime = '18:00';

    day.enabled = false;
    component.onDayToggle(day);

    expect(day.startTime).toBe('09:00');
    expect(day.endTime).toBe('17:00');
  });
});
