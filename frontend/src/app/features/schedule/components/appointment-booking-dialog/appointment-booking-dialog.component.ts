import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { Subject, takeUntil, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import {
  Appointment,
  CheckRecurringConflictsRequest,
  ConflictCheckResponse,
  ConflictingAppointment,
  ConflictResolution,
  CreateAppointmentRequest,
  CreateRecurringSeriesRequest,
  CreateRecurringSeriesResponse,
  RecurrenceType,
  RecurringConflictCheckResponse,
  ScheduleSummary,
  SessionType
} from '../../models/schedule.model';
import { AppointmentApiService } from '../../services/appointment-api.service';
import {
  RecurringConflictReviewDialogComponent
} from '../recurring-conflict-review-dialog/recurring-conflict-review-dialog.component';

interface ClientOption {
  id: string;
  name: string;
}

/**
 * Modal dialog for booking a new appointment with conflict detection
 */
@Component({
  selector: 'app-appointment-booking-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoPipe,
    DatePicker,
    Select,
    RecurringConflictReviewDialogComponent
  ],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" [attr.aria-labelledby]="'booking-title'">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="booking-title">{{ 'schedule.appointment.booking.title' | transloco }}</h2>
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
          <div class="form-grid">
            <!-- Client Selection -->
            <div class="field">
              <label for="client">
                {{ 'schedule.appointment.booking.clientLabel' | transloco }}
                <span class="required" aria-hidden="true">*</span>
              </label>
              <p-select
                inputId="client"
                formControlName="clientId"
                [options]="clients"
                optionLabel="name"
                optionValue="id"
                [placeholder]="'schedule.appointment.booking.clientPlaceholder' | transloco"
                [showClear]="false"
                [attr.aria-invalid]="isInvalid('clientId')"
                aria-required="true"
                styleClass="w-full"
                appendTo="body"
              />
              <span *ngIf="isInvalid('clientId')" class="error-msg" role="alert">
                {{ 'schedule.appointment.validation.clientRequired' | transloco }}
              </span>
            </div>

            <!-- Session Type Selection -->
            <div class="field">
              <label for="sessionType">
                {{ 'schedule.appointment.booking.sessionTypeLabel' | transloco }}
                <span class="required" aria-hidden="true">*</span>
              </label>
              <p-select
                inputId="sessionType"
                formControlName="sessionTypeId"
                [options]="sessionTypes"
                optionLabel="name"
                optionValue="id"
                [placeholder]="'schedule.appointment.booking.sessionTypePlaceholder' | transloco"
                [showClear]="false"
                [attr.aria-invalid]="isInvalid('sessionTypeId')"
                aria-required="true"
                styleClass="w-full"
                appendTo="body"
              />
              <span *ngIf="isInvalid('sessionTypeId')" class="error-msg" role="alert">
                {{ 'schedule.appointment.validation.sessionTypeRequired' | transloco }}
              </span>
            </div>

            <!-- Date & Time -->
            <div class="field">
              <label for="startTime">
                {{ 'schedule.appointment.booking.dateLabel' | transloco }}
                <span class="required" aria-hidden="true">*</span>
              </label>
              <p-datepicker
                inputId="startTime"
                formControlName="startTime"
                [placeholder]="'schedule.appointment.booking.datePlaceholder' | transloco"
                [showTime]="true"
                [showSeconds]="false"
                [stepMinute]="15"
                hourFormat="24"
                dateFormat="dd/mm/yy"
                [minDate]="minDate"
                [attr.aria-invalid]="isInvalid('startTime')"
                aria-required="true"
                styleClass="w-full"
                appendTo="body"
              />
              <span *ngIf="isInvalid('startTime')" class="error-msg" role="alert">
                <ng-container *ngIf="form.get('startTime')?.hasError('required')">
                  {{ 'schedule.appointment.validation.dateRequired' | transloco }}
                </ng-container>
                <ng-container *ngIf="form.get('startTime')?.hasError('pastDate')">
                  {{ 'schedule.appointment.validation.pastDateError' | transloco }}
                </ng-container>
              </span>
            </div>

            <!-- Duration -->
            <div class="field">
              <label for="duration">
                {{ 'schedule.appointment.booking.durationLabel' | transloco }}
                <span class="required" aria-hidden="true">*</span>
              </label>
              <input
                id="duration"
                type="number"
                formControlName="durationMinutes"
                [placeholder]="'schedule.appointment.booking.durationPlaceholder' | transloco"
                min="15"
                max="480"
                step="15"
                [attr.aria-invalid]="isInvalid('durationMinutes')"
                aria-required="true"
              />
              <span *ngIf="isInvalid('durationMinutes')" class="error-msg" role="alert">
                <ng-container *ngIf="form.get('durationMinutes')?.hasError('required')">
                  {{ 'schedule.appointment.validation.durationRequired' | transloco }}
                </ng-container>
                <ng-container *ngIf="form.get('durationMinutes')?.hasError('min')">
                  {{ 'schedule.appointment.validation.durationMin' | transloco }}
                </ng-container>
                <ng-container *ngIf="form.get('durationMinutes')?.hasError('max')">
                  {{ 'schedule.appointment.validation.durationMax' | transloco }}
                </ng-container>
              </span>
            </div>
          </div>

          <!-- Notes (full width) -->
          <div class="field">
            <label for="notes">{{ 'schedule.appointment.booking.notesLabel' | transloco }}</label>
            <textarea
              id="notes"
              formControlName="notes"
              rows="3"
              [placeholder]="'schedule.appointment.booking.notesPlaceholder' | transloco"
            ></textarea>
          </div>

          <!-- Recurring Appointment Toggle -->
          <div class="recurrence-section">
            <div class="recurrence-toggle">
              <input
                type="checkbox"
                id="isRecurring"
                formControlName="isRecurring"
                aria-label="Book as recurring appointment series"
              />
              <label for="isRecurring" class="toggle-label">
                {{ 'schedule.appointment.booking.recurringLabel' | transloco }}
              </label>
            </div>

            <div *ngIf="form.value.isRecurring" class="recurrence-options" role="group" aria-label="Recurrence options">
              <!-- Recurrence Type -->
              <div class="field recurrence-field">
                <label>
                  {{ 'schedule.appointment.booking.recurrenceTypeLabel' | transloco }}
                  <span class="required" aria-hidden="true">*</span>
                </label>
                <div class="radio-group" role="radiogroup" aria-label="Recurrence type">
                  <label class="radio-option" *ngFor="let type of recurrenceTypes">
                    <input
                      type="radio"
                      formControlName="recurrenceType"
                      [value]="type.value"
                      [attr.aria-label]="type.label"
                    />
                    {{ type.label }}
                  </label>
                </div>
              </div>

              <!-- Occurrences Slider -->
              <div class="field recurrence-field">
                <label for="occurrences">
                  {{ 'schedule.appointment.booking.occurrencesLabel' | transloco }}
                  ({{ form.value.occurrences }})
                  <span class="required" aria-hidden="true">*</span>
                </label>
                <input
                  id="occurrences"
                  type="range"
                  formControlName="occurrences"
                  min="1"
                  max="20"
                  step="1"
                  class="slider"
                  aria-label="Number of occurrences"
                  aria-valuemin="1"
                  aria-valuemax="20"
                  [attr.aria-valuenow]="form.value.occurrences"
                />
                <div class="slider-labels">
                  <span>1</span>
                  <span>10</span>
                  <span>20</span>
                </div>
              </div>

              <!-- Preview & Check Conflicts Button -->
              <button
                type="button"
                class="btn-preview"
                (click)="previewRecurringConflicts()"
                [disabled]="checkingRecurringConflicts || !canPreviewConflicts()"
                aria-label="Preview recurring slots and check for conflicts"
              >
                <span *ngIf="!checkingRecurringConflicts">
                  {{ 'schedule.appointment.booking.previewConflictsButton' | transloco }}
                </span>
                <span *ngIf="checkingRecurringConflicts" class="spinner-inline"></span>
              </button>
            </div>
          </div>

          <!-- Recurring Conflict Review Dialog (inline) -->
          <app-recurring-conflict-review-dialog
            *ngIf="showConflictReview && recurringConflictResponse"
            [response]="recurringConflictResponse"
            (resolved)="onConflictResolved($event)"
            (cancelled)="onConflictReviewCancelled()"
          ></app-recurring-conflict-review-dialog>

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
              <strong>{{ 'schedule.appointment.booking.conflictsDetected' | transloco }}</strong>
            </div>
            <p class="conflict-description">{{ 'schedule.appointment.booking.conflictWarning' | transloco }}</p>
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
                {{ 'schedule.appointment.booking.overrideLabel' | transloco }}
              </label>
            </div>
          </div>

          <!-- Outside Working Hours Warning -->
          <div *ngIf="isOutsideWorkingHours && !checkingConflicts" class="warning-message" role="alert">
            <div class="warning-header">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M12 9v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
              <strong>{{ 'schedule.appointment.booking.outsideWorkingHoursWarning' | transloco }}</strong>
            </div>
            <p class="warning-description">{{ 'schedule.appointment.booking.outsideWorkingHoursMessage' | transloco }}</p>
            <div class="override-checkbox">
              <input
                type="checkbox"
                id="confirmOutsideHours"
                formControlName="confirmOutsideHours"
              />
              <label for="confirmOutsideHours">
                {{ 'schedule.appointment.booking.confirmOverrideLabel' | transloco }}
              </label>
            </div>
          </div>

          <!-- During Leave Warning -->
          <div *ngIf="isDuringLeave && !checkingConflicts" class="warning-message leave-warning" role="alert">
            <div class="warning-header">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M12 9v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
              <strong>{{ 'schedule.appointment.booking.duringLeaveWarning' | transloco }}</strong>
            </div>
            <p class="warning-description">{{ 'schedule.appointment.booking.duringLeaveMessage' | transloco }}</p>
            <div *ngIf="!isOutsideWorkingHours" class="override-checkbox">
              <input
                type="checkbox"
                id="confirmOutsideHoursLeave"
                formControlName="confirmOutsideHours"
              />
              <label for="confirmOutsideHoursLeave">
                {{ 'schedule.appointment.booking.confirmOverrideLabel' | transloco }}
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
              [disabled]="saving || !form.valid || (conflicts.length > 0 && !form.value.allowConflictOverride) || ((isOutsideWorkingHours || isDuringLeave) && !form.value.confirmOutsideHours)"
            >
              <span *ngIf="!saving">{{ 'schedule.appointment.booking.bookButton' | transloco }}</span>
              <span *ngIf="saving">{{ 'schedule.appointment.booking.bookingInProgress' | transloco }}</span>
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

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .dialog {
      background: var(--color-surface, #FFFFFF);
      border-radius: 12px;
      padding: 0;
      width: 640px;
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

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.25rem;
      margin-bottom: 1.25rem;
    }

    .field {
      display: flex;
      flex-direction: column;
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

    input[type='number'],
    input[type='text'],
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
    }

    input:focus,
    textarea:focus {
      border-color: var(--color-accent, #0EA5A0);
      box-shadow: 0 0 0 3px rgba(14, 165, 160, 0.15);
    }

    input[aria-invalid="true"],
    textarea[aria-invalid="true"] {
      border-color: var(--color-error, #DC2626);
    }

    textarea {
      resize: vertical;
      min-height: 80px;
    }

    .error-msg {
      margin-top: 0.375rem;
      font-size: 0.8125rem;
      color: var(--color-error, #DC2626);
      font-weight: 500;
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

    .warning-message {
      background: linear-gradient(135deg, #FEF9C3 0%, #FDE68A 100%);
      border: 1.5px solid #F59E0B;
      border-radius: 10px;
      padding: 1.25rem;
      margin-bottom: 1.25rem;
      animation: slideDown 0.3s ease-out;
    }

    .warning-message.leave-warning {
      background: linear-gradient(135deg, #FEE2E2 0%, #FECACA 100%);
      border-color: #EF4444;
    }

    .warning-header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-bottom: 0.75rem;
      color: #D97706;
    }

    .leave-warning .warning-header {
      color: #DC2626;
    }

    .warning-header svg {
      flex-shrink: 0;
    }

    .warning-description {
      margin: 0 0 0.875rem 0;
      font-size: 0.875rem;
      color: #92400E;
    }

    .leave-warning .warning-description {
      color: #991B1B;
    }

    .leave-warning .override-checkbox label {
      color: #991B1B;
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

    /* Recurrence section */
    .recurrence-section {
      margin-bottom: 1.25rem;
    }

    .recurrence-toggle {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem 1.25rem;
      background: #F8FAFC;
      border: 1.5px solid var(--color-border, #E2E8F0);
      border-radius: 8px;
      cursor: pointer;
    }

    .recurrence-toggle input[type="checkbox"] {
      width: 18px;
      height: 18px;
      cursor: pointer;
      accent-color: var(--color-accent, #0EA5A0);
    }

    .toggle-label {
      font-weight: 600;
      font-size: 0.9375rem;
      color: var(--color-text-primary, #0F172A);
      cursor: pointer;
      margin: 0;
    }

    .recurrence-options {
      margin-top: 1rem;
      padding: 1.25rem;
      background: #F8FAFC;
      border: 1.5px solid var(--color-border, #E2E8F0);
      border-radius: 8px;
      animation: slideIn 0.2s ease;
    }

    .recurrence-field {
      margin-bottom: 1rem;
    }

    .radio-group {
      display: flex;
      gap: 1.5rem;
      flex-wrap: wrap;
      margin-top: 0.5rem;
    }

    .radio-option {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 500;
      font-size: 0.9375rem;
      color: var(--color-text-primary, #0F172A);
      cursor: pointer;
    }

    .radio-option input[type="radio"] {
      width: 16px;
      height: 16px;
      accent-color: var(--color-accent, #0EA5A0);
      cursor: pointer;
    }

    .slider {
      width: 100%;
      appearance: none;
      height: 6px;
      background: #E2E8F0;
      border-radius: 9999px;
      outline: none;
      cursor: pointer;
      margin-top: 0.5rem;
    }

    .slider::-webkit-slider-thumb {
      appearance: none;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background: var(--color-accent, #0EA5A0);
      cursor: pointer;
      box-shadow: 0 2px 6px rgba(14, 165, 160, 0.4);
      transition: box-shadow 0.2s;
    }

    .slider::-webkit-slider-thumb:hover {
      box-shadow: 0 2px 12px rgba(14, 165, 160, 0.5);
    }

    .slider-labels {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      color: var(--color-text-secondary, #64748B);
      margin-top: 0.25rem;
    }

    .btn-preview {
      padding: 0.625rem 1.25rem;
      background: #EFF6FF;
      color: #1D4ED8;
      border: 1.5px solid #BFDBFE;
      border-radius: 8px;
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .btn-preview:hover:not(:disabled) {
      background: #DBEAFE;
      border-color: #93C5FD;
    }

    .btn-preview:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .spinner-inline {
      display: inline-block;
      width: 14px;
      height: 14px;
      border: 2px solid #BFDBFE;
      border-top-color: #1D4ED8;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    /* PrimeNG overrides */
    :host ::ng-deep {
      .p-select,
      .p-dropdown,
      .p-calendar {
        width: 100%;
      }

      .p-select .p-select-label,
      .p-dropdown .p-inputtext,
      .p-calendar .p-inputtext {
        padding: 0.75rem 1rem;
        border: 1.5px solid var(--color-border, #E2E8F0);
        border-radius: 8px;
        font-size: 0.9375rem;
        background: var(--color-surface, #FFFFFF);
        color: var(--color-text-primary, #0F172A);
        transition: all 0.2s ease;
      }

      .p-select:not(.p-disabled):hover .p-select-label,
      .p-dropdown:not(.p-disabled):hover .p-inputtext,
      .p-calendar:not(.p-disabled):hover .p-inputtext {
        border-color: #CBD5E1;
      }

      .p-select:not(.p-disabled).p-focus .p-select-label,
      .p-dropdown:not(.p-disabled).p-focus .p-inputtext,
      .p-calendar:not(.p-disabled).p-focus .p-inputtext {
        border-color: var(--color-accent, #0EA5A0);
        box-shadow: 0 0 0 3px rgba(14, 165, 160, 0.15);
      }
    }
  `]
})
export class AppointmentBookingDialogComponent implements OnInit, OnChanges, OnDestroy {
  @Input() therapistProfileId!: string;
  @Input() clients: ClientOption[] = [];
  @Input() initialDateTime?: Date;
  @Input() schedule?: ScheduleSummary; // Schedule data for working hours and leave checks
  @Output() submitted = new EventEmitter<Appointment>();
  @Output() seriesCreated = new EventEmitter<CreateRecurringSeriesResponse>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  sessionTypes: SessionType[] = [];
  conflicts: ConflictingAppointment[] = [];
  checkingConflicts = false;
  saving = false;
  serverError: string | null = null;
  minDate = new Date();

  // Working hours and leave warnings
  isOutsideWorkingHours = false;
  isDuringLeave = false;

  // Recurring series state (PA-33)
  checkingRecurringConflicts = false;
  recurringConflictResponse: RecurringConflictCheckResponse | null = null;
  showConflictReview = false;
  pendingConflictResolution: ConflictResolution | null = null;

  readonly recurrenceTypes: Array<{ value: RecurrenceType; label: string }> = [
    { value: 'WEEKLY', label: 'Weekly' },
    { value: 'BIWEEKLY', label: 'Biweekly' },
    { value: 'MONTHLY', label: 'Monthly' }
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentApiService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadSessionTypes();
    this.setupConflictDetection();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Update form when initialDateTime input changes
    if (changes['initialDateTime'] && this.form && changes['initialDateTime'].currentValue) {
      this.form.patchValue({
        startTime: changes['initialDateTime'].currentValue
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      clientId: ['', Validators.required],
      sessionTypeId: ['', Validators.required],
      startTime: [this.initialDateTime || '', [Validators.required, this.pastDateValidator.bind(this)]],
      durationMinutes: [60, [Validators.required, Validators.min(15), Validators.max(480)]],
      notes: [''],
      allowConflictOverride: [false],
      confirmOutsideHours: [false], // Confirmation for outside hours/leave
      // Recurring series fields (PA-33)
      isRecurring: [false],
      recurrenceType: ['WEEKLY'],
      occurrences: [4]
    });
  }

  private pastDateValidator(control: any) {
    if (!control.value) return null;
    const selectedDate = new Date(control.value);
    const now = new Date();
    return selectedDate < now ? { pastDate: true } : null;
  }

  private loadSessionTypes(): void {
    this.appointmentService.getSessionTypes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (types) => this.sessionTypes = types,
        error: (err) => console.error('Failed to load session types', err)
      });
  }

  private setupConflictDetection(): void {
    // Watch for changes in date, time, or duration
    this.form.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged((prev, curr) =>
          prev.startTime === curr.startTime && prev.durationMinutes === curr.durationMinutes
        ),
        switchMap(() => {
          this.checkWorkingHoursAndLeave(); // Check working hours and leave
          return this.checkForConflicts();
        }),
        takeUntil(this.destroy$)
      )
      .subscribe();
  }

  private checkForConflicts() {
    const { startTime, durationMinutes } = this.form.value;

    if (!startTime || !durationMinutes || this.form.get('startTime')?.invalid) {
      this.conflicts = [];
      return new Subject<void>().asObservable();
    }

    this.checkingConflicts = true;
    this.conflicts = [];

    const isoStartTime = new Date(startTime).toISOString();

    return this.appointmentService.checkConflicts({
      therapistProfileId: this.therapistProfileId,
      startTime: isoStartTime,
      durationMinutes
    }).pipe(
      takeUntil(this.destroy$),
      tap({
        next: (response: ConflictCheckResponse) => {
          this.checkingConflicts = false;
          this.conflicts = response.conflicts || [];
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

  /**
   * Check if selected time is outside working hours or during leave
   */
  private checkWorkingHoursAndLeave(): void {
    const { startTime } = this.form.value;
    
    if (!startTime || !this.schedule) {
      this.isOutsideWorkingHours = false;
      this.isDuringLeave = false;
      return;
    }

    const appointmentDate = new Date(startTime);
    const dateStr = this.formatDate(appointmentDate);
    const dayOfWeek = appointmentDate.getDay() === 0 ? 7 : appointmentDate.getDay(); // Convert Sunday from 0 to 7
    const timeStr = this.formatTime24(appointmentDate);

    // Check if during leave
    this.isDuringLeave = this.schedule.leavePeriods?.some(leave => 
      leave.status === 'APPROVED' &&
      dateStr >= leave.startDate &&
      dateStr <= leave.endDate
    ) || false;

    // Check if outside working hours
    // First check for date-specific override
    const override = this.schedule.overrides?.find(o => o.date === dateStr);
    if (override) {
      if (!override.available) {
        this.isOutsideWorkingHours = true;
      } else if (override.startTime && override.endTime) {
        this.isOutsideWorkingHours = !this.isTimeBetween(timeStr, override.startTime, override.endTime);
      } else {
        this.isOutsideWorkingHours = false;
      }
    } else {
      // Check recurring schedule for this day of week
      const daySchedule = this.schedule.recurringSchedule?.filter(s => s.dayOfWeek === dayOfWeek) || [];
      if (daySchedule.length === 0) {
        this.isOutsideWorkingHours = true; // No schedule for this day
      } else {
        // Check if time falls within any of the scheduled time ranges for this day
        this.isOutsideWorkingHours = !daySchedule.some(s => {
          const startTimeStr = this.normalizeTime(s.startTime);
          const endTimeStr = this.normalizeTime(s.endTime);
          return this.isTimeBetween(timeStr, startTimeStr, endTimeStr);
        });
      }
    }

    // Reset confirmation if warnings change
    if (this.isOutsideWorkingHours || this.isDuringLeave) {
      this.form.patchValue({ confirmOutsideHours: false }, { emitEvent: false });
    }
  }

  /**
   * Helper to format date as yyyy-MM-dd
   */
  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Helper to format time as HH:mm
   */
  private formatTime24(date: Date): string {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  /**
   * Normalize time from various formats to HH:mm
   */
  private normalizeTime(time: string | any): string {
    if (typeof time === 'string') {
      // Remove seconds if present: "HH:mm:ss" -> "HH:mm"
      return time.substring(0, 5);
    } else if (Array.isArray(time) && time.length >= 2) {
      // Array format [hour, minute, second]
      const hours = String(time[0]).padStart(2, '0');
      const minutes = String(time[1]).padStart(2, '0');
      return `${hours}:${minutes}`;
    }
    return '00:00';
  }

  /**
   * Check if a time is between start and end times
   */
  private isTimeBetween(time: string, start: string, end: string): boolean {
    return time >= start && time < end;
  }

  formatTime(isoString: string): string {
    return new Date(isoString).toLocaleString('en-US', {
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

  // ========== Recurring Series Methods (PA-33) ==========

  canPreviewConflicts(): boolean {
    const { startTime, durationMinutes, clientId, sessionTypeId } = this.form.value;
    return !!(startTime && durationMinutes && clientId && sessionTypeId
              && this.form.get('startTime')?.valid);
  }

  previewRecurringConflicts(): void {
    if (!this.canPreviewConflicts()) {
      return;
    }
    this.checkingRecurringConflicts = true;
    this.recurringConflictResponse = null;
    this.showConflictReview = false;

    const { startTime, durationMinutes, clientId, sessionTypeId, recurrenceType, occurrences } =
      this.form.value;

    const request: CheckRecurringConflictsRequest = {
      therapistProfileId: this.therapistProfileId,
      clientId,
      sessionTypeId,
      startTime: new Date(startTime).toISOString(),
      durationMinutes,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      recurrenceType,
      occurrences
    };

    this.appointmentService.checkRecurringConflicts(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.checkingRecurringConflicts = false;
          this.recurringConflictResponse = response;
          this.showConflictReview = true;
        },
        error: () => {
          this.checkingRecurringConflicts = false;
          this.serverError = 'Failed to check recurring conflicts. Please try again.';
        }
      });
  }

  onConflictResolved(resolution: ConflictResolution): void {
    this.showConflictReview = false;
    this.pendingConflictResolution = resolution;
    // Immediately submit with this resolution
    this.submitRecurringSeries(resolution);
  }

  onConflictReviewCancelled(): void {
    this.showConflictReview = false;
    this.recurringConflictResponse = null;
    this.pendingConflictResolution = null;
  }

  private submitRecurringSeries(resolution: ConflictResolution): void {
    this.saving = true;
    this.serverError = null;

    const { startTime, durationMinutes, clientId, sessionTypeId, recurrenceType, occurrences, notes } =
      this.form.value;

    const request: CreateRecurringSeriesRequest = {
      therapistProfileId: this.therapistProfileId,
      clientId,
      sessionTypeId,
      startTime: new Date(startTime).toISOString(),
      durationMinutes,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      recurrenceType,
      occurrences,
      notes: notes || undefined,
      conflictResolution: resolution
    };

    this.appointmentService.createRecurringSeries(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.saving = false;
          this.pendingConflictResolution = null;
          this.seriesCreated.emit(response);
        },
        error: (err: HttpErrorResponse) => {
          this.saving = false;
          this.serverError = err.error?.message || 'Failed to create recurring series. Please try again.';
        }
      });
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
      return;
    }

    // Route to recurring series flow if toggle is on
    if (this.form.value.isRecurring) {
      // Must run conflict preview first
      if (!this.recurringConflictResponse) {
        this.serverError = 'Please click "Preview & Check Conflicts" before booking.';
        return;
      }
      if (this.recurringConflictResponse.conflictCount === 0) {
        // No conflicts — proceed directly with ABORT (clean path)
        this.submitRecurringSeries('ABORT');
      } else {
        this.showConflictReview = true;
      }
      return;
    }

    if (this.conflicts.length > 0 && !this.form.value.allowConflictOverride) {
      return;
    }

    // Check if user needs to confirm outside hours/leave warning
    if ((this.isOutsideWorkingHours || this.isDuringLeave) && !this.form.value.confirmOutsideHours) {
      return;
    }

    this.saving = true;
    this.serverError = null;

    const request: CreateAppointmentRequest = {
      therapistProfileId: this.therapistProfileId,
      clientId: this.form.value.clientId,
      sessionTypeId: this.form.value.sessionTypeId,
      startTime: new Date(this.form.value.startTime).toISOString(),
      durationMinutes: this.form.value.durationMinutes,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      notes: this.form.value.notes || undefined,
      allowConflictOverride: this.form.value.allowConflictOverride || false
    };

    this.appointmentService.createAppointment(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (appointment) => {
          this.saving = false;
          this.submitted.emit(appointment);
        },
        error: (err: HttpErrorResponse) => {
          this.saving = false;
          this.serverError = err.error?.message || 'Failed to book appointment. Please try again.';
        }
      });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}

// Import tap operator
import { tap } from 'rxjs/operators';
