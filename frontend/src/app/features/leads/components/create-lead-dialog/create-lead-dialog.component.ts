import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Output } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { LeadDetail } from '../../models/lead.model';
import { LeadService } from '../../services/lead.service';

/**
 * Modal dialog for creating a new lead.
 *
 * Emits `created` with the server-returned LeadDetail on success,
 * or `cancelled` when the user dismisses without saving.
 */
@Component({
  selector: 'app-create-lead-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true" aria-labelledby="create-lead-title">
      <div class="dialog">
        <h2 id="create-lead-title">New Lead</h2>

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
              <select formControlName="type" aria-label="Contact type">
                <option value="EMAIL">Email</option>
                <option value="PHONE">Phone</option>
              </select>
              <input
                type="text"
                formControlName="value"
                [attr.aria-label]="'Contact value ' + (i + 1)"
                [attr.aria-invalid]="isCmInvalid(i, 'value')"
                placeholder="email or phone"
              />
              <label class="primary-label">
                <input type="checkbox" formControlName="isPrimary" />
                Primary
              </label>
              <button type="button" class="btn-remove" (click)="removeContactMethod(i)"
                      [attr.aria-label]="'Remove contact ' + (i + 1)">✕</button>
            </div>

            <button type="button" class="btn-add-contact" (click)="addContactMethod()">
              + Add contact method
            </button>
          </fieldset>

          <div class="field">
            <label for="source">Source</label>
            <input id="source" type="text" formControlName="source" placeholder="e.g. referral, website" />
          </div>

          <div class="field">
            <label for="notes">Notes</label>
            <textarea id="notes" formControlName="notes" rows="3" placeholder="Optional notes"></textarea>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">{{ serverError }}</div>

          <div class="actions">
            <button type="button" (click)="cancel()" [disabled]="saving">Cancel</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? 'Creating…' : 'Create lead' }}
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
    h2 { margin: 0 0 1.5rem; font-size: 1.25rem; }
    .field { display: flex; flex-direction: column; margin-bottom: 1rem; }
    label { font-weight: 500; margin-bottom: .25rem; }
    input[type="text"], input[type="email"], textarea, select {
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
    }
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
    button[type="button"].actions button, .actions button[type="button"] {
      background: #F1F5F9; color: #374151; border: 1.5px solid #E2E8F0;
    }
    .actions button[type="button"] { background: #F1F5F9; color: #374151; border: 1.5px solid #E2E8F0; }
    .actions button[type="button"]:hover:not(:disabled) { background: #E2E8F0; }
    button:disabled { opacity: .55; cursor: not-allowed; }
  `]
})
export class CreateLeadDialogComponent {
  @Output() created = new EventEmitter<LeadDetail>();
  @Output() cancelled = new EventEmitter<void>();

  form: FormGroup;
  saving = false;
  serverError: string | null = null;

  constructor(private fb: FormBuilder, private leadService: LeadService) {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(255)]],
      contactMethods: this.fb.array([this.newContactMethodGroup()]),
      source: ['', Validators.maxLength(100)],
      notes: [''],
    });
  }

  get contactMethods(): FormArray {
    return this.form.get('contactMethods') as FormArray;
  }

  addContactMethod(): void {
    this.contactMethods.push(this.newContactMethodGroup());
  }

  removeContactMethod(index: number): void {
    this.contactMethods.removeAt(index);
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  isCmInvalid(index: number, field: string): boolean {
    const ctrl = (this.contactMethods.at(index) as FormGroup).get(field);
    return !!ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.contactMethods.length === 0) return;

    this.saving = true;
    this.serverError = null;

    const raw = this.form.value;
    const payload = {
      fullName: raw.fullName,
      contactMethods: raw.contactMethods,
      ...(raw.source ? { source: raw.source } : {}),
      ...(raw.notes ? { notes: raw.notes } : {}),
    };

    this.leadService.createLead(payload).subscribe({
      next: (lead) => {
        this.saving = false;
        this.created.emit(lead);
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.serverError = err.status === 400
          ? 'Please check the form fields and try again.'
          : 'Failed to create lead. Please try again.';
      }
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  private newContactMethodGroup(): FormGroup {
    return this.fb.group({
      type: ['EMAIL'],
      value: ['', [Validators.required, Validators.maxLength(255)]],
      isPrimary: [false],
    });
  }

  isAbstractControl(ctrl: AbstractControl): ctrl is AbstractControl {
    return ctrl instanceof AbstractControl;
  }
}
