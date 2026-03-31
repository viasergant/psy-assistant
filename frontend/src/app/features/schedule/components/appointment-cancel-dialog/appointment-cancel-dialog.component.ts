import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { DropdownModule } from 'primeng/dropdown';
import { Subject, takeUntil } from 'rxjs';
import {
  Appointment,
  CancelAppointmentRequest,
  CancellationType
} from '../../models/schedule.model';
import { AppointmentApiService } from '../../services/appointment-api.service';

/**
 * Modal dialog for cancelling an appointment (destructive action)
 */
@Component({
  selector: 'app-appointment-cancel-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe, DropdownModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" [attr.aria-labelledby]="'cancel-title'">
      <div class="dialog" @fadeInUp>
        <div class="dialog-header warning">
          <div class="header-content">
            <div class="warning-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                <path d="M12 9v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
            </div>
            <h2 id="cancel-title">{{ 'schedule.appointment.cancel.title' | transloco }}</h2>
          </div>
          <button
            type="button"
            class="close-btn"
            (click)="cancel()"
            [attr.aria-label]="'common.actions.close' | transloco"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 5L5 15M5 5l10 10" />
            </svg>
          </button>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <!-- Appointment Details -->
          <div class="appointment-card">
            <div class="card-row">
              <div class="card-label">{{ 'schedule.appointment.cancel.appointmentTimeLabel' | transloco }}</div>
              <div class="card-value">{{ formatTime(appointment.startTime) }}</div>
            </div>
            <div class="card-row">
              <div class="card-label">{{ 'schedule.appointment.cancel.clientLabel' | transloco }}</div>
              <div class="card-value">{{ appointment.clientId }}</div>
            </div>
            <div class="card-row">
              <div class="card-label">Duration</div>
              <div class="card-value">{{ appointment.durationMinutes }} minutes</div>
            </div>
          </div>

          <!-- Warning Message -->
          <div class="warning-message" role="alert">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M12 9v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
            <p>{{ 'schedule.appointment.cancel.confirmationMessage' | transloco }}</p>
          </div>

          <!-- Cancellation Type -->
          <div class="field">
            <label for="cancellationType">
              {{ 'schedule.appointment.cancel.cancellationTypeLabel' | transloco }}
              <span class="required" aria-hidden="true">*</span>
            </label>
            <p-dropdown
              inputId="cancellationType"
              formControlName="cancellationType"
              [options]="cancellationTypes"
              optionLabel="label"
              optionValue="value"
              [placeholder]="'schedule.appointment.cancel.cancellationTypePlaceholder' | transloco"
              [showClear]="false"
              [attr.aria-invalid]="isInvalid('cancellationType')"
              aria-required="true"
              styleClass="w-full"
            />
            <span *ngIf="isInvalid('cancellationType')" class="error-msg" role="alert">
              {{ 'schedule.appointment.validation.cancellationTypeRequired' | transloco }}
            </span>
          </div>

          <!-- Reason -->
          <div class="field">
            <label for="reason">
              {{ 'schedule.appointment.cancel.reasonLabel' | transloco }}
              <span class="required" aria-hidden="true">*</span>
            </label>
            <textarea
              id="reason"
              formControlName="reason"
              rows="4"
              [placeholder]="'schedule.appointment.cancel.reasonPlaceholder' | transloco"
              minlength="10"
              maxlength="1000"
              [attr.aria-invalid]="isInvalid('reason')"
              aria-required="true"
            ></textarea>
            <div class="field-footer">
              <span *ngIf="isInvalid('reason')" class="error-msg" role="alert">
                <ng-container *ngIf="form.get('reason')?.hasError('required')">
                  {{ 'schedule.appointment.validation.reasonRequired' | transloco }}
                </ng-container>
                <ng-container *ngIf="form.get('reason')?.hasError('minlength')">
                  {{ 'schedule.appointment.validation.reasonMinLength' | transloco }}
                </ng-container>
                <ng-container *ngIf="form.get('reason')?.hasError('maxlength')">
                  {{ 'schedule.appointment.validation.reasonMaxLength' | transloco }}
                </ng-container>
              </span>
              <span class="char-count" [class.over-limit]="form.value.reason?.length > 1000">
                {{ form.value.reason?.length || 0 }} / 1000
              </span>
            </div>
          </div>

          <!-- Server Error -->
          <div *ngIf="serverError" class="alert-error" role="alert">{{ serverError }}</div>

          <!-- Actions -->
          <div class="actions">
            <button type="button" class="btn-secondary" (click)="cancel()" [disabled]="saving">
              {{ 'common.actions.cancel' | transloco }}
            </button>
            <button
              type="submit"
              class="btn-danger"
              [disabled]="saving || !form.valid"
            >
              <span *ngIf="!saving">{{ 'schedule.appointment.cancel.cancelButton' | transloco }}</span>
              <span *ngIf="saving">{{ 'schedule.appointment.cancel.cancellingInProgress' | transloco }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(20px) scale(0.96);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .dialog-overlay {
      position: fixed;
      inset: 0;
      background: rgba(20, 30, 43, 0.65);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.2s ease;
    }

    .dialog {
      background: var(--color-surface, #FFFFFF);
      border-radius: 12px;
      padding: 0;
      width: 560px;
      max-width: 95vw;
      max-height: 90vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-shadow: 0 24px 48px rgba(0, 0, 0, 0.18), 0 8px 16px rgba(0, 0, 0, 0.12);
      animation: fadeInUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    }

    .dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.75rem 2rem;
      border-bottom: 1px solid var(--color-border, #E2E8F0);
    }

    .dialog-header.warning {
      background: linear-gradient(135deg, #FEF2F2 0%, #FEE2E2 100%);
      border-bottom-color: #FECACA;
    }

    .header-content {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .warning-icon {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: linear-gradient(135deg, #DC2626 0%, #B91C1C 100%);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 12px rgba(220, 38, 38, 0.35);
    }

    h2 {
      margin: 0;
      font-size: 1.375rem;
      font-weight: 700;
      color: #991B1B;
      letter-spacing: -0.01em;
    }

    .close-btn {
      padding: 0.5rem;
      border: none;
      background: transparent;
      color: #DC2626;
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .close-btn:hover {
      background: rgba(220, 38, 38, 0.1);
    }

    form {
      padding: 2rem;
      overflow-y: auto;
      flex: 1;
    }

    .appointment-card {
      background: #F8FAFC;
      border: 1.5px solid var(--color-border, #E2E8F0);
      border-radius: 10px;
      padding: 1.25rem 1.5rem;
      margin-bottom: 1.5rem;
    }

    .card-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.5rem 0;
    }

    .card-row:not(:last-child) {
      border-bottom: 1px solid #E2E8F0;
    }

    .card-label {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--color-text-secondary, #64748B);
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .card-value {
      font-size: 0.9375rem;
      font-weight: 600;
      color: var(--color-text-primary, #0F172A);
    }

    .warning-message {
      display: flex;
      align-items: start;
      gap: 0.875rem;
      padding: 1.125rem 1.25rem;
      background: #FFF4E6;
      border: 1.5px solid #FFD088;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      color: #92400E;
    }

    .warning-message svg {
      flex-shrink: 0;
      margin-top: 0.125rem;
    }

    .warning-message p {
      margin: 0;
      font-size: 0.9375rem;
      font-weight: 500;
      line-height: 1.5;
    }

    .field {
      display: flex;
      flex-direction: column;
      margin-bottom: 1.25rem;
    }

    label {
      font-weight: 600;
      margin-bottom: 0.5rem;
      font-size: 0.875rem;
      color: var(--color-text-primary, #0F172A);
      letter-spacing: 0.01em;
    }

    .required {
      color: var(--color-error, #DC2626);
      margin-left: 2px;
    }

    textarea {
      padding: 0.75rem 1rem;
      border: 1.5px solid var(--color-border, #E2E8F0);
      border-radius: 8px;
      font-size: 0.9375rem;
      font-family: var(--font-family), sans-serif;
      color: var(--color-text-primary, #0F172A);
      background: var(--color-surface, #FFFFFF);
      outline: none;
      transition: all 0.2s ease;
      resize: vertical;
      min-height: 100px;
    }

    textarea:focus {
      border-color: var(--color-accent, #0EA5A0);
      box-shadow: 0 0 0 3px rgba(14, 165, 160, 0.15);
    }

    textarea[aria-invalid="true"] {
      border-color: var(--color-error, #DC2626);
    }

    .field-footer {
      display: flex;
      justify-content: space-between;
      align-items: start;
      margin-top: 0.375rem;
      gap: 0.75rem;
    }

    .error-msg {
      font-size: 0.8125rem;
      color: var(--color-error, #DC2626);
      font-weight: 500;
      flex: 1;
    }

    .char-count {
      font-size: 0.8125rem;
      color: var(--color-text-muted, #94A3B8);
      font-weight: 500;
      white-space: nowrap;
    }

    .char-count.over-limit {
      color: var(--color-error, #DC2626);
    }

    .alert-error {
      background: var(--color-error-bg, #FEF2F2);
      border: 1.5px solid var(--color-error, #DC2626);
      border-radius: 8px;
      padding: 1rem 1.25rem;
      margin-bottom: 1.25rem;
      font-size: 0.875rem;
      color: #991B1B;
      font-weight: 500;
    }

    .actions {
      display: flex;
      gap: 0.875rem;
      justify-content: flex-end;
      padding-top: 1.5rem;
      border-top: 1px solid var(--color-border, #E2E8F0);
      margin-top: 1.5rem;
    }

    .btn-secondary,
    .btn-danger {
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      font-size: 0.9375rem;
      font-weight: 600;
      font-family: var(--font-family), sans-serif;
      cursor: pointer;
      transition: all 0.2s ease;
      border: none;
      outline: none;
    }

    .btn-secondary {
      background: #F1F5F9;
      color: #374151;
      border: 1.5px solid var(--color-border, #E2E8F0);
    }

    .btn-secondary:hover:not(:disabled) {
      background: #E2E8F0;
    }

    .btn-danger {
      background: var(--color-error, #DC2626);
      color: #FFFFFF;
    }

    .btn-danger:hover:not(:disabled) {
      background: #B91C1C;
      box-shadow: 0 4px 12px rgba(220, 38, 38, 0.35);
      transform: translateY(-1px);
    }

    .btn-danger:active:not(:disabled) {
      transform: translateY(0);
    }

    .btn-secondary:disabled,
    .btn-danger:disabled {
      opacity: 0.55;
      cursor: not-allowed;
      transform: none !important;
    }

    /* PrimeNG overrides */
    :host ::ng-deep {
      .p-dropdown {
        width: 100%;
      }

      .p-dropdown .p-inputtext {
        padding: 0.75rem 1rem;
        border: 1.5px solid var(--color-border, #E2E8F0);
        border-radius: 8px;
        font-size: 0.9375rem;
        transition: all 0.2s ease;
      }

      .p-dropdown:not(.p-disabled):hover .p-inputtext {
        border-color: #CBD5E1;
      }

      .p-dropdown:not(.p-disabled).p-focus .p-inputtext {
        border-color: var(--color-accent, #0EA5A0);
        box-shadow: 0 0 0 3px rgba(14, 165, 160, 0.15);
      }
    }
  `]
})
export class AppointmentCancelDialogComponent implements OnInit, OnDestroy {
  @Input() appointment!: Appointment;
  @Output() submitted = new EventEmitter<Appointment>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  cancellationTypes: Array<{value: CancellationType; label: string}> = [];
  saving = false;
  serverError: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentApiService,
    private translocoService: TranslocoPipe
  ) {}

  ngOnInit(): void {
    this.initializeCancellationTypes();
    this.initializeForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeCancellationTypes(): void {
    this.cancellationTypes = [
      {
        value: CancellationType.CLIENT_INITIATED,
        label: 'Client Initiated'
      },
      {
        value: CancellationType.THERAPIST_INITIATED,
        label: 'Therapist Initiated'
      },
      {
        value: CancellationType.LATE_CANCELLATION,
        label: 'Late Cancellation'
      }
    ];
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      cancellationType: ['', Validators.required],
      reason: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
    });
  }

  formatTime(isoString: string): string {
    return new Date(isoString).toLocaleString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  isInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field?.invalid && (field?.dirty || field?.touched));
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
      return;
    }

    this.saving = true;
    this.serverError = null;

    const request: CancelAppointmentRequest = {
      cancellationType: this.form.value.cancellationType,
      reason: this.form.value.reason
    };

    this.appointmentService.cancelAppointment(this.appointment.id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (appointment) => {
          this.saving = false;
          this.submitted.emit(appointment);
        },
        error: (err: HttpErrorResponse) => {
          this.saving = false;
          if (err.status === 400 && err.error?.message?.includes('already cancelled')) {
            this.serverError = 'This appointment has already been cancelled.';
          } else {
            this.serverError = err.error?.message || 'Failed to cancel appointment. Please try again.';
          }
        }
      });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
