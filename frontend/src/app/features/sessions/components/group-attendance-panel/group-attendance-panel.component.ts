import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Subject, forkJoin, takeUntil } from 'rxjs';
import { AttendanceOutcome, AttendanceOutcomeResponse, GroupSessionParticipant } from '../../models/session.model';
import { SessionService } from '../../services/session.service';

export interface GroupAttendanceResult {
  clientId: string;
  clientName: string;
  response: AttendanceOutcomeResponse;
}

/**
 * Panel component for recording per-client attendance outcomes in a GROUP session.
 *
 * Renders one attendance selector per active participant. Submitting the form
 * calls the group attendance API independently for each client (matching the
 * REQUIRES_NEW transaction isolation in GroupAttendanceService).
 */
@Component({
  selector: 'app-group-attendance-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe, Button, Select],
  template: `
    <div class="group-attendance-panel">
      <div class="panel-header">
        <span class="panel-title">{{ 'sessions.group.recordAttendance' | transloco }}</span>
      </div>

      <form [formGroup]="attendanceForm" (ngSubmit)="submit()" *ngIf="participants.length > 0">
        <div *ngFor="let p of activeParticipants" class="participant-row">
          <label class="participant-label">{{ p.clientName }}</label>
          <p-select
            [formControlName]="p.clientId"
            [options]="outcomeOptions"
            optionLabel="label"
            optionValue="value"
            [placeholder]="'sessions.attendance.selectOutcome' | transloco"
            styleClass="w-full"
          ></p-select>
        </div>

        <div class="form-actions">
          <p-button
            type="submit"
            [label]="'sessions.group.saveAttendance' | transloco"
            [loading]="loading"
            [disabled]="attendanceForm.invalid || loading"
          ></p-button>
        </div>
      </form>

      <div *ngIf="participants.length === 0" class="empty-state">
        {{ 'sessions.group.noParticipants' | transloco }}
      </div>
    </div>
  `,
  styles: [`
    .group-attendance-panel {
      margin-top: 1rem;
    }
    .panel-header {
      margin-bottom: 1rem;
      font-weight: 600;
    }
    .participant-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      align-items: center;
      gap: 1rem;
      margin-bottom: 0.75rem;
    }
    .participant-label {
      font-weight: 500;
    }
    .form-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 1rem;
    }
    .empty-state {
      color: var(--text-color-secondary);
      font-style: italic;
      font-size: 0.875rem;
    }
  `]
})
export class GroupAttendancePanelComponent implements OnInit, OnDestroy {
  @Input() sessionId!: string;
  @Input() participants: GroupSessionParticipant[] = [];
  @Output() saved = new EventEmitter<GroupAttendanceResult[]>();

  attendanceForm: FormGroup;
  loading = false;
  activeParticipants: GroupSessionParticipant[] = [];

  outcomeOptions = [
    { label: 'Attended', value: AttendanceOutcome.ATTENDED },
    { label: 'No Show', value: AttendanceOutcome.NO_SHOW },
    { label: 'Late Cancellation', value: AttendanceOutcome.LATE_CANCELLATION },
    { label: 'Cancelled', value: AttendanceOutcome.CANCELLED },
    { label: 'Therapist Cancellation', value: AttendanceOutcome.THERAPIST_CANCELLATION },
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private sessionService: SessionService,
    private transloco: TranslocoService,
    private messageService: MessageService
  ) {
    this.attendanceForm = this.fb.group({});
  }

  ngOnInit(): void {
    this.activeParticipants = (this.participants ?? []).filter(p => !p.removedAt);
    // Build one form control per active participant
    this.activeParticipants.forEach(p => {
      this.attendanceForm.addControl(
        p.clientId,
        this.fb.control(p.attendanceOutcome ?? null, Validators.required)
      );
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.attendanceForm.invalid) {
      this.attendanceForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const formValue = this.attendanceForm.value;

    // Fire one request per client in parallel; failures on one must not block others
    const requests: Record<string, any> = {};
    this.activeParticipants.forEach(p => {
      const outcome: AttendanceOutcome = formValue[p.clientId];
      requests[p.clientId] = this.sessionService.recordGroupClientAttendance(
        this.sessionId,
        p.clientId,
        { outcome }
      );
    });

    forkJoin(requests)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results: any) => {
          const outcomes: GroupAttendanceResult[] = this.activeParticipants.map(p => ({
            clientId: p.clientId,
            clientName: p.clientName,
            response: results[p.clientId] as AttendanceOutcomeResponse,
          }));
          this.messageService.add({
            severity: 'success',
            summary: this.transloco.translate('sessions.group.attendanceSaved'),
          });
          this.loading = false;
          this.saved.emit(outcomes);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: this.transloco.translate('sessions.messages.attendanceError'),
          });
          this.loading = false;
        },
      });
  }
}
