import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ScheduleCalendarComponent } from './schedule-calendar.component';
import { AvailabilityService } from '../../services/availability.service';
import { of } from 'rxjs';
import { AvailabilitySlot } from '../../models/schedule.model';

interface CalendarCell {
  day: WeekDay;
  timeSlot: TimeSlot;
  available: boolean;
  hasOverride: boolean;
  isLeave: boolean;
  isBooked: boolean;
}

interface WeekDay {
  date: Date;
  dateString: string;
  dayOfWeek: string;
  dayLabel: string;
}

interface TimeSlot {
  hour: number;
  minute: number;
  timeString: string;
  displayTime: string;
}

describe('ScheduleCalendarComponent', () => {
  let component: ScheduleCalendarComponent;
  let fixture: ComponentFixture<ScheduleCalendarComponent>;
  let availabilityService: jasmine.SpyObj<AvailabilityService>;

  beforeEach(async () => {
    const availabilityServiceSpy = jasmine.createSpyObj('AvailabilityService', [
      'getAvailableSlots'
    ]);

    await TestBed.configureTestingModule({
      imports: [ScheduleCalendarComponent],
      providers: [
        { provide: AvailabilityService, useValue: availabilityServiceSpy }
      ]
    }).compileComponents();

    availabilityService = TestBed.inject(
      AvailabilityService
    ) as jasmine.SpyObj<AvailabilityService>;
    fixture = TestBed.createComponent(ScheduleCalendarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should generate 7 week days', () => {
    component.ngOnInit();
    expect(component.weekDays.length).toBe(7);
  });

  it('should generate time slots from 6 AM to 10 PM', () => {
    component.ngOnInit();
    expect(component.timeSlots.length).toBeGreaterThan(0);
    expect(component.timeSlots[0].hour).toBe(6);
    expect(component.timeSlots[0].minute).toBe(0);
  });

  it('should load availability when therapistProfileId is provided', () => {
    const mockSlots: AvailabilitySlot[] = [
      {
        date: '2026-03-31',
        startTime: '09:00',
        endTime: '09:30',
        available: true
      }
    ];

    availabilityService.getAvailableSlots.and.returnValue(of(mockSlots));
    component.therapistProfileId = 'test-therapist-id';

    component.ngOnInit();

    expect(availabilityService.getAvailableSlots).toHaveBeenCalled();
  });

  it('should navigate to previous week', () => {
    component.ngOnInit();
    const initialStart = component.currentWeekStart;

    availabilityService.getAvailableSlots.and.returnValue(of([]));
    component.previousWeek();

    expect(component.currentWeekStart).toBeLessThan(initialStart);
  });

  it('should navigate to next week', () => {
    component.ngOnInit();
    const initialStart = component.currentWeekStart;

    availabilityService.getAvailableSlots.and.returnValue(of([]));
    component.nextWeek();

    expect(component.currentWeekStart).toBeGreaterThan(initialStart);
  });

  it('should return correct cell class for available slot', () => {
    const cell = {
      available: true,
      isLeave: false,
      hasOverride: false,
      isBooked: false,
      day: {} as WeekDay,
      timeSlot: {} as TimeSlot
    } as CalendarCell;

    const cellClass = component.getCellClass(cell);
    expect(cellClass).toBe('cell-available');
  });

  it('should return correct cell class for leave', () => {
    const cell = {
      available: false,
      isLeave: true,
      hasOverride: false,
      isBooked: false,
      day: {} as WeekDay,
      timeSlot: {} as TimeSlot
    } as CalendarCell;

    const cellClass = component.getCellClass(cell);
    expect(cellClass).toBe('cell-leave');
  });

  it('should return correct cell title for tooltip', () => {
    const baseCell = {
      day: {} as WeekDay,
      timeSlot: {} as TimeSlot,
      hasOverride: false,
      isBooked: false
    };
    
    const availableCell: CalendarCell = { 
      ...baseCell, 
      available: true, 
      isLeave: false,
      hasOverride: false,
      isBooked: false 
    };
    
    const leaveCell: CalendarCell = { 
      ...baseCell, 
      available: false, 
      isLeave: true,
      hasOverride: false,
      isBooked: false
    };
    
    const bookedCell: CalendarCell = { 
      ...baseCell, 
      available: false, 
      isLeave: false,
      hasOverride: false,
      isBooked: true 
    };

    expect(component.getCellTitle(availableCell)).toBe('Available');
    expect(component.getCellTitle(leaveCell)).toBe('On leave');
    expect(component.getCellTitle(bookedCell)).toBe('Appointment booked');
  });
});
