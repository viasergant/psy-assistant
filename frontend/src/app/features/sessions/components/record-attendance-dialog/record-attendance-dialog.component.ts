import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Subject, takeUntil } from 'rxjs';
import {
  AttendanceOutcome,
  AttendanceOutcomeResponse,
  SessionRecord,
} from '../../models/session.model';
import { SessionService } from '../../services/session.service';

/**
 * Dialog for recording an attendance outcome for a session.
 */
@Component({
  selector: 'app-record-attendance-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe, DialogModule, Button, Select],
  templateUrl: './record-attendance-dialog.component.html',
  styleUrls: ['./record-attendance-dialog.component.scss'],
})
export class RecordAttendanceDialogComponent implements OnInit, OnDestroy {
  @Input() session!: SessionRecord;
  @Input() visible = false;
  @Output() recorded = new EventEmitter<AttendanceOutcomeResponse>();
  @Output() closed = new EventEmitter<void>();

  attendanceForm: FormGroup;
  loading = false;

  outcomeOptions = [
    {
      label: 'sessions.attendance.outcome.attended',
      value: AttendanceOutcome.ATTENDED,
    },
    {
      label: 'sessions.attendance.outcome.noShow',
      value: AttendanceOutcome.NO_SHOW,
    },
    {
      label: 'sessions.attendance.outcome.lateCancellation',
      value: AttendanceOutcome.LATE_CANCELLATION,
    },
    {
      label: 'sessions.attendance.outcome.cancelled',
      value: AttendanceOutcome.CANCELLED,
    },
    {
      label: 'sessions.attendance.outcome.therapistCancellation',
      value: AttendanceOutcome.THERAPIST_CANCELLATION,
    },
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private sessionService: SessionService,
    private transloco: TranslocoService,
    private messageService: MessageService
  ) {
    this.attendanceForm = this.fb.group({
      outcome: [null, Validators.required],
    });
  }

  ngOnInit(): void {
    if (this.session?.attendanceOutcome) {
      this.attendanceForm.patchValue({ outcome: this.session.attendanceOutcome });
    }
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
    const outcome: AttendanceOutcome = this.attendanceForm.get('outcome')?.value;

    this.sessionService
      .recordAttendanceOutcome(this.session.id, { outcome })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: this.transloco.translate('sessions.attendance.saved'),
          });
          this.loading = false;
          this.recorded.emit(response);
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

  close(): void {
    this.closed.emit();
  }

  isInvalid(controlName: string): boolean {
    const control = this.attendanceForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
