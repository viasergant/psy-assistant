import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  ContactMethodType,
  ConversionErrorBody,
  ConvertLeadResponse,
  LeadDetail,
} from '../../models/lead.model';
import { LeadService } from '../../services/lead.service';

/**
 * Modal dialog for converting a QUALIFIED lead into a client record.
 *
 * Pre-populates the form from the lead's current data.
 * Emits `converted` with the ConvertLeadResponse on success,
 * or `cancelled` when dismissed without saving.
 *
 * Error surfaces:
 * - 409 LEAD_ALREADY_CONVERTED: inline error with optional "View client record" link
 * - 422 INVALID_STATUS_TRANSITION: inline message
 * - Other: generic inline message
 */
@Component({
  selector: 'app-convert-lead-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true"
         aria-labelledby="convert-lead-title">
      <div class="dialog">
        <h2 id="convert-lead-title">Convert Lead to Client</h2>
        <p class="sub">
          Converting <strong>{{ lead.fullName }}</strong> to a permanent client record.
          All activity history will be preserved.
        </p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>

          <div class="field">
            <label for="fullName">Full name <span aria-hidden="true">*</span></label>
            <input
              id="fullName"
              type="text"
              formControlName="fullName"
              [attr.aria-invalid]="isInvalid('fullName')"
              aria-required="true"
            />
            <span *ngIf="isInvalid('fullName')" class="error-msg" role="alert">
              Full name is required.
            </span>
          </div>

          <!-- Contact methods -->
          <fieldset formArrayName="contactMethods">
            <legend>Contact methods <span aria-hidden="true">*</span></legend>
            <div *ngIf="contactMethods.touched && contactMethods.length === 0"
                 class="error-msg" role="alert">
              At least one contact method is required.
            </div>

            <div *ngFor="let cm of contactMethods.controls; let i = index"
                 [formGroupName]="i" class="contact-row">
              <select formControlName="type" [attr.aria-label]="'leads.create.contactTypeAriaLabel' | transloco">
                <option value="EMAIL">{{ 'leads.create.contactTypeEmail' | transloco }}</option>
                <option value="PHONE">{{ 'leads.create.contactTypePhone' | transloco }}</option>
              </select>
              <input
                type="text"
                formControlName="value"
                [attr.aria-label]="'Contact value ' + (i + 1)"
                placeholder="email or phone"
              />
              <label class="primary-label">
                <input type="checkbox" formControlName="isPrimary" />
                Primary
              </label>
              <button type="button" class="btn-remove" (click)="removeContactMethod(i)"
                      [attr.aria-label]="'Remove contact ' + (i + 1)">&#x2715;</button>
            </div>

            <button type="button" class="btn-add-contact" (click)="addContactMethod()">
              + Add contact method
            </button>
          </fieldset>

          <div class="field">
            <label for="notes">{{ 'leads.convert.notesLabel' | transloco }}</label>
            <textarea id="notes" formControlName="notes" rows="3"
                      [placeholder]="'leads.convert.notesPlaceholder' | transloco"></textarea>
          </div>

          <!-- Error display -->
          <div *ngIf="serverError" class="alert-error" role="alert">
            <span>{{ serverError }}</span>
            <a *ngIf="existingClientId"
               [href]="'/clients/' + existingClientId"
               class="client-link">
              View existing client record
            </a>
          </div>

          <div class="actions">
            <button type="button" (click)="cancel()" [disabled]="saving">{{ 'common.actions.cancel' | transloco }}</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? 'Converting…' : 'Convert to Client' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,.45);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000;
    }
    .dialog {
      background: #fff; border-radius: 8px;
      padding: 2rem; width: 480px; max-width: 95vw; max-height: 90vh; overflow-y: auto;
      box-shadow: 0 4px 24px rgba(0,0,0,.15);
    }
    h2 { margin: 0 0 .5rem; font-size: 1.25rem; }
    .sub { margin: 0 0 1.5rem; color: #4B5563; font-size: .9375rem; }
    .field { display: flex; flex-direction: column; margin-bottom: 1rem; }
    label { font-weight: 500; margin-bottom: .25rem; }
    input[type="text"], textarea, select {
      appearance: none; -webkit-appearance: none;
      padding: .6rem .875rem; border: 1.5px solid #D1D5DB;
      border-radius: 8px; font-size: .9375rem; font-family: inherit;
      color: #0F172A; background: #fff; outline: none;
      transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    input:focus, textarea:focus, select:focus {
      border-color: #0EA5A0; box-shadow: 0 0 0 3px rgba(14,165,160,.15);
    }
    input[aria-invalid="true"] { border-color: #DC2626; }
    textarea { resize: vertical; }
    .error-msg { color: #DC2626; font-size: .8125rem; margin-top: .25rem; }
    fieldset {
      border: 1.5px solid #E2E8F0; border-radius: 8px;
      padding: 1rem; margin-bottom: 1rem;
    }
    legend { font-weight: 500; padding: 0 .5rem; }
    .contact-row {
      display: flex; gap: .5rem; align-items: center; margin-bottom: .5rem;
    }
    .contact-row select { width: 90px; flex-shrink: 0; }
    .contact-row input[type="text"] { flex: 1; }
    .primary-label {
      display: flex; align-items: center; gap: .25rem;
      font-size: .8125rem; font-weight: 400; white-space: nowrap; cursor: pointer;
    }
    .primary-label input[type="checkbox"] { width: 14px; height: 14px; }
    .btn-remove {
      background: none; border: none; cursor: pointer;
      color: #9CA3AF; font-size: 1rem; padding: .25rem;
    }
    .btn-remove:hover { color: #DC2626; }
    .btn-add-contact {
      background: none; border: 1.5px dashed #D1D5DB; border-radius: 8px;
      padding: .4rem 1rem; cursor: pointer; font-size: .875rem;
      color: #374151; width: 100%; margin-top: .25rem;
    }
    .btn-add-contact:hover { border-color: #0EA5A0; color: #0EA5A0; }
    .alert-error {
      padding: .75rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; margin-bottom: 1rem; font-size: .875rem;
      display: flex; flex-direction: column; gap: .4rem;
    }
    .client-link {
      color: #1D4ED8; text-decoration: underline; font-size: .8125rem;
    }
    .client-link:hover { color: #1e40af; }
    .actions { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.5rem; }
    button {
      padding: .6rem 1.25rem; border-radius: 8px; border: none;
      cursor: pointer; font-size: .9375rem; font-family: inherit; font-weight: 500;
      transition: background 0.15s ease, box-shadow 0.15s ease;
    }
    button[type="submit"] { background: #0EA5A0; color: #fff; }
    button[type="submit"]:hover:not(:disabled) {
      background: #0C9490; box-shadow: 0 4px 12px rgba(14,165,160,.28);
    }
    .actions button[type="button"] { background: #F1F5F9; color: #374151; border: 1.5px solid #E2E8F0; }
    .actions button[type="button"]:hover:not(:disabled) { background: #E2E8F0; }
    button:disabled { opacity: .55; cursor: not-allowed; }
  `]
})
export class ConvertLeadDialogComponent implements OnInit {
  /** The QUALIFIED lead to convert. */
  @Input() lead!: LeadDetail;

  @Output() converted = new EventEmitter<ConvertLeadResponse>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  serverError: string | null = null;
  /** Set when 409 returns an existingClientId. */
  existingClientId: string | null = null;

  constructor(private fb: FormBuilder, private leadService: LeadService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      fullName: [this.lead.fullName, [Validators.required, Validators.maxLength(255)]],
      contactMethods: this.fb.array(
        this.lead.contactMethods.map(cm => this.fb.group({
          type: [cm.type],
          value: [cm.value, [Validators.required, Validators.maxLength(255)]],
          isPrimary: [cm.isPrimary],
        }))
      ),
      notes: [this.lead.notes ?? ''],
    });
  }

  get contactMethods(): FormArray {
    return this.form.get('contactMethods') as FormArray;
  }

  addContactMethod(): void {
    this.contactMethods.push(this.fb.group({
      type: ['EMAIL'],
      value: ['', [Validators.required, Validators.maxLength(255)]],
      isPrimary: [false],
    }));
  }

  removeContactMethod(index: number): void {
    this.contactMethods.removeAt(index);
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.contactMethods.length === 0) return;

    this.saving = true;
    this.serverError = null;
    this.existingClientId = null;

    const raw = this.form.value;
    const payload = {
      fullName: raw.fullName as string,
      contactMethods: (raw.contactMethods as { type: string; value: string; isPrimary: boolean }[]).map(cm => ({
        ...cm,
        type: cm.type as ContactMethodType,
      })),
      ...(raw.notes ? { notes: raw.notes as string } : {}),
    };

    this.leadService.convertLead(this.lead.id, payload).subscribe({
      next: (response: ConvertLeadResponse) => {
        this.saving = false;
        this.converted.emit(response);
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        if (err.status === 409) {
          const body = err.error as ConversionErrorBody;
          this.serverError = 'This lead has already been converted to a client.';
          this.existingClientId = body?.existingClientId ?? null;
        } else if (err.status === 422) {
          this.serverError = 'Only leads with status Qualified may be converted.';
        } else if (err.status === 400) {
          this.serverError = 'Please check the form fields and try again.';
        } else {
          this.serverError = 'Conversion failed. Please try again.';
        }
      }
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
