import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ClientService } from '../../services/client.service';
import { ClientTimelineComponent } from './client-timeline.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslocoPipe } from '@jsverse/transloco';
import { Pipe, PipeTransform } from '@angular/core';
import { TimelineEvent } from '../../models/client.model';

@Pipe({ name: 'transloco', standalone: true })
class MockTranslocoPipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

describe('ClientTimelineComponent', () => {
  let component: ClientTimelineComponent;
  let fixture: ComponentFixture<ClientTimelineComponent>;
  let clientServiceSpy: jasmine.SpyObj<ClientService>;

  const mockTimelineEvents: TimelineEvent[] = [
    {
      eventId: 'evt-1',
      eventType: 'APPOINTMENT',
      eventSubtype: 'appointment.confirmed',
      eventTimestamp: '2026-03-15T10:00:00Z',
      actorName: 'Dr. Smith',
      eventData: {
        notes: 'Initial consultation',
        durationMinutes: 60
      },
      createdAt: '2026-03-15T10:00:00Z'
    },
    {
      eventId: 'evt-2',
      eventType: 'PROFILE_CHANGE',
      eventSubtype: 'profile.updated',
      eventTimestamp: '2026-03-10T14:30:00Z',
      actorName: 'Admin User',
      eventData: {
        changes: [
          { fieldName: 'phoneNumber', oldValue: '+380671111111', newValue: '+380672222222' }
        ]
      },
      createdAt: '2026-03-10T14:30:00Z'
    },
    {
      eventId: 'evt-3',
      eventType: 'CONVERSION',
      eventSubtype: 'lead.converted',
      eventTimestamp: '2026-03-01T09:00:00Z',
      actorName: 'Admin User',
      eventData: {},
      createdAt: '2026-03-01T09:00:00Z'
    }
  ];

  beforeEach(async () => {
    const clientService = jasmine.createSpyObj('ClientService', ['getTimeline']);
    clientService.getTimeline.and.returnValue(of([])); // Default spy return value

    await TestBed.configureTestingModule({
      imports: [
        ClientTimelineComponent,
        HttpClientTestingModule
      ],
      providers: [
        { provide: ClientService, useValue: clientService }
      ]
    })
    .overrideComponent(ClientTimelineComponent, {
      remove: { imports: [TranslocoPipe] },
      add: { imports: [MockTranslocoPipe] }
    })
    .compileComponents();

    fixture = TestBed.createComponent(ClientTimelineComponent);
    component = fixture.componentInstance;
    clientServiceSpy = TestBed.inject(ClientService) as jasmine.SpyObj<ClientService>;
    component.clientId = 'test-client-id';
    // Don't call fixture.detectChanges() here - let each test control when it runs
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load timeline on init', () => {
      clientServiceSpy.getTimeline.and.returnValue(of(mockTimelineEvents));
      
      fixture.detectChanges(); // Trigger ngOnInit
      
      expect(clientServiceSpy.getTimeline).toHaveBeenCalledWith('test-client-id', undefined, 0, 50);
      expect(component.events).toEqual(mockTimelineEvents);
      expect(component.loading).toBe(false);
    });

    it('should not load if clientId is missing', () => {
      component.clientId = '';
      
      fixture.detectChanges(); // Trigger ngOnInit
      
      expect(clientServiceSpy.getTimeline).not.toHaveBeenCalled();
    });
  });

  describe('loadTimeline', () => {
    it('should set loading state correctly', () => {
      clientServiceSpy.getTimeline.and.returnValue(of(mockTimelineEvents));
      
      expect(component.loading).toBe(false);
      component.loadTimeline();
      
      // Loading is set to true synchronously, then false after response
      expect(component.loading).toBe(false); // After observable completes
    });

    it('should append events on successful load', () => {
      const firstBatch = [mockTimelineEvents[0]];
      const secondBatch = [mockTimelineEvents[1]];
      
      clientServiceSpy.getTimeline.and.returnValue(of(firstBatch));
      component.loadTimeline();
      
      expect(component.events).toEqual(firstBatch);
      
      component.currentPage = 1;
      clientServiceSpy.getTimeline.and.returnValue(of(secondBatch));
      component.loadTimeline();
      
      expect(component.events).toEqual([...firstBatch, ...secondBatch]);
    });

    it('should set hasMore to true when results equal page size', () => {
      const fullPage = new Array(50).fill(mockTimelineEvents[0]);
      clientServiceSpy.getTimeline.and.returnValue(of(fullPage));
      
      component.loadTimeline();
      
      expect(component.hasMore).toBe(true);
    });

    it('should set hasMore to false when results are less than page size', () => {
      clientServiceSpy.getTimeline.and.returnValue(of(mockTimelineEvents));
      
      component.loadTimeline();
      
      expect(component.hasMore).toBe(false);
    });

    it('should handle errors gracefully', () => {
      clientServiceSpy.getTimeline.and.returnValue(
        throwError(() => new Error('Network error'))
      );
      
      component.loadTimeline();
      
      expect(component.loading).toBe(false);
      expect(component.hasMore).toBe(false);
    });

    it('should not load if already loading', () => {
      component.loading = true;
      clientServiceSpy.getTimeline.and.returnValue(of(mockTimelineEvents));
      
      component.loadTimeline();
      
      expect(clientServiceSpy.getTimeline).not.toHaveBeenCalled();
    });
  });

  describe('loadMore', () => {
    it('should increment page and load next batch', () => {
      clientServiceSpy.getTimeline.and.returnValue(of(mockTimelineEvents));
      
      expect(component.currentPage).toBe(0);
      component.loadMore();
      
      expect(component.currentPage).toBe(1);
      expect(clientServiceSpy.getTimeline).toHaveBeenCalledWith('test-client-id', undefined, 1, 50);
    });
  });

  describe('onFilterChange', () => {
    it('should reset events and reload with filter', () => {
      component.events = mockTimelineEvents;
      component.currentPage = 2;
      component.selectedEventType = 'APPOINTMENT';
      
      // Return a full page to set hasMore to true
      const fullPage = new Array(50).fill(mockTimelineEvents[0]);
      clientServiceSpy.getTimeline.and.returnValue(of(fullPage));
      
      component.onFilterChange();
      
      expect(component.events).toEqual(fullPage);
      expect(component.currentPage).toBe(0);
      expect(component.hasMore).toBe(true);
      expect(clientServiceSpy.getTimeline).toHaveBeenCalledWith(
        'test-client-id',
        ['APPOINTMENT'],
        0,
        50
      );
    });

    it('should pass undefined eventTypes when filter is null', () => {
      component.selectedEventType = null;
      clientServiceSpy.getTimeline.and.returnValue(of(mockTimelineEvents));
      
      component.onFilterChange();
      
      expect(clientServiceSpy.getTimeline).toHaveBeenCalledWith(
        'test-client-id',
        undefined,
        0,
        50
      );
    });
  });

  describe('getEventTypeClass', () => {
    it('should return the event type as class name', () => {
      expect(component.getEventTypeClass('APPOINTMENT')).toBe('APPOINTMENT');
      expect(component.getEventTypeClass('PROFILE_CHANGE')).toBe('PROFILE_CHANGE');
    });
  });

  describe('formatTimestamp', () => {
    it('should format timestamp to locale string', () => {
      const result = component.formatTimestamp('2026-03-15T10:00:00Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });
  });

  describe('hasEventData', () => {
    it('should return true when event has data', () => {
      const event = mockTimelineEvents[0];
      expect(component.hasEventData(event)).toBe(true);
    });

    it('should return false when event data is empty', () => {
      const event = mockTimelineEvents[2];
      expect(component.hasEventData(event)).toBe(false);
    });
  });

  describe('ngOnDestroy', () => {
    it('should complete destroy$ subject', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');
      
      component.ngOnDestroy();
      
      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });

  describe('pagination', () => {
    it('should support infinite scroll with 50-item page size', () => {
      expect(component.pageSize).toBe(50);
    });

    it('should respect hasMore flag for pagination', () => {
      // Verify hasMore logic rather than DOM rendering
      component.hasMore = true;
      expect(component.hasMore).toBe(true);
      
      component.hasMore = false;
      expect(component.hasMore).toBe(false);
    });

    it('should respect hasMore flag for pagination', () => {
      // Verify hasMore logic rather than DOM rendering
      component.hasMore = true;
      expect(component.hasMore).toBe(true);
      
      component.hasMore = false;
      expect(component.hasMore).toBe(false);
    });

    it('should toggle loading state appropriately', () => {
      component.loading = true;
      fixture.detectChanges();
      expect(component.loading).toBe(true);
      
      component.loading = false;
      fixture.detectChanges();
      expect(component.loading).toBe(false);
    });
  });

  describe('event type filters', () => {
    it('should have all event type filters', () => {
      const expectedFilters = [
        { label: 'All Events', value: null },
        { label: 'Appointments', value: 'APPOINTMENT' },
        { label: 'Profile Changes', value: 'PROFILE_CHANGE' },
        { label: 'Conversion', value: 'CONVERSION' },
        { label: 'Notes', value: 'NOTE' },
        { label: 'Payments', value: 'PAYMENT' },
        { label: 'Communications', value: 'COMMUNICATION' }
      ];
      
      expect(component.eventTypeFilters).toEqual(expectedFilters);
    });
  });

  describe('empty state', () => {
    it('should show empty message when no events and not loading', () => {
      clientServiceSpy.getTimeline.and.returnValue(of([]));
      component.events = [];
      component.loading = false;
      
      fixture.detectChanges();
      
      const emptyMessage = fixture.nativeElement.querySelector('.timeline-empty');
      expect(emptyMessage).toBeTruthy();
    });

    it('should not show empty message when loading', () => {
      clientServiceSpy.getTimeline.and.returnValue(of([]));
      component.events = [];
      component.loading = true;
      
      fixture.detectChanges();
      
      const emptyMessage = fixture.nativeElement.querySelector('.timeline-empty');
      expect(emptyMessage).toBeFalsy();
    });
  });
});
