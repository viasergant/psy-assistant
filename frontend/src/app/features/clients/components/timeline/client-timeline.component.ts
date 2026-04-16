import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { TimelineModule } from 'primeng/timeline';
import { Subject, takeUntil } from 'rxjs';
import { TimelineEvent } from '../../models/client.model';
import { ClientService } from '../../services/client.service';

interface EventTypeFilter {
  label: string;
  value: string | null;
}

/**
 * Client activity timeline component.
 * Displays appointments, profile changes, conversion history with infinite scroll.
 */
@Component({
  selector: 'app-client-timeline',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoPipe, TimelineModule, Select],
  template: `
    <div class="timeline-container">
      <div class="timeline-header">
        <h3>{{ 'timeline.title' | transloco }}</h3>
        <p-select
          [options]="eventTypeFilters"
          [(ngModel)]="selectedEventType"
          (onChange)="onFilterChange()"
          [placeholder]="'timeline.filter.placeholder' | transloco"
          optionLabel="label"
          optionValue="value"
          styleClass="event-type-filter"
        />
      </div>

      <div class="timeline-content" *ngIf="events.length > 0">
        <p-timeline [value]="events" align="left">
          <ng-template pTemplate="content" let-event>
            <div class="timeline-event">
              <div class="event-header">
                <span class="event-type" [class]="getEventTypeClass(event.eventType)">
                  {{ 'timeline.eventType.' + event.eventType | transloco }}
                </span>
                <span class="event-timestamp">{{ formatTimestamp(event.eventTimestamp) }}</span>
              </div>
              <div class="event-details">
                <div class="event-subtype">{{ 'timeline.eventSubtype.' + event.eventSubtype | transloco }}</div>
                <div class="event-actor" *ngIf="event.actorName">
                  {{ 'timeline.by' | transloco }} {{ event.actorName }}
                </div>
                <div class="event-data" *ngIf="hasEventData(event)">
                  <ng-container [ngSwitch]="event.eventType">
                    <div *ngSwitchCase="'APPOINTMENT'">
                      <div *ngIf="event.eventData.notes">{{ event.eventData.notes }}</div>
                      <div>{{ 'timeline.appointment.duration' | transloco }}: {{ event.eventData.durationMinutes }}{{ 'timeline.appointment.minutes' | transloco }}</div>
                    </div>
                    <div *ngSwitchCase="'PROFILE_CHANGE'">
                      <div *ngFor="let change of event.eventData.changes" class="field-change">
                        <strong>{{ change.fieldName }}:</strong>
                        <span class="old-value">{{ change.oldValue || ('timeline.empty' | transloco) }}</span>
                        →
                        <span class="new-value">{{ change.newValue || ('timeline.empty' | transloco) }}</span>
                      </div>
                    </div>
                    <div *ngSwitchCase="'CONVERSION'">
                      {{ 'timeline.conversion.fromLead' | transloco }}
                    </div>
                    <div *ngSwitchCase="'ATTENDANCE_OUTCOME'">
                      <div *ngIf="event.eventData.outcome" class="attendance-outcome" [class]="'outcome-' + event.eventData.outcome">
                        {{ 'timeline.attendanceOutcome.' + event.eventData.outcome | transloco }}
                      </div>
                    </div>
                  </ng-container>
                </div>
              </div>
            </div>
          </ng-template>
        </p-timeline>
      </div>

      <div class="timeline-empty" *ngIf="events.length === 0 && !loading">
        {{ 'timeline.empty' | transloco }}
      </div>

      <div class="timeline-loading" *ngIf="loading">
        {{ 'timeline.loading' | transloco }}
      </div>

      <div class="timeline-load-more" *ngIf="hasMore && !loading">
        <button class="load-more-btn" (click)="loadMore()" type="button">
          {{ 'timeline.loadMore' | transloco }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .timeline-container {
      padding: 1rem;
    }

    .timeline-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .timeline-header h3 {
      margin: 0;
    }

    ::ng-deep .event-type-filter {
      min-width: 200px;
    }

    .timeline-event {
      padding: 0.5rem 0;
    }

    .event-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .event-type {
      font-weight: 600;
      padding: 0.25rem 0.75rem;
      border-radius: 1rem;
      font-size: 0.875rem;

      &.APPOINTMENT {
        background: var(--blue-100);
        color: var(--blue-900);
      }

      &.PROFILE_CHANGE {
        background: var(--orange-100);
        color: var(--orange-900);
      }

      &.CONVERSION {
        background: var(--green-100);
        color: var(--green-900);
      }

      &.NOTE {
        background: var(--purple-100);
        color: var(--purple-900);
      }

      &.PAYMENT {
        background: var(--teal-100);
        color: var(--teal-900);
      }

      &.COMMUNICATION {
        background: var(--pink-100);
        color: var(--pink-900);
      }

      &.ATTENDANCE_OUTCOME {
        background: var(--indigo-100);
        color: var(--indigo-900);
      }
    }

    .event-timestamp {
      color: var(--text-color-secondary);
      font-size: 0.875rem;
    }

    .event-details {
      padding-left: 1rem;
    }

    .event-subtype {
      font-weight: 500;
      margin-bottom: 0.25rem;
    }

    .event-actor {
      color: var(--text-color-secondary);
      font-size: 0.875rem;
      margin-bottom: 0.5rem;
    }

    .event-data {
      font-size: 0.875rem;
    }

    .attendance-outcome {
      display: inline-block;
      padding: 0.2rem 0.6rem;
      border-radius: 1rem;
      font-size: 0.8rem;
      font-weight: 600;

      &.outcome-ATTENDED { background: var(--green-100); color: var(--green-900); }
      &.outcome-NO_SHOW { background: var(--red-100); color: var(--red-900); }
      &.outcome-LATE_CANCELLATION { background: var(--orange-100); color: var(--orange-900); }
      &.outcome-CANCELLED { background: var(--surface-200); color: var(--text-color-secondary); }
      &.outcome-THERAPIST_CANCELLATION { background: var(--blue-100); color: var(--blue-900); }
    }

    .field-change {
      margin: 0.25rem 0;
    }

    .old-value {
      color: var(--red-600);
    }

    .new-value {
      color: var(--green-600);
    }

    .timeline-empty,
    .timeline-loading {
      text-align: center;
      padding: 2rem;
      color: var(--text-color-secondary);
    }

    .timeline-load-more {
      text-align: center;
      margin-top: 1rem;
    }

    .load-more-btn {
      padding: 0.75rem 1.5rem;
      background: var(--primary-color);
      color: white;
      border: none;
      border-radius: 0.25rem;
      cursor: pointer;

      &:hover {
        background: var(--primary-color-emphasis);
      }
    }
  `]
})
export class ClientTimelineComponent implements OnInit, OnDestroy {
  @Input() clientId!: string;

  events: TimelineEvent[] = [];
  selectedEventType: string | null = null;
  loading = false;
  hasMore = true;
  currentPage = 0;
  pageSize = 50;

  eventTypeFilters: EventTypeFilter[] = [
    { label: 'All Events', value: null },
    { label: 'Appointments', value: 'APPOINTMENT' },
    { label: 'Profile Changes', value: 'PROFILE_CHANGE' },
    { label: 'Conversion', value: 'CONVERSION' },
    { label: 'Notes', value: 'NOTE' },
    { label: 'Payments', value: 'PAYMENT' },
    { label: 'Communications', value: 'COMMUNICATION' },
    { label: 'Attendance', value: 'ATTENDANCE_OUTCOME' }
  ];

  private destroy$ = new Subject<void>();

  constructor(private clientService: ClientService) {}

  ngOnInit(): void {
    this.loadTimeline();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTimeline(): void {
    if (this.loading || !this.clientId) {
      return;
    }

    this.loading = true;

    const eventTypes = this.selectedEventType ? [this.selectedEventType] : undefined;

    this.clientService
      .getTimeline(this.clientId, eventTypes, this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.events = [...this.events, ...results];
          this.hasMore = results.length === this.pageSize;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.hasMore = false;
        }
      });
  }

  loadMore(): void {
    this.currentPage++;
    this.loadTimeline();
  }

  onFilterChange(): void {
    this.events = [];
    this.currentPage = 0;
    this.hasMore = true;
    this.loadTimeline();
  }

  getEventTypeClass(eventType: string): string {
    return eventType;
  }

  formatTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleString();
  }

  hasEventData(event: TimelineEvent): boolean {
    return event.eventData && Object.keys(event.eventData).length > 0;
  }
}
