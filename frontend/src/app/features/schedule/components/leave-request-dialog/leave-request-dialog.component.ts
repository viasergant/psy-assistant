import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Leave, LeaveRequest, LeaveType, getLeaveTypeLabel } from '../../models/schedule.model';
import { LeaveRequestService } from '../../services/leave-request.service';

/**
 * Modal dialog for submitting a leave request.
 *
 * Emits `submitted` with the created Leave record on success,
 * or `cancelled` when the user dismisses without saving.
 */
@Component({
  selector: 'app-leave-request-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="leave-request-title">
      <div class="dialog">
        <h2 id="leave-request-title">Request Leave</h2>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="leaveType">Leave type <span aria-hidden="true">*</span></label>
            <select
              id="leaveType"
              formControlName="leaveType"
              [attr.aria-invalid]="isInvalid('leaveType')"
              aria-required="true"
            >
              <option value="">Select leave type...</option>
              <option *ngFor="let type of leaveTypes" [value]="type">
                {{ getLeaveTypeLabel(type) }}
              </option>
            </select>
            <span *ngIf="isInvalid('leaveType')" class="error-msg" role="alert">
              Leave type is required.
            </span>
          </div>

          <div class="field">
            <label for="startDate">Start date <span aria-hidden="true">*</span></label>
            <input
              id="startDate"
              type="date"
              formControlName="startDate"
              [attr.aria-invalid]="isInvalid('startDate')"
              aria-required="true"
            />
            <span *ngIf="isInvalid('startDate')" class="error-msg" role="alert">
              Start date is required.
            </span>
          </div>

          <div class="field">
            <label for="endDate">End date <span aria-hidden="true">*</span></label>
            <input
              id="endDate"
              type="date"
              formControlName="endDate"
              [attr.aria-invalid]="isInvalid('endDate')"
              aria-required="true"
              [min]="form.value.startDate"
            />
            <span *ngIf="isInvalid('endDate')" class="error-msg" role="alert">
              {{ getEndDateError() }}
            </span>
          </div>

          <div class="field">
            <label for="requestNotes">{{ 'schedule.leave.notes' | transloco }} (optional)</label>
            <textarea
              id="requestNotes"
              formControlName="requestNotes"
              rows="4"
              [placeholder]="'schedule.leave.notesPlaceholder' | transloco"
            ></textarea>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">{{ serverError }}</div>

          <div class="actions">
            <button type="button" class="btn-secondary" (click)="cancel()" [disabled]="saving">
              Cancel
            </button>
            <button type="submit" class="btn-primary" [disabled]="saving || !form.valid">
              {{ saving ? 'Submitting…' : 'Submit Request' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .dialog-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.45);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
      }

      .dialog {
        background: #fff;
        border-radius: 8px;
        padding: 2rem;
        width: 480px;
        max-width: 95vw;
        max-height: 90vh;
        overflow-y: auto;
        box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
      }

      h2 {
        margin: 0 0 1.5rem;
        font-size: 1.25rem;
      }

      .field {
        display: flex;
        flex-direction: column;
        margin-bottom: 1rem;
      }

      label {
        font-weight: 500;
        margin-bottom: 0.25rem;
        font-size: 0.9375rem;
      }

      input[type='text'],
      input[type='date'],
      textarea,
      select {
        appearance: none;
        -webkit-appearance: none;
        padding: 0.6rem 0.875rem;
        border: 1.5px solid #d1d5db;
        border-radius: 8px;
        font-size: 0.9375rem;
        font-family: inherit;
        color: #0f172a;
        background: #fff;
        outline: none;
        transition: border-color 0.15s ease, box-shadow 0.15s ease;
      }

      input:focus,
      textarea:focus,
      select:focus {
        border-color: #0ea5a0;
        box-shadow: 0 0 0 3px rgba(14, 165, 160, 0.15);
      }

      input[aria-invalid='true'],
      select[aria-invalid='true'] {
        border-color: #dc2626;
      }

      textarea {
        resize: vertical;
      }

      .error-msg {
        color: #dc2626;
        font-size: 0.8125rem;
        margin-top: 0.25rem;
      }

      .alert-error {
        background: #fef2f2;
        border: 1px solid #dc2626;
        color: #991b1b;
        padding: 0.75rem;
        border-radius: 6px;
        margin-bottom: 1rem;
        font-size: 0.875rem;
      }

      .actions {
        display: flex;
        gap: 0.75rem;
        justify-content: flex-end;
        margin-top: 1.5rem;
      }

      .btn-primary,
      .btn-secondary {
        padding: 0.625rem 1.25rem;
        border-radius: 6px;
        font-size: 0.875rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
        border: none;
      }

      .btn-primary {
        background: #0ea5a0;
        color: white;
      }

      .btn-primary:hover:not(:disabled) {
        background: #0d9490;
      }

      .btn-primary:disabled {
        background: #9ca3af;
        cursor: not-allowed;
      }

      .btn-secondary {
        background: transparent;
        color: #374151;
        border: 1px solid #d1d5db;
      }

      .btn-secondary:hover:not(:disabled) {
        background: #f9fafb;
      }

      .btn-secondary:disabled {
        color: #9ca3af;
        cursor: not-allowed;
      }
    `
  ]
})
export class LeaveRequestDialogComponent implements OnInit {
  @Input() therapistProfileId!: string;
  @Output() submitted = new EventEmitter<Leave>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  serverError: string | null = null;
  leaveTypes = Object.values(LeaveType);

  constructor(
    private fb: FormBuilder,
    private leaveRequestService: LeaveRequestService
  ) {}

  ngOnInit(): void {
    const today = new Date().toISOString().split('T')[0];

    this.form = this.fb.group(
      {
        leaveType: ['', Validators.required],
        startDate: [today, Validators.required],
        endDate: [today, Validators.required],
        requestNotes: ['']
      },
      {
        validators: this.dateRangeValidator
      }
    );
  }

  /**
   * Custom validator to ensure endDate >= startDate
   */
  dateRangeValidator(group: FormGroup): { [key: string]: boolean } | null {
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;

    if (start && end && new Date(end) < new Date(start)) {
      return { dateRangeInvalid: true };
    }

    return null;
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && control.touched);
  }

  getEndDateError(): string {
    const endDateControl = this.form.get('endDate');
    if (endDateControl?.hasError('required')) {
      return 'End date is required.';
    }
    if (this.form.hasError('dateRangeInvalid')) {
      return 'End date cannot be before start date.';
    }
    return 'Invalid end date.';
  }

  getLeaveTypeLabel(type: LeaveType): string {
    return getLeaveTypeLabel(type);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.serverError = null;

    const request: LeaveRequest = {
      leaveType: this.form.value.leaveType,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
      requestNotes: this.form.value.requestNotes || undefined
    };

    this.leaveRequestService.submitLeaveRequest(this.therapistProfileId, request).subscribe({
      next: (leave) => {
        this.saving = false;
        this.submitted.emit(leave);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error submitting leave request:', err);
        this.serverError = err.error?.message || 'Failed to submit leave request. Please try again.';
        this.saving = false;
      }
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
