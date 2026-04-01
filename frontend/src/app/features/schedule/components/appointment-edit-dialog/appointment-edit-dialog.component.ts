import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { Appointment, AppointmentStatus } from '../../models/schedule.model';
import { AppointmentApiService } from '../../services/appointment-api.service';

interface StatusOption {
  value: AppointmentStatus;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-appointment-edit-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoPipe],
  template: `
    <div class="dialog-overlay" (click)="onCancel()">
      <div class="dialog-container" (click)="$event.stopPropagation()">
        <!-- Dialog Header -->
        <header class="dialog-header">
          <h2>{{ 'schedule.appointment.edit.title' | transloco }}</h2>
          <button
            type="button"
            class="btn-close"
            (click)="onCancel()"
            [attr.aria-label]="'common.actions.close' | transloco"
          >
            ×
          </button>
        </header>

        <!-- Dialog Body -->
        <div class="dialog-body">
          <!-- Appointment Details (Read-only) -->
          <div class="form-section">
            <div class="form-field">
              <label class="form-label">{{ 'schedule.appointment.edit.appointmentTimeLabel' | transloco }}</label>
              <div class="read-only-value">{{ formatDateTime(appointment.startTime) }}</div>
            </div>

            <div class="form-field">
              <label class="form-label">{{ 'schedule.appointment.edit.durationLabel' | transloco }}</label>
              <div class="read-only-value">{{ appointment.durationMinutes }} {{ 'schedule.appointment.edit.minutes' | transloco }}</div>
            </div>
          </div>

          <!-- Status Selection -->
          <div class="form-section">
            <div class="form-field">
              <label for="status" class="form-label required">
                {{ 'schedule.appointment.edit.statusLabel' | transloco }}
              </label>
              <select
                id="status"
                [(ngModel)]="selectedStatus"
                class="form-select"
                [attr.aria-label]="'schedule.appointment.edit.statusLabel' | transloco"
              >
                <option
                  *ngFor="let option of statusOptions"
                  [value]="option.value"
                  [disabled]="option.disabled"
                >
                  {{ option.label }}
                </option>
              </select>
            </div>

            <!-- Notes -->
            <div class="form-field">
              <label for="notes" class="form-label">
                {{ 'schedule.appointment.edit.notesLabel' | transloco }}
              </label>
              <textarea
                id="notes"
                [(ngModel)]="notes"
                class="form-textarea"
                rows="4"
                [placeholder]="'schedule.appointment.edit.notesPlaceholder' | transloco"
                maxlength="1000"
              ></textarea>
              <div class="form-hint">
                {{ notes.length }}/1000 {{ 'schedule.appointment.edit.characters' | transloco }}
              </div>
            </div>
          </div>

          <!-- Error Message -->
          <div *ngIf="errorMessage" class="error-banner">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
            </svg>
            <span>{{ errorMessage }}</span>
          </div>
        </div>

        <!-- Dialog Footer -->
        <footer class="dialog-footer">
          <button
            type="button"
            class="btn btn-secondary"
            (click)="onCancel()"
            [disabled]="isSubmitting"
          >
            {{ 'common.actions.cancel' | transloco }}
          </button>
          <button
            type="button"
            class="btn btn-primary"
            (click)="onSubmit()"
            [disabled]="!isFormValid() || isSubmitting"
          >
            {{ isSubmitting ? ('schedule.appointment.edit.updatingInProgress' | transloco) : ('schedule.appointment.edit.updateButton' | transloco) }}
          </button>
        </footer>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
    }

    .dialog-container {
      background: white;
      border-radius: 12px;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1),
                  0 10px 10px -5px rgba(0, 0, 0, 0.04);
      max-width: 500px;
      width: 100%;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.5rem;
      border-bottom: 1px solid var(--color-border);
    }

    .dialog-header h2 {
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--color-text-primary);
      margin: 0;
    }

    .btn-close {
      background: transparent;
      border: none;
      font-size: 1.5rem;
      line-height: 1;
      color: var(--color-text-secondary);
      cursor: pointer;
      padding: 0.25rem;
      width: 2rem;
      height: 2rem;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      transition: all 0.2s;
    }

    .btn-close:hover {
      background: var(--color-bg);
      color: var(--color-text-primary);
    }

    .dialog-body {
      flex: 1;
      overflow-y: auto;
      padding: 1.5rem;
    }

    .form-section {
      margin-bottom: 1.5rem;
    }

    .form-section:last-child {
      margin-bottom: 0;
    }

    .form-field {
      margin-bottom: 1rem;
    }

    .form-field:last-child {
      margin-bottom: 0;
    }

    .form-label {
      display: block;
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--color-text-primary);
      margin-bottom: 0.5rem;
    }

    .form-label.required::after {
      content: ' *';
      color: var(--color-error, #dc2626);
    }

    .read-only-value {
      padding: 0.625rem 0.75rem;
      background: var(--color-bg);
      border-radius: 6px;
      font-size: 0.875rem;
      color: var(--color-text-secondary);
    }

    .form-select,
    .form-textarea {
      width: 100%;
      padding: 0.625rem 0.75rem;
      border: 1px solid var(--color-border);
      border-radius: 6px;
      font-size: 0.875rem;
      color: var(--color-text-primary);
      background: white;
      transition: all 0.2s;
    }

    .form-select:hover,
    .form-textarea:hover {
      border-color: var(--color-accent);
    }

    .form-select:focus,
    .form-textarea:focus {
      outline: none;
      border-color: var(--color-accent);
      box-shadow: 0 0 0 3px rgba(14, 165, 160, 0.1);
    }

    .form-textarea {
      resize: vertical;
      font-family: inherit;
    }

    .form-hint {
      font-size: 0.75rem;
      color: var(--color-text-secondary);
      margin-top: 0.25rem;
    }

    .error-banner {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      background: rgba(220, 38, 38, 0.1);
      border: 1px solid rgba(220, 38, 38, 0.3);
      border-radius: 6px;
      color: var(--color-error, #dc2626);
      font-size: 0.875rem;
      margin-top: 1rem;
    }

    .error-banner svg {
      flex-shrink: 0;
      margin-top: 0.125rem;
    }

    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      padding: 1.5rem;
      border-top: 1px solid var(--color-border);
    }

    .btn {
      padding: 0.625rem 1.25rem;
      border-radius: 8px;
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      border: none;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-secondary {
      background: var(--color-bg);
      color: var(--color-text-primary);
      border: 1px solid var(--color-border);
    }

    .btn-secondary:hover:not(:disabled) {
      background: var(--color-border);
    }

    .btn-primary {
      background: var(--color-accent, #0EA5A0);
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background: var(--color-accent-hover, #0C9490);
      box-shadow: 0 4px 12px rgba(14, 165, 160, 0.28);
      transform: translateY(-1px);
    }

    .btn-primary:active:not(:disabled) {
      transform: translateY(0);
    }

    @media (max-width: 640px) {
      .dialog-container {
        max-width: 100%;
        max-height: 100vh;
        border-radius: 0;
      }
    }
  `]
})
export class AppointmentEditDialogComponent implements OnInit {
  @Input() appointment!: Appointment;
  @Output() submitted = new EventEmitter<Appointment>();
  @Output() cancelled = new EventEmitter<void>();

  selectedStatus: AppointmentStatus = AppointmentStatus.SCHEDULED;
  notes = '';
  isSubmitting = false;
  errorMessage = '';

  statusOptions: StatusOption[] = [];

  constructor(private appointmentService: AppointmentApiService) {}

  ngOnInit(): void {
    this.selectedStatus = this.appointment.status;
    this.initializeStatusOptions();
  }

  private initializeStatusOptions(): void {
    // Map status values to translated labels
    this.statusOptions = [
      {
        value: AppointmentStatus.SCHEDULED,
        label: 'Scheduled',
        disabled: false
      },
      {
        value: AppointmentStatus.CONFIRMED,
        label: 'Confirmed',
        disabled: false
      },
      {
        value: AppointmentStatus.IN_PROGRESS,
        label: 'In Progress',
        disabled: false
      },
      {
        value: AppointmentStatus.COMPLETED,
        label: 'Completed',
        disabled: false
      },
      {
        value: AppointmentStatus.NO_SHOW,
        label: 'No Show',
        disabled: false
      },
      {
        value: AppointmentStatus.CANCELLED,
        label: 'Cancelled (use Cancel action)',
        disabled: true // Cannot set to cancelled via this dialog
      }
    ];
  }

  formatDateTime(isoString: string): string {
    const date = new Date(isoString);
    return date.toLocaleString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }

  isFormValid(): boolean {
    return this.selectedStatus !== null && this.selectedStatus !== this.appointment.status;
  }

  onSubmit(): void {
    if (!this.isFormValid()) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    this.appointmentService
      .updateAppointmentStatus(this.appointment.id, {
        status: this.selectedStatus,
        notes: this.notes || undefined
      })
      .subscribe({
        next: (updatedAppointment) => {
          this.isSubmitting = false;
          this.submitted.emit(updatedAppointment);
        },
        error: (error) => {
          console.error('Failed to update appointment:', error);
          this.isSubmitting = false;
          this.errorMessage =
            error.error?.message || 'Failed to update appointment. Please try again.';
        }
      });
  }

  onCancel(): void {
    if (!this.isSubmitting) {
      this.cancelled.emit();
    }
  }
}
