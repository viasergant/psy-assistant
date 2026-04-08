import { CommonModule } from '@angular/common';
import {
  Component,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { Invoice } from '../../models/invoice.model';
import { Payment, Refund } from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';
import { RegisterPaymentDialogComponent } from '../register-payment-dialog/register-payment-dialog.component';
import { RegisterRefundDialogComponent } from '../register-refund-dialog/register-refund-dialog.component';

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoModule,
    RegisterPaymentDialogComponent,
    RegisterRefundDialogComponent,
  ],
  template: `
    <section aria-labelledby="payment-history-heading">
      <div class="section-header">
        <h2 id="payment-history-heading" class="section-title">
          {{ 'billing.payment.paymentHistory' | transloco }}
        </h2>
        <div class="action-bar">
          <button *ngIf="canRegisterPayment"
                  type="button" class="btn-primary btn-sm"
                  (click)="showPaymentDialog = true">
            {{ 'billing.payment.registerPayment' | transloco }}
          </button>
          <button *ngIf="canRegisterRefund"
                  type="button" class="btn-secondary btn-sm"
                  (click)="showRefundDialog = true">
            {{ 'billing.payment.registerRefund' | transloco }}
          </button>
        </div>
      </div>

      <!-- Outstanding balance summary -->
      <div class="summary-row" *ngIf="invoice">
        <span class="summary-label">{{ 'billing.payment.totalPaid' | transloco }}:</span>
        <span class="summary-value">{{ invoice.paidAmount | number:'1.2-2' }}</span>
        <span class="summary-separator">|</span>
        <span class="summary-label">{{ 'billing.payment.outstanding' | transloco }}:</span>
        <span class="summary-value">{{ invoice.outstandingBalance | number:'1.2-2' }}</span>
      </div>

      <!-- Payments table -->
      <div *ngIf="loadingPayments" class="state-msg" aria-live="polite">
        {{ 'common.loading' | transloco }}
      </div>

      <div *ngIf="!loadingPayments">
        <div *ngIf="payments.length === 0" class="empty-msg">
          {{ 'billing.payment.noPayments' | transloco }}
        </div>

        <div *ngIf="payments.length > 0" class="table-wrapper">
          <table aria-label="{{ 'billing.payment.paymentHistory' | transloco }}">
            <thead>
              <tr>
                <th scope="col">{{ 'billing.payment.date' | transloco }}</th>
                <th scope="col">{{ 'billing.payment.method' | transloco }}</th>
                <th scope="col">{{ 'billing.payment.amount' | transloco }}</th>
                <th scope="col">{{ 'billing.payment.reference' | transloco }}</th>
                <th scope="col">{{ 'billing.invoices.table.actions' | transloco }}</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of payments">
                <td>{{ p.paymentDate | date:'mediumDate' }}</td>
                <td>{{ 'billing.payment.methods.' + p.paymentMethod | transloco }}</td>
                <td>{{ p.amount | number:'1.2-2' }}</td>
                <td>{{ p.reference || '—' }}</td>
                <td>{{ p.createdBy }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Refunds section -->
      <div *ngIf="refunds.length > 0" class="table-wrapper" style="margin-top: var(--spacing-lg)">
        <h3 class="subsection-title">{{ 'billing.payment.refunds' | transloco }}</h3>
        <table aria-label="{{ 'billing.payment.refunds' | transloco }}">
          <thead>
            <tr>
              <th scope="col">{{ 'billing.payment.date' | transloco }}</th>
              <th scope="col">{{ 'billing.payment.amount' | transloco }}</th>
              <th scope="col">{{ 'billing.payment.refundReason' | transloco }}</th>
              <th scope="col">{{ 'billing.invoices.table.actions' | transloco }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let r of refunds">
              <td>{{ r.refundDate | date:'mediumDate' }}</td>
              <td>{{ r.amount | number:'1.2-2' }}</td>
              <td>{{ r.reason }}</td>
              <td>{{ r.createdBy }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Dialogs -->
    <app-register-payment-dialog
      *ngIf="showPaymentDialog && invoice"
      [invoice]="invoice"
      (paymentRegistered)="onPaymentRegistered($event)"
      (dialogClosed)="showPaymentDialog = false">
    </app-register-payment-dialog>

    <app-register-refund-dialog
      *ngIf="showRefundDialog && invoice"
      [invoiceId]="invoice.id"
      [payments]="payments"
      (refundRegistered)="onRefundRegistered($event)"
      (dialogClosed)="showRefundDialog = false">
    </app-register-refund-dialog>
  `,
})
export class PaymentHistoryComponent implements OnInit, OnChanges {
  @Input({ required: true }) invoice!: Invoice;

  payments: Payment[] = [];
  refunds: Refund[] = [];
  loadingPayments = false;
  showPaymentDialog = false;
  showRefundDialog = false;

  constructor(private paymentService: PaymentService) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['invoice'] && !changes['invoice'].firstChange) {
      this.load();
    }
  }

  get canRegisterPayment(): boolean {
    return (
      this.invoice.status === 'ISSUED' ||
      this.invoice.status === 'OVERDUE' ||
      this.invoice.status === 'PARTIALLY_PAID'
    );
  }

  get canRegisterRefund(): boolean {
    return this.invoice.status === 'PAID';
  }

  load(): void {
    this.loadingPayments = true;
    this.paymentService.getPayments(this.invoice.id).subscribe({
      next: (payments) => {
        this.payments = payments;
        this.loadingPayments = false;
      },
      error: () => {
        this.loadingPayments = false;
      },
    });
    this.paymentService.getRefunds(this.invoice.id).subscribe({
      next: (refunds) => (this.refunds = refunds),
    });
  }

  onPaymentRegistered(payment: Payment): void {
    this.showPaymentDialog = false;
    this.payments = [...this.payments, payment];
    // Reload invoice to get updated status and balances
    this.invoice = {
      ...this.invoice,
      status: payment.invoiceStatus,
      outstandingBalance: payment.invoiceOutstandingBalance,
      paidAmount: this.invoice.total - payment.invoiceOutstandingBalance,
    };
  }

  onRefundRegistered(refund: Refund): void {
    this.showRefundDialog = false;
    this.refunds = [...this.refunds, refund];
  }
}
