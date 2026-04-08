import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { Payment, Refund, RegisterRefundRequest } from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-register-refund-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" role="dialog" aria-modal="true"
         [attr.aria-label]="'billing.payment.registerRefund' | transloco">
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="register-refund-title">{{ 'billing.payment.registerRefund' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="cancel()"
                  [attr.aria-label]="'common.close' | transloco">✕</button>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="dialog-content"
              aria-labelledby="register-refund-title">

          <!-- Amount -->
          <div class="field">
            <label for="refund-amount" class="form-label">
              {{ 'billing.payment.amount' | transloco }} <span class="required">*</span>
            </label>
            <input id="refund-amount" type="number" step="0.01" min="0.01"
                   formControlName="amount" class="form-input" />
            <span *ngIf="form.get('amount')?.hasError('required') && form.get('amount')?.touched"
                  class="error-msg" role="alert">
              {{ 'billing.payment.validation.amountRequired' | transloco }}
            </span>
            <span *ngIf="form.get('amount')?.hasError('min') && form.get('amount')?.touched"
                  class="error-msg" role="alert">
              {{ 'billing.payment.validation.amountPositive' | transloco }}
            </span>
          </div>

          <!-- Reason -->
          <div class="field">
            <label for="refund-reason" class="form-label">
              {{ 'billing.payment.refundReason' | transloco }} <span class="required">*</span>
            </label>
            <textarea id="refund-reason" formControlName="reason" class="form-textarea" rows="3"></textarea>
            <span *ngIf="form.get('reason')?.hasError('required') && form.get('reason')?.touched"
                  class="error-msg" role="alert">
              {{ 'billing.payment.validation.reasonRequired' | transloco }}
            </span>
          </div>

          <!-- Refund date -->
          <div class="field">
            <label for="refund-date" class="form-label">
              {{ 'billing.payment.date' | transloco }} <span class="required">*</span>
            </label>
            <input id="refund-date" type="date" formControlName="refundDate" class="form-input" />
            <span *ngIf="form.get('refundDate')?.hasError('required') && form.get('refundDate')?.touched"
                  class="error-msg" role="alert">
              {{ 'billing.payment.validation.dateRequired' | transloco }}
            </span>
          </div>

          <!-- Related payment (optional) -->
          <div class="field" *ngIf="payments.length > 0">
            <label for="related-payment" class="form-label">
              {{ 'billing.payment.relatedPayment' | transloco }}
            </label>
            <select id="related-payment" formControlName="paymentId" class="form-select">
              <option [value]="null">— {{ 'billing.payment.noRelatedPayment' | transloco }} —</option>
              <option *ngFor="let p of payments" [value]="p.id">
                {{ p.paymentDate }} — {{ p.amount | number:'1.2-2' }}
                ({{ 'billing.payment.methods.' + p.paymentMethod | transloco }})
              </option>
            </select>
          </div>

          <!-- Reference (optional) -->
          <div class="field">
            <label for="refund-reference" class="form-label">
              {{ 'billing.payment.reference' | transloco }}
            </label>
            <input id="refund-reference" type="text" formControlName="reference" class="form-input"
                   maxlength="255" />
          </div>

          <!-- Error -->
          <div *ngIf="submitError" class="alert-error" role="alert">{{ submitError }}</div>

          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="cancel()" [disabled]="loading">
              {{ 'common.cancel' | transloco }}
            </button>
            <button type="submit" class="btn-primary" [disabled]="form.invalid || loading">
              <span *ngIf="loading">{{ 'common.saving' | transloco }}</span>
              <span *ngIf="!loading">{{ 'billing.payment.registerRefund' | transloco }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
})
export class RegisterRefundDialogComponent implements OnInit {
  @Input({ required: true }) invoiceId!: string;
  @Input() payments: Payment[] = [];
  @Output() refundRegistered = new EventEmitter<Refund>();
  @Output() dialogClosed = new EventEmitter<void>();

  form!: FormGroup;
  loading = false;
  submitError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      amount: [null, [Validators.required, Validators.min(0.01)]],
      reason: ['', Validators.required],
      refundDate: [new Date().toISOString().slice(0, 10), Validators.required],
      paymentId: [null],
      reference: ['', Validators.maxLength(255)],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.submitError = null;
    const req: RegisterRefundRequest = {
      amount: parseFloat(this.form.value.amount),
      reason: this.form.value.reason,
      refundDate: this.form.value.refundDate,
      paymentId: this.form.value.paymentId ?? undefined,
      reference: this.form.value.reference || undefined,
    };
    this.paymentService.registerRefund(this.invoiceId, req).subscribe({
      next: (refund) => {
        this.loading = false;
        this.refundRegistered.emit(refund);
      },
      error: (err) => {
        this.loading = false;
        this.submitError = err?.error?.message ?? 'billing.payment.error.refundFailed';
      },
    });
  }

  cancel(): void {
    this.dialogClosed.emit();
  }
}
