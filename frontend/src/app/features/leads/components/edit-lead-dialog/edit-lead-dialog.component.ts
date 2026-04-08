import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import {
  ContactMethod,
  ConvertLeadResponse,
  LeadDetail,
  LeadStatus,
  LEAD_STATUS_LABELS,
} from '../../models/lead.model';
import { LeadService } from '../../services/lead.service';
import { ConvertLeadDialogComponent } from '../convert-lead-dialog/convert-lead-dialog.component';

/**
 * Modal dialog for editing an existing lead's details.
 *
 * Status changes are handled separately (via transitionStatus).
 * This dialog manages: fullName, contactMethods, source, and notes.
 *
 * Emits `updated` with the refreshed LeadDetail on success,
 * or `cancelled` when dismissed without saving.
 */
@Component({
  selector: 'app-edit-lead-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslocoPipe, ConvertLeadDialogComponent],
  template: `
    <!-- Nested convert dialog (shown when user clicks Convert to Client) -->
    <app-convert-lead-dialog
      *ngIf="showConvertDialog"
      [lead]="lead"
      (converted)="onConverted($event)"
      (cancelled)="showConvertDialog = false">
    </app-convert-lead-dialog>

    <div class="dialog-overlay" *ngIf="!showConvertDialog"
         role="dialog" aria-modal="true" aria-labelledby="edit-lead-title">
      <div class="dialog">
        <h2 id="edit-lead-title">Edit Lead</h2>
        <p class="sub">
          <span class="status-badge" [class]="'status-' + lead.status.toLowerCase()">
            {{ statusLabels[lead.status] }}
          </span>
        </p>

        <!-- Convert to Client action (QUALIFIED only) -->
        <div *ngIf="lead.status === 'QUALIFIED'" class="convert-banner">
          <span>This lead is qualified and ready to convert.</span>
          <button type="button" class="btn-convert" (click)="openConvertDialog()">
            Convert to Client
          </button>
        </div>

        <!-- Already-converted notice -->
        <div *ngIf="lead.status === 'CONVERTED'" class="converted-banner">
          <span>This lead has been converted to a client.</span>
          <a *ngIf="lead.convertedClientId"
             [href]="'/clients/' + lead.convertedClientId"
             class="client-link">
            View client record
          </a>
        </div>

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
                      [attr.aria-label]="'Remove contact ' + (i + 1)">✕</button>
            </div>

            <button type="button" class="btn-add-contact" (click)="addContactMethod()">
              + Add contact method
            </button>
          </fieldset>

          <div class="field">
            <label for="source">{{ 'leads.create.sourceLabel' | transloco }}</label>
            <input id="source" type="text" formControlName="source" [placeholder]="'leads.create.sourcePlaceholder' | transloco" />
          </div>

          <div class="field">
            <label for="notes">{{ 'leads.create.notesLabel' | transloco }}</label>
            <textarea id="notes" formControlName="notes" rows="3"></textarea>
          </div>

          <!-- Status transition (non-terminal leads only) -->
          <div class="field" *ngIf="availableTransitions.length > 0">
            <label for="statusTransition">Move to status</label>
            <select id="statusTransition" [(ngModel)]="selectedTransition"
                    [ngModelOptions]="{standalone: true}">
              <option value="">— keep current —</option>
              <option *ngFor="let s of availableTransitions" [value]="s">{{ statusLabels[s] }}</option>
            </select>
          </div>

          <div *ngIf="serverError" class="alert-error" role="alert">{{ serverError }}</div>

          <div class="actions">
            <button type="button" (click)="cancel()" [disabled]="saving">{{ 'common.actions.cancel' | transloco }}</button>
            <button type="submit" [disabled]="saving">
              {{ saving ? 'Saving…' : 'Save changes' }}
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
    h2 { margin: 0 0 .25rem; font-size: 1.25rem; }
    .sub { margin: 0 0 1.5rem; }
    .status-badge {
      display: inline-block; padding: .2rem .6rem;
      border-radius: 999px; font-size: .8rem; font-weight: 500;
      background: #e2e8f0; color: #2d3748;
    }
    .status-new { background: #EFF6FF; color: #1D4ED8; }
    .status-contacted { background: #F0FDF4; color: #15803D; }
    .status-qualified { background: #FFFBEB; color: #B45309; }
    .status-converted { background: #F0FDF4; color: #166534; }
    .status-inactive { background: #F1F5F9; color: #64748B; }
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
    .convert-banner {
      display: flex; align-items: center; justify-content: space-between;
      padding: .75rem 1rem; background: #FFFBEB; border: 1px solid #FCD34D;
      border-radius: 8px; margin-bottom: 1.25rem; font-size: .875rem; color: #92400E;
    }
    .btn-convert {
      padding: .4rem 1rem; background: #0EA5A0; color: #fff;
      border: none; border-radius: 6px; cursor: pointer; font-size: .875rem; font-weight: 600;
    }
    .btn-convert:hover { background: #0C9490; }
    .converted-banner {
      display: flex; align-items: center; justify-content: space-between;
      padding: .75rem 1rem; background: #F0FDF4; border: 1px solid #86EFAC;
      border-radius: 8px; margin-bottom: 1.25rem; font-size: .875rem; color: #166534;
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
export class EditLeadDialogComponent implements OnInit {
  /** The lead being edited (pre-populates the form). */
  @Input() lead!: LeadDetail;

  @Output() updated = new EventEmitter<LeadDetail>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() converted = new EventEmitter<ConvertLeadResponse>();

  form!: FormGroup;
  saving = false;
  serverError: string | null = null;
  selectedTransition: LeadStatus | '' = '';
  showConvertDialog = false;

  readonly statusLabels = LEAD_STATUS_LABELS;

  /** Forward transitions available from the current status. */
  get availableTransitions(): LeadStatus[] {
    const map: Record<LeadStatus, LeadStatus[]> = {
      NEW: ['CONTACTED', 'INACTIVE'],
      CONTACTED: ['QUALIFIED', 'INACTIVE'],
      QUALIFIED: ['CONVERTED', 'INACTIVE'],
      CONVERTED: [],
      INACTIVE: [],
    };
    return map[this.lead.status] ?? [];
  }

  constructor(private fb: FormBuilder, private leadService: LeadService) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      fullName: [this.lead.fullName, [Validators.required, Validators.maxLength(255)]],
      contactMethods: this.fb.array(
        this.lead.contactMethods.map(cm => this.contactMethodGroup(cm))
      ),
      source: [this.lead.source ?? '', Validators.maxLength(100)],
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

    const raw = this.form.value;
    const payload = {
      fullName: raw.fullName,
      contactMethods: raw.contactMethods,
      source: raw.source || undefined,
      notes: raw.notes || undefined,
    };

    const update$ = this.leadService.updateLead(this.lead.id, payload);

    update$.subscribe({
      next: (updatedLead) => {
        if (this.selectedTransition) {
          this.leadService.transitionStatus(updatedLead.id, { status: this.selectedTransition }).subscribe({
            next: (transitioned) => {
              this.saving = false;
              this.updated.emit(transitioned);
            },
            error: (err: HttpErrorResponse) => {
              this.saving = false;
              this.serverError = err.error?.code === 'INVALID_STATUS_TRANSITION'
                ? 'This status transition is not allowed.'
                : 'Lead updated but status change failed. Please retry.';
              this.updated.emit(updatedLead);
            }
          });
        } else {
          this.saving = false;
          this.updated.emit(updatedLead);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.serverError = err.status === 400
          ? 'Please check the form fields and try again.'
          : 'Failed to update lead. Please try again.';
      }
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  openConvertDialog(): void {
    this.showConvertDialog = true;
  }

  onConverted(response: ConvertLeadResponse): void {
    this.showConvertDialog = false;
    this.converted.emit(response);
  }

  private contactMethodGroup(cm: ContactMethod): FormGroup {
    return this.fb.group({
      type: [cm.type],
      value: [cm.value, [Validators.required, Validators.maxLength(255)]],
      isPrimary: [cm.isPrimary],
    });
  }
}
