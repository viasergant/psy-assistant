import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { Textarea } from 'primeng/textarea';
import { Subject, takeUntil } from 'rxjs';
import { SessionRecord } from '../../models/session.model';
import { SessionService } from '../../services/session.service';

/**
 * Dialog for cancelling a session with a required cancellation reason
 */
@Component({
  selector: 'app-cancel-session-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoPipe,
    DialogModule,
    Button,
    Textarea,
  ],
  templateUrl: './cancel-session-dialog.component.html',
  styleUrls: ['./cancel-session-dialog.component.scss'],
})
export class CancelSessionDialogComponent implements OnInit, OnDestroy {
  @Input() session!: SessionRecord;
  @Input() visible = false;
  @Output() cancelled = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  cancelForm: FormGroup;
  loading = false;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private sessionService: SessionService,
    private transloco: TranslocoService,
    private messageService: MessageService
  ) {
    this.cancelForm = this.fb.group({
      reason: ['', [Validators.required, Validators.minLength(5)]],
    });
  }

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.cancelForm.invalid) {
      this.cancelForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const reason = this.cancelForm.get('reason')?.value.trim();

    this.sessionService
      .cancelSession(this.session.id, reason)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'info',
            summary: this.transloco.translate('sessions.messages.sessionCancelled'),
          });
          this.loading = false;
          this.cancelled.emit();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: this.transloco.translate('sessions.messages.cancelError'),
          });
          this.loading = false;
        },
      });
  }

  close(): void {
    this.closed.emit();
  }

  isInvalid(controlName: string): boolean {
    const control = this.cancelForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get reasonLength(): number {
    return this.cancelForm.get('reason')?.value?.length || 0;
  }
}
