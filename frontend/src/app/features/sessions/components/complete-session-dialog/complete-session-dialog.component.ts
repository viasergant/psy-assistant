import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { Textarea } from 'primeng/textarea';
import { Subject, takeUntil } from 'rxjs';
import { CompleteSessionRequest, SessionRecord } from '../../models/session.model';
import { SessionService } from '../../services/session.service';

/**
 * Dialog for completing an in-progress session with notes and optional actual end time
 */
@Component({
  selector: 'app-complete-session-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoPipe,
    Dialog,
    Button,
    Textarea,
    DatePicker,
  ],
  templateUrl: './complete-session-dialog.component.html',
  styleUrls: ['./complete-session-dialog.component.scss'],
})
export class CompleteSessionDialogComponent implements OnInit, OnDestroy {
  @Input() session!: SessionRecord;
  @Input() visible = false;
  @Output() completed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  completeForm: FormGroup;
  loading = false;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private sessionService: SessionService,
    private transloco: TranslocoService,
    private messageService: MessageService
  ) {
    this.completeForm = this.fb.group({
      sessionNotes: ['', [Validators.required, Validators.minLength(10)]],
      actualEndTime: [null],
    });
  }

  ngOnInit(): void {
    // Initialize with current time as default actual end time
    const now = new Date();
    this.completeForm.patchValue({
      actualEndTime: now,
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.completeForm.invalid) {
      this.completeForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const formValue = this.completeForm.value;
    const request: CompleteSessionRequest = {
      sessionNotes: formValue.sessionNotes.trim(),
    };

    if (formValue.actualEndTime) {
      request.actualEndTime = this.formatTime(formValue.actualEndTime);
    }

    this.sessionService
      .completeSession(this.session.id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: this.transloco.translate('sessions.messages.sessionCompleted'),
          });
          this.loading = false;
          this.completed.emit();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: this.transloco.translate('sessions.messages.completeError'),
          });
          this.loading = false;
        },
      });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  isInvalid(controlName: string): boolean {
    const control = this.completeForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  private formatTime(date: Date): string {
    return date.toTimeString().split(' ')[0]; // Returns HH:mm:ss
  }

  get notesLength(): number {
    return this.completeForm.get('sessionNotes')?.value?.length || 0;
  }
}
