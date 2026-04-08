import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { Invoice } from '../../models/invoice.model';
import { Payment, PaymentMethod, RegisterPaymentRequest } from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';

function maxAmountValidator(max: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    const val = parseFloat(control.value);
    if (!isNaN(val) && val > max) {
      return { maxAmount: { max } };
    }
    return null;
  };
}

@Component({
  selector: 'app-register-payment-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true"
         [attr.aria-label]="'billing.payment.registerPayment' | transloco">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="register-payment-title">{{ 'billing.payment.registerPayment' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="cancel()"
                  [attr.aria-label]="'common.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="dialog-content"
              aria-labelledby="register-payment-title">

          <!-- Outstanding balance info -->
          <div class="field">
            <label class="form-label">{{ 'billing.payment.outstanding' | transloco }}</label>
            <span class="amount-display">{{ invoice.outstandingBalance | number:'1.2-2' }}</span>
          </div>

          <!-- Amount -->
          <div class="field">
            <label for="payment-amount" class="form-label">
              {{ 'billing.payment.amount' | transloco }} <span class="required">*</span>
            </label>
            <input id="payment-amount" type="number" step="0.01" min="0.01"
                   formControlName="amount" class="form-input"
                   [attr.aria-describedby]="form.get('amount')?.invalid && form.get('amount')?.touched ? 'amount-error' : null" />
            <span *ngIf="form.get('amount')?.hasError('required') && form.get('amount')?.touched"
                  id="amount-error" class="error-msg" role="alert">
              {{ 'billing.payment.validation.amountRequired' | transloco }}
            </span>
            <span *ngIf="form.get('amount')?.hasError('min') && form.get('amount')?.touched"
                  id="amount-error" class="error-msg" role="alert">
              {{ 'billing.payment.validation.amountPositive' | transloco }}
            </span>
            <span *ngIf="form.get('amount')?.hasError('maxAmount') && form.get('amount')?.touched"
                  id="amount-error" class="error-msg" role="alert">
              {{ 'billing.payment.validation.amountExceedsOutstanding' | transloco }}
            </span>
          </div>

          <!-- Payment method -->
          <div class="field">
            <label for="payment-method" class="form-label">
              {{ 'billing.payment.method' | transloco }} <span class="required">*</span>
            </label>
            <select id="payment-method" formControlName="paymentMethod" class="form-select"
                    [attr.aria-describedby]="form.get('paymentMethod')?.invalid && form.get('paymentMethod')?.touched ? 'method-error' : null">
              <option value="" disabled>— {{ 'billing.payment.method' | transloco }} —</option>
              <option *ngFor="let m of paymentMethods" [value]="m">
                {{ 'billing.payment.methods.' + m | transloco }}
              </option>
            </select>
            <span *ngIf="form.get('paymentMethod')?.invalid && form.get('paymentMethod')?.touched"
                  id="method-error" class="error-msg" role="alert">
              {{ 'billing.payment.validation.methodRequired' | transloco }}
            </span>
          </div>

          <!-- Payment date -->
          <div class="field">
            <label for="payment-date" class="form-label">
              {{ 'billing.payment.date' | transloco }} <span class="required">*</span>
            </label>
            <input id="payment-date" type="date" formControlName="paymentDate" class="form-input"
                   [attr.aria-describedby]="form.get('paymentDate')?.invalid && form.get('paymentDate')?.touched ? 'date-error' : null" />
            <span *ngIf="form.get('paymentDate')?.invalid && form.get('paymentDate')?.touched"
                  id="date-error" class="error-msg" role="alert">
              {{ 'billing.payment.validation.dateRequired' | transloco }}
            </span>
          </div>

          <!-- Reference (optional) -->
          <div class="field">
            <label for="payment-reference" class="form-label">
              {{ 'billing.payment.reference' | transloco }}
            </label>
            <input id="payment-reference" type="text" formControlName="reference" class="form-input"
                   [placeholder]="'billing.payment.referencePlaceholder' | transloco"
                   maxlength="255" />
            <span *ngIf="form.get('reference')?.hasError('maxlength')"
                  class="error-msg" role="alert">
              {{ 'billing.payment.validation.referenceMaxLength' | transloco }}
            </span>
          </div>

          <!-- Notes (optional) -->
          <div class="field">
            <label for="payment-notes" class="form-label">
              {{ 'billing.payment.notes' | transloco }}
            </label>
            <textarea id="payment-notes" formControlName="notes" class="form-textarea" rows="3"></textarea>
          </div>

          <!-- Error -->
          <div *ngIf="submitError" class="alert-error" role="alert">{{ submitError }}</div>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="cancel()" [disabled]="loading">
              {{ 'common.cancel' | transloco }}
            </button>
            <button type="submit" class="btn-primary" [disabled]="form.invalid || loading">
              <span *ngIf="loading">{{ 'common.saving' | transloco }}</span>
              <span *ngIf="!loading">{{ 'billing.payment.registerPayment' | transloco }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class RegisterPaymentDialogComponent implements OnInit {
  @Input({ required: true }) invoice!: Invoice;
  @Output() paymentRegistered = new EventEmitter<Payment>();
  @Output() dialogClosed = new EventEmitter<void>();

  form!: FormGroup;
  loading = false;
  submitError: string | null = null;
  readonly paymentMethods: PaymentMethod[] = ['CASH', 'BANK_TRANSFER', 'CARD'];

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      amount: [
        null,
        [
          Validators.required,
          Validators.min(0.01),
          maxAmountValidator(this.invoice.outstandingBalance),
        ],
      ],
      paymentMethod: ['', Validators.required],
      paymentDate: [new Date().toISOString().slice(0, 10), Validators.required],
      reference: ['', Validators.maxLength(255)],
      notes: [''],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.submitError = null;
    const req: RegisterPaymentRequest = {
      amount: parseFloat(this.form.value.amount),
      paymentMethod: this.form.value.paymentMethod,
      paymentDate: this.form.value.paymentDate,
      reference: this.form.value.reference || undefined,
      notes: this.form.value.notes || undefined,
    };
    this.paymentService.registerPayment(this.invoice.id, req).subscribe({
      next: (payment) => {
        this.loading = false;
        this.paymentRegistered.emit(payment);
      },
      error: (err) => {
        this.loading = false;
        this.submitError = err?.error?.message ?? 'billing.payment.error.registerFailed';
      },
    });
  }

  cancel(): void {
    this.dialogClosed.emit();
  }
}
