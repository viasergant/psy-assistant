import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Invoice } from '../../models/invoice.model';
import { InvoiceService } from '../../services/invoice.service';
import { CancelInvoiceRequest } from '../../models/invoice.model';
import { PaymentHistoryComponent } from '../payment-history/payment-history.component';

@Component({
  selector: 'app-invoice-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, TranslocoModule, PaymentHistoryComponent],
  template: `
    <div class="page-container">

      <!-- Back -->
      <a [routerLink]="['/billing/invoices']" class="back-link">
        ← {{ 'billing.invoices.actions.backToList' | transloco }}
      </a>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'billing.invoices.detail.loading' | transloco }}
      </div>
      <div *ngIf="error" class="alert-error" role="alert">{{ error }}</div>

      <ng-container *ngIf="invoice && !loading">

        <!-- Header -->
        <div class="detail-header">
          <div class="detail-title-row">
            <h1 class="page-title">{{ 'billing.invoices.detail.title' | transloco }} #{{ invoice.invoiceNumber }}</h1>
            <span class="status-badge" [ngClass]="'status-' + invoice.status.toLowerCase()">
              {{ 'billing.invoices.status.' + invoice.status | transloco }}
            </span>
          </div>
          <div class="detail-meta">
            <span>{{ 'billing.invoices.detail.createdAt' | transloco }}: {{ invoice.createdAt | date:'mediumDate' }}</span>
            <span *ngIf="invoice.issuedDate">{{ 'billing.invoices.detail.issuedDate' | transloco }}: {{ invoice.issuedDate | date:'mediumDate' }}</span>
            <span *ngIf="invoice.dueDate">{{ 'billing.invoices.detail.dueDate' | transloco }}: {{ invoice.dueDate | date:'mediumDate' }}</span>
          </div>
        </div>

        <!-- Actions bar -->
        <div class="action-bar">
          <button *ngIf="invoice.status === 'DRAFT'"
                  type="button" class="btn-primary"
                  [disabled]="actionLoading"
                  (click)="issue()">
            {{ 'billing.invoices.actions.issue' | transloco }}
          </button>
          <button *ngIf="invoice.status === 'DRAFT' || invoice.status === 'ISSUED'"
                  type="button" class="btn-danger"
                  [disabled]="actionLoading"
                  (click)="showCancelDialog = true">
            {{ 'billing.invoices.actions.cancel' | transloco }}
          </button>
          <a *ngIf="invoice.pdfPath"
             [href]="getPdfUrl()"
             target="_blank"
             class="btn-secondary">
            {{ 'billing.invoices.actions.downloadPdf' | transloco }}
          </a>
        </div>

        <!-- Line items -->
        <div class="card">
          <h2 class="card-title">{{ 'billing.invoices.detail.lineItems' | transloco }}</h2>

          <table class="data-table">
            <thead>
              <tr>
                <th scope="col">{{ 'billing.invoices.table.description' | transloco }}</th>
                <th scope="col" class="col-right">{{ 'billing.invoices.table.qty' | transloco }}</th>
                <th scope="col" class="col-right">{{ 'billing.invoices.table.unitPrice' | transloco }}</th>
                <th scope="col" class="col-right">{{ 'billing.invoices.table.lineTotal' | transloco }}</th>
                <th scope="col" *ngIf="invoice.status === 'DRAFT'"></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of invoice.lineItems">
                <td>{{ item.description }}</td>
                <td class="col-right">{{ item.quantity | number:'1.2-2' }}</td>
                <td class="col-right">{{ item.unitPrice | number:'1.2-2' }}</td>
                <td class="col-right">{{ item.lineTotal | number:'1.2-2' }}</td>
                <td *ngIf="invoice.status === 'DRAFT'">
                  <button type="button" class="btn-icon-danger"
                          [attr.aria-label]="'billing.invoices.actions.removeLineItem' | transloco"
                          (click)="removeLineItem(item.id)">✕</button>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <!-- Totals -->
              <tr class="totals-tr">
                <td colspan="2" class="totals-spacer"></td>
                <td colspan="2" class="totals-cell">
                  <div class="totals-line">
                    <span>{{ 'billing.invoices.detail.subtotal' | transloco }}</span>
                    <span>{{ invoice.subtotal | number:'1.2-2' }}</span>
                  </div>
                  <div class="totals-line" *ngIf="invoice.discount > 0">
                    <span>{{ 'billing.invoices.detail.discount' | transloco }}</span>
                    <span>- {{ invoice.discount | number:'1.2-2' }}</span>
                  </div>
                  <div class="totals-line total">
                    <span>{{ 'billing.invoices.detail.total' | transloco }}</span>
                    <span>{{ invoice.total | number:'1.2-2' }}</span>
                  </div>
                </td>
                <td *ngIf="invoice.status === 'DRAFT'"></td>
              </tr>
              <!-- Add line item (draft only) -->
              <tr *ngIf="invoice.status === 'DRAFT'" class="add-line-header-tr">
                <td colspan="5" class="add-line-label">{{ 'billing.invoices.actions.addLineItem' | transloco }}</td>
              </tr>
              <tr *ngIf="invoice.status === 'DRAFT'" class="add-line-inputs-tr">
                <td>
                  <input type="text" class="table-input"
                         [(ngModel)]="newItem.description"
                         [placeholder]="'billing.invoices.form.description' | transloco" />
                </td>
                <td>
                  <input type="number" class="table-input table-input-right"
                         [(ngModel)]="newItem.quantity"
                         min="0.01"
                         [placeholder]="'billing.invoices.form.quantity' | transloco" />
                </td>
                <td>
                  <input type="number" class="table-input table-input-right"
                         [(ngModel)]="newItem.unitPrice"
                         min="0"
                         [placeholder]="'billing.invoices.form.unitPrice' | transloco" />
                </td>
                <td colspan="2">
                  <button type="button" class="btn-primary table-add-btn"
                          [disabled]="!newItem.description || !newItem.quantity || newItem.unitPrice === undefined"
                          (click)="addLineItem()">
                    {{ 'billing.invoices.actions.addLineItem' | transloco }}
                  </button>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>

        <!-- Notes -->
        <div class="card" *ngIf="invoice.notes">
          <h2 class="card-title">{{ 'billing.invoices.detail.notes' | transloco }}</h2>
          <p class="notes-text">{{ invoice.notes }}</p>
        </div>

        <!-- Cancellation info -->
        <div class="card alert-info" *ngIf="invoice.status === 'CANCELLED' && invoice.cancellationReason">
          <h2 class="card-title">{{ 'billing.invoices.detail.cancellationReason' | transloco }}</h2>
          <p>{{ invoice.cancellationReason }}</p>
        </div>

        <!-- Payment history (shown for non-draft, non-cancelled invoices) -->
        <div class="card" *ngIf="invoice.status !== 'DRAFT' && invoice.status !== 'CANCELLED'">
          <app-payment-history [invoice]="invoice"></app-payment-history>
        </div>

      </ng-container>

      <!-- Cancel dialog -->
      <div *ngIf="showCancelDialog" class="dialog-overlay" role="dialog"
           [attr.aria-label]="'billing.invoices.cancel.title' | transloco">
        <div class="dialog">
          <h2>{{ 'billing.invoices.cancel.title' | transloco }}</h2>
          <p>{{ 'billing.invoices.cancel.description' | transloco }}</p>
          <textarea class="form-textarea"
                    [(ngModel)]="cancelReason"
                    rows="3"
                    [placeholder]="'billing.invoices.cancel.reasonPlaceholder' | transloco"
                    [attr.aria-label]="'billing.invoices.cancel.reason' | transloco">
          </textarea>
          <div class="dialog-actions">
            <button type="button" class="btn-secondary" (click)="showCancelDialog = false">
              {{ 'common.actions.cancel' | transloco }}
            </button>
            <button type="button" class="btn-danger"
                    [disabled]="!cancelReason.trim()"
                    (click)="confirmCancel()">
              {{ 'billing.invoices.actions.cancel' | transloco }}
            </button>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .page-container { padding: var(--spacing-lg); }
    .back-link { display: inline-block; color: var(--color-accent); text-decoration: none;
      margin-bottom: var(--spacing-md); font-size: 0.875rem; }
    .back-link:hover { text-decoration: underline; }
    .state-msg { padding: var(--spacing-md); color: var(--color-text-muted); }
    .alert-error { padding: var(--spacing-sm) var(--spacing-md); background: #fee2e2;
      color: #991b1b; border-radius: var(--radius-sm); margin-bottom: var(--spacing-md); }
    .detail-header { margin-bottom: var(--spacing-md); }
    .detail-title-row { display: flex; align-items: center; gap: var(--spacing-md);
      margin-bottom: var(--spacing-sm); }
    .page-title { margin: 0; font-size: 1.5rem; }
    .detail-meta { display: flex; gap: var(--spacing-lg); font-size: 0.875rem;
      color: var(--color-text-muted); }
    .action-bar { display: flex; gap: var(--spacing-sm); margin-bottom: var(--spacing-lg); }
    .card { background: var(--color-surface, #fff); border: 1px solid var(--color-border);
      border-radius: var(--radius-md); padding: var(--spacing-md); margin-bottom: var(--spacing-md); }
    .card-title { font-size: 1rem; font-weight: 600; margin: 0 0 var(--spacing-md); }
    .data-table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
    .data-table th { background: var(--color-surface-alt, #f8f9fa); padding: 8px 10px;
      text-align: left; font-weight: 600; border-bottom: 2px solid var(--color-border); }
    .data-table td { padding: 8px 10px; border-bottom: 1px solid var(--color-border); }
    .col-right { text-align: right; }
    .totals-line { display: flex; justify-content: space-between; gap: var(--spacing-sm); padding: 4px 0;
      font-size: 0.9rem; }
    .totals-line.total { font-weight: 700; font-size: 1rem; border-top: 2px solid var(--color-border);
      margin-top: 4px; padding-top: 8px; }
    .data-table tfoot td { border-bottom: none; }
    .totals-tr td { border-top: 2px solid var(--color-border); padding-top: var(--spacing-sm); }
    .add-line-header-tr td { padding-top: var(--spacing-md); border-top: 1px solid var(--color-border);
      font-size: 0.875rem; font-weight: 600; }
    .add-line-inputs-tr td { padding-top: 4px; padding-bottom: var(--spacing-sm); }
    .table-input { width: 100%; padding: 6px 8px; border: 1px solid var(--color-border);
      border-radius: var(--radius-sm); font-size: 0.875rem; box-sizing: border-box; }
    .table-input-right { text-align: right; }
    .table-add-btn { width: 100%; }
    .btn-icon-danger { background: none; border: none; color: #ef4444; cursor: pointer;
      font-size: 1rem; padding: 4px 6px; border-radius: var(--radius-sm); line-height: 1; }
    .btn-icon-danger:hover { background: #fee2e2; }
    .notes-text { margin: 0; color: var(--color-text-secondary); }
    .alert-info { background: #fef3c7; border-color: #fcd34d; }
    .status-badge { padding: 2px 8px; border-radius: var(--radius-full); font-size: 0.75rem;
      font-weight: 600; text-transform: uppercase; }
    .status-draft { background: #fef3c7; color: #92400e; }
    .status-issued { background: #dbeafe; color: #1e40af; }
    .status-overdue { background: #fee2e2; color: #991b1b; }
    .status-paid { background: #dcfce7; color: #166534; }
    .status-partially_paid { background: #fef9c3; color: #854d0e; }
    .status-cancelled { background: #f3f4f6; color: #6b7280; }
    .dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5);
      display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .dialog { background: #fff; border-radius: var(--radius-md); padding: var(--spacing-lg);
      max-width: 480px; width: 90%; }
    .dialog h2 { margin: 0 0 var(--spacing-sm); }
    .dialog p { color: var(--color-text-secondary); margin-bottom: var(--spacing-md); }
    .form-textarea { width: 100%; padding: 8px 10px; border: 1px solid var(--color-border);
      border-radius: var(--radius-sm); font-size: 0.875rem; resize: vertical;
      margin-bottom: var(--spacing-md); }
    .dialog-actions { display: flex; justify-content: flex-end; gap: var(--spacing-sm); }
  `]
})
export class InvoiceDetailComponent implements OnInit {
  invoice: Invoice | null = null;
  loading = false;
  error: string | null = null;
  actionLoading = false;

  showCancelDialog = false;
  cancelReason = '';

  newItem = { description: '', quantity: 1, unitPrice: 0 };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invoiceService: InvoiceService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadInvoice(id);
    }
  }

  loadInvoice(id: string): void {
    this.loading = true;
    this.error = null;
    this.invoiceService.getById(id).subscribe({
      next: (inv) => {
        this.invoice = inv;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load invoice';
        this.loading = false;
      }
    });
  }

  issue(): void {
    if (!this.invoice) { return; }
    this.actionLoading = true;
    this.invoiceService.issue(this.invoice.id).subscribe({
      next: (updated) => {
        this.invoice = updated;
        this.actionLoading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to issue invoice';
        this.actionLoading = false;
      }
    });
  }

  confirmCancel(): void {
    if (!this.invoice || !this.cancelReason?.trim()) { return; }
    this.actionLoading = true;
    const request: CancelInvoiceRequest = { reason: this.cancelReason.trim() };
    this.invoiceService.cancel(this.invoice.id, request).subscribe({
      next: (updated) => {
        this.invoice = updated;
        this.actionLoading = false;
        this.showCancelDialog = false;
        this.cancelReason = '';
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to cancel invoice';
        this.actionLoading = false;
        this.showCancelDialog = false;
      }
    });
  }

  addLineItem(): void {
    if (!this.invoice) { return; }
    this.invoiceService.addLineItem(this.invoice.id, {
      description: this.newItem.description,
      quantity: this.newItem.quantity,
      unitPrice: this.newItem.unitPrice
    }).subscribe({
      next: (updated) => {
        this.invoice = updated;
        this.newItem = { description: '', quantity: 1, unitPrice: 0 };
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to add line item';
      }
    });
  }

  removeLineItem(itemId: string): void {
    if (!this.invoice) { return; }
    this.invoiceService.removeLineItem(this.invoice.id, itemId).subscribe({
      next: () => {
        this.loadInvoice(this.invoice!.id);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to remove line item';
      }
    });
  }

  getPdfUrl(): string {
    return this.invoice ? this.invoiceService.getPdfUrl(this.invoice.id) : '#';
  }
}
