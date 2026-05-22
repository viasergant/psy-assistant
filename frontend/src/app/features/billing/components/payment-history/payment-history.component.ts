import { CommonModule } from '@angular/common';
import {
  Component,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { TableModule } from 'primeng/table';
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
    TableModule,
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
      <p-table
        [value]="payments"
        [loading]="loadingPayments"
        styleClass="p-datatable-sm"
        responsiveLayout="scroll"
      >
        <ng-template pTemplate="header">
          <tr>
            <th>{{ 'billing.payment.date' | transloco }}</th>
            <th>{{ 'billing.payment.method' | transloco }}</th>
            <th>{{ 'billing.payment.amount' | transloco }}</th>
            <th>{{ 'billing.payment.reference' | transloco }}</th>
            <th>{{ 'billing.invoices.table.actions' | transloco }}</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-p>
          <tr>
            <td>{{ p.paymentDate | date:'mediumDate' }}</td>
            <td>{{ 'billing.payment.methods.' + p.paymentMethod | transloco }}</td>
            <td>{{ p.amount | number:'1.2-2' }}</td>
            <td>{{ p.reference || '—' }}</td>
            <td>{{ p.createdBy }}</td>
          </tr>
        </ng-template>
        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="5" class="empty-msg">{{ 'billing.payment.noPayments' | transloco }}</td>
          </tr>
        </ng-template>
      </p-table>

      <!-- Refunds table -->
      <div *ngIf="refunds.length > 0" style="margin-top: var(--spacing-lg)">
        <h3 class="subsection-title">{{ 'billing.payment.refunds' | transloco }}</h3>
        <p-table
          [value]="refunds"
          styleClass="p-datatable-sm"
          responsiveLayout="scroll"
        >
          <ng-template pTemplate="header">
            <tr>
              <th>{{ 'billing.payment.date' | transloco }}</th>
              <th>{{ 'billing.payment.amount' | transloco }}</th>
              <th>{{ 'billing.payment.refundReason' | transloco }}</th>
              <th>{{ 'billing.invoices.table.actions' | transloco }}</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-r>
            <tr>
              <td>{{ r.refundDate | date:'mediumDate' }}</td>
              <td>{{ r.amount | number:'1.2-2' }}</td>
              <td>{{ r.reason }}</td>
              <td>{{ r.createdBy }}</td>
            </tr>
          </ng-template>
        </p-table>
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
