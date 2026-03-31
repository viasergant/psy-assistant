import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { DatePicker } from 'primeng/datepicker';
import { Subject, takeUntil, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs';
import {
  Appointment,
  RescheduleAppointmentRequest,
  ConflictingAppointment,
  ConflictCheckResponse
} from '../../models/schedule.model';
import { AppointmentApiService } from '../../services/appointment-api.service';

/**
 * Modal dialog for rescheduling an existing appointment with conflict detection
 */
@Component({
  selector: 'app-appointment-reschedule-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe, DatePicker],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" [attr.aria-labelledby]="'reschedule-title'">
      <div class="dialog" @fadeInUp>
        <div class="dialog-header">
          <h2 id="reschedule-title">{{ 'schedule.appointment.reschedule.title' | transloco }}</h2>
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
          <!-- Current Time Display -->
          <div class="current-time-card">
            <div class="card-label">{{ 'schedule.appointment.reschedule.currentTimeLabel' | transloco }}</div>
            <div class="card-value">{{ formatTime(appointment.startTime) }}</div>
            <div class="card-meta">{{ appointment.durationMinutes }} minutes</div>
          </div>

          <!-- New Date & Time -->
          <div class="field">
            <label for="newStartTime">
              {{ 'schedule.appointment.reschedule.newTimeLabel' | transloco }}
              <span class="required" aria-hidden="true">*</span>
            </label>
            <p-datepicker
              inputId="newStartTime"
              formControlName="newStartTime"
              [placeholder]="'schedule.appointment.reschedule.newTimePlaceholder' | transloco"
              [showTime]="true"
              [showSeconds]="false"
              [stepMinute]="15"
              hourFormat="24"
              dateFormat="dd/mm/yy"
              [minDate]="minDate"
              [attr.aria-invalid]="isInvalid('newStartTime')"
              aria-required="true"
              styleClass="w-full"
            />
            <span *ngIf="isInvalid('newStartTime')" class="error-msg" role="alert">
              <ng-container *ngIf="form.get('newStartTime')?.hasError('required')">
                {{ 'schedule.appointment.validation.timeRequired' | transloco }}
              </ng-container>
              <ng-container *ngIf="form.get('newStartTime')?.hasError('pastDate')">
                {{ 'schedule.appointment.validation.pastDateError' | transloco }}
              </ng-container>
            </span>
          </div>

          <!-- Reason -->
          <div class="field">
            <label for="reason">
              {{ 'schedule.appointment.reschedule.reasonLabel' | transloco }}
              <span class="required" aria-hidden="true">*</span>
            </label>
            <textarea
              id="reason"
              formControlName="reason"
              rows="4"
              [placeholder]="'schedule.appointment.reschedule.reasonPlaceholder' | transloco"
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

          <!-- Conflict Warning -->
          <div *ngIf="checkingConflicts" class="conflict-status checking" role="status">
            <div class="spinner"></div>
            <span>{{ 'schedule.appointment.booking.checkingConflicts' | transloco }}</span>
          </div>

          <div *ngIf="conflicts.length > 0 && !checkingConflicts" class="conflict-warning" role="alert">
            <div class="conflict-header">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M12 9v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
              <strong>{{ 'schedule.appointment.reschedule.conflictsDetected' | transloco }}</strong>
            </div>
            <p class="conflict-description">{{ 'schedule.appointment.reschedule.conflictWarning' | transloco }}</p>
            <ul class="conflict-list">
              <li *ngFor="let conflict of conflicts">
                <strong>{{ conflict.clientName || 'Client ID: ' + conflict.clientId }}</strong>
                — {{ formatTime(conflict.startTime) }} ({{ conflict.durationMinutes }} min)
              </li>
            </ul>
            <div class="override-checkbox">
              <input
                type="checkbox"
                id="allowOverride"
                formControlName="allowConflictOverride"
                [attr.aria-describedby]="'override-help'"
              />
              <label for="allowOverride">
                {{ 'schedule.appointment.reschedule.overrideLabel' | transloco }}
              </label>
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
              class="btn-primary"
              [disabled]="saving || !form.valid || (conflicts.length > 0 && !form.value.allowConflictOverride)"
            >
              <span *ngIf="!saving">{{ 'schedule.appointment.reschedule.rescheduleButton' | transloco }}</span>
              <span *ngIf="saving">{{ 'schedule.appointment.reschedule.reschedulingInProgress' | transloco }}</span>
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

    @keyframes spin {
      to { transform: rotate(360deg); }
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
      width: 580px;
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
      background: linear-gradient(to bottom, #FFFFFF, #FAFBFC);
    }

    h2 {
      margin: 0;
      font-size: 1.375rem;
      font-weight: 700;
      color: var(--color-text-primary, #0F172A);
      letter-spacing: -0.01em;
    }

    .close-btn {
      padding: 0.5rem;
      border: none;
      background: transparent;
      color: var(--color-text-secondary, #64748B);
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .close-btn:hover {
      background: var(--color-border, #E2E8F0);
      color: var(--color-text-primary, #0F172A);
    }

    form {
      padding: 2rem;
      overflow-y: auto;
      flex: 1;
    }

    .current-time-card {
      background: linear-gradient(135deg, var(--color-accent, #0EA5A0) 0%, #0C9490 100%);
      border-radius: 10px;
      padding: 1.25rem 1.5rem;
      margin-bottom: 1.5rem;
      color: white;
      box-shadow: 0 4px 12px rgba(14, 165, 160, 0.25);
    }

    .card-label {
      font-size: 0.8125rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      opacity: 0.85;
      margin-bottom: 0.5rem;
    }

    .card-value {
      font-size: 1.5rem;
      font-weight: 700;
      letter-spacing: -0.02em;
      margin-bottom: 0.25rem;
    }

    .card-meta {
      font-size: 0.875rem;
      opacity: 0.75;
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

    .conflict-status {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 1.25rem;
      background: #F8FAFC;
      border: 1.5px solid #E2E8F0;
      border-radius: 8px;
      margin-bottom: 1.25rem;
      font-size: 0.9375rem;
      color: var(--color-text-secondary, #64748B);
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2px solid #E2E8F0;
      border-top-color: var(--color-accent, #0EA5A0);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .conflict-warning {
      background: #FFF4E6;
      border: 1.5px solid #FFD088;
      border-radius: 10px;
      padding: 1.25rem;
      margin-bottom: 1.25rem;
      animation: slideIn 0.3s ease;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(-8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .conflict-header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 0.75rem;
      color: #D97706;
    }

    .conflict-header svg {
      flex-shrink: 0;
    }

    .conflict-description {
      margin: 0 0 0.875rem 0;
      font-size: 0.875rem;
      color: #92400E;
    }

    .conflict-list {
      margin: 0 0 1rem 1.5rem;
      padding: 0;
      list-style: disc;
      font-size: 0.875rem;
      color: #92400E;
    }

    .conflict-list li {
      margin-bottom: 0.5rem;
    }

    .override-checkbox {
      display: flex;
      align-items: start;
      gap: 0.75rem;
      padding: 0.875rem;
      background: rgba(255, 255, 255, 0.6);
      border-radius: 6px;
    }

    .override-checkbox input[type="checkbox"] {
      margin-top: 0.125rem;
      width: 18px;
      height: 18px;
      cursor: pointer;
      accent-color: var(--color-accent, #0EA5A0);
    }

    .override-checkbox label {
      margin: 0;
      font-size: 0.875rem;
      font-weight: 500;
      color: #92400E;
      cursor: pointer;
      flex: 1;
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
    .btn-primary {
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

    .btn-primary {
      background: var(--color-accent, #0EA5A0);
      color: #FFFFFF;
    }

    .btn-primary:hover:not(:disabled) {
      background: var(--color-accent-hover, #0C9490);
      box-shadow: 0 4px 12px rgba(14, 165, 160, 0.28);
      transform: translateY(-1px);
    }

    .btn-primary:active:not(:disabled) {
      transform: translateY(0);
    }

    .btn-secondary:disabled,
    .btn-primary:disabled {
      opacity: 0.55;
      cursor: not-allowed;
      transform: none !important;
    }

    /* PrimeNG overrides */
    :host ::ng-deep {
      .p-calendar {
        width: 100%;
      }

      .p-calendar .p-inputtext {
        padding: 0.75rem 1rem;
        border: 1.5px solid var(--color-border, #E2E8F0);
        border-radius: 8px;
        font-size: 0.9375rem;
        transition: all 0.2s ease;
      }

      .p-calendar:not(.p-disabled):hover .p-inputtext {
        border-color: #CBD5E1;
      }

      .p-calendar:not(.p-disabled).p-focus .p-inputtext {
        border-color: var(--color-accent, #0EA5A0);
        box-shadow: 0 0 0 3px rgba(14, 165, 160, 0.15);
      }
    }
  `]
})
export class AppointmentRescheduleDialogComponent implements OnInit, OnDestroy {
  @Input() appointment!: Appointment;
  @Output() submitted = new EventEmitter<Appointment>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  conflicts: ConflictingAppointment[] = [];
  checkingConflicts = false;
  saving = false;
  serverError: string | null = null;
  minDate = new Date();

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentApiService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.setupConflictDetection();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      newStartTime: ['', [Validators.required, this.pastDateValidator.bind(this)]],
      reason: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
      allowConflictOverride: [false]
    });
  }

  private pastDateValidator(control: any) {
    if (!control.value) return null;
    const selectedDate = new Date(control.value);
    const now = new Date();
    return selectedDate < now ? { pastDate: true } : null;
  }

  private setupConflictDetection(): void {
    this.form.get('newStartTime')?.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged(),
        switchMap(() => this.checkForConflicts()),
        takeUntil(this.destroy$)
      )
      .subscribe();
  }

  private checkForConflicts() {
    const newStartTime = this.form.value.newStartTime;

    if (!newStartTime || this.form.get('newStartTime')?.invalid) {
      this.conflicts = [];
      return new Subject<void>().asObservable();
    }

    this.checkingConflicts = true;
    this.conflicts = [];

    const isoStartTime = new Date(newStartTime).toISOString();

    return this.appointmentService.checkConflicts({
      therapistProfileId: this.appointment.therapistProfileId,
      startTime: isoStartTime,
      durationMinutes: this.appointment.durationMinutes
    }).pipe(
      takeUntil(this.destroy$),
      tap({
        next: (response: ConflictCheckResponse) => {
          this.checkingConflicts = false;
          // Filter out the current appointment from conflicts
          this.conflicts = (response.conflicts || [])
            .filter(c => c.id !== this.appointment.id);
          if (this.conflicts.length > 0) {
            this.form.patchValue({ allowConflictOverride: false }, { emitEvent: false });
          }
        },
        error: () => {
          this.checkingConflicts = false;
        }
      })
    );
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

    if (this.conflicts.length > 0 && !this.form.value.allowConflictOverride) {
      return;
    }

    this.saving = true;
    this.serverError = null;

    const request: RescheduleAppointmentRequest = {
      newStartTime: new Date(this.form.value.newStartTime).toISOString(),
      reason: this.form.value.reason,
      allowConflictOverride: this.form.value.allowConflictOverride || false
    };

    this.appointmentService.rescheduleAppointment(this.appointment.id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (appointment) => {
          this.saving = false;
          this.submitted.emit(appointment);
        },
        error: (err: HttpErrorResponse) => {
          this.saving = false;
          this.serverError = err.error?.message || 'Failed to reschedule appointment. Please try again.';
        }
      });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
