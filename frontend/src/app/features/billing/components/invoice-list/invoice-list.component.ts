import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { InvoiceListItem, InvoiceStatus } from '../../models/invoice.model';
import { InvoiceService } from '../../services/invoice.service';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">{{ 'billing.invoices.title' | transloco }}</h1>
        <button type="button" class="btn-primary" [routerLink]="['/billing/invoices/new']">
          + {{ 'billing.invoices.actions.create' | transloco }}
        </button>
      </div>

      <!-- Filters -->
      <div class="filter-bar">
        <select class="filter-select"
                [(ngModel)]="statusFilter"
                (ngModelChange)="applyFilter()"
                [attr.aria-label]="'billing.invoices.filters.status' | transloco">
          <option value="">{{ 'billing.invoices.filters.allStatuses' | transloco }}</option>
          <option value="DRAFT">{{ 'billing.invoices.status.DRAFT' | transloco }}</option>
          <option value="ISSUED">{{ 'billing.invoices.status.ISSUED' | transloco }}</option>
          <option value="OVERDUE">{{ 'billing.invoices.status.OVERDUE' | transloco }}</option>
          <option value="PAID">{{ 'billing.invoices.status.PAID' | transloco }}</option>
          <option value="CANCELLED">{{ 'billing.invoices.status.CANCELLED' | transloco }}</option>
        </select>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'billing.invoices.list.loading' | transloco }}
      </div>

      <!-- Error -->
      <div *ngIf="error" class="alert-error" role="alert">{{ error }}</div>

      <!-- Empty -->
      <div *ngIf="!loading && !error && invoices.length === 0" class="empty-state">
        {{ 'billing.invoices.list.empty' | transloco }}
      </div>

      <!-- Invoice table -->
      <div *ngIf="!loading && invoices.length > 0" class="table-wrapper">
        <table class="data-table" role="table">
          <thead>
            <tr>
              <th scope="col">{{ 'billing.invoices.table.number' | transloco }}</th>
              <th scope="col">{{ 'billing.invoices.table.client' | transloco }}</th>
              <th scope="col">{{ 'billing.invoices.table.source' | transloco }}</th>
              <th scope="col">{{ 'billing.invoices.table.status' | transloco }}</th>
              <th scope="col">{{ 'billing.invoices.table.issuedDate' | transloco }}</th>
              <th scope="col">{{ 'billing.invoices.table.dueDate' | transloco }}</th>
              <th scope="col" class="col-right">{{ 'billing.invoices.table.total' | transloco }}</th>
              <th scope="col">{{ 'billing.invoices.table.actions' | transloco }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let invoice of invoices">
              <td>
                <a [routerLink]="['/billing/invoices', invoice.id]" class="link-primary">
                  {{ invoice.invoiceNumber }}
                </a>
              </td>
              <td>{{ invoice.clientId }}</td>
              <td>{{ 'billing.invoices.source.' + invoice.source | transloco }}</td>
              <td>
                <span class="status-badge" [ngClass]="'status-' + invoice.status.toLowerCase()">
                  {{ 'billing.invoices.status.' + invoice.status | transloco }}
                </span>
              </td>
              <td>{{ invoice.issuedDate | date:'mediumDate' }}</td>
              <td>{{ invoice.dueDate | date:'mediumDate' }}</td>
              <td class="col-right">{{ invoice.total | number:'1.2-2' }}</td>
              <td>
                <a [routerLink]="['/billing/invoices', invoice.id]" class="btn-link">
                  {{ 'common.actions.view' | transloco }}
                </a>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div class="pagination-bar" *ngIf="totalPages > 1">
          <button type="button" class="btn-secondary"
                  [disabled]="currentPage === 0"
                  (click)="loadPage(currentPage - 1)">
            {{ 'common.actions.previous' | transloco }}
          </button>
          <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button type="button" class="btn-secondary"
                  [disabled]="currentPage >= totalPages - 1"
                  (click)="loadPage(currentPage + 1)">
            {{ 'common.actions.next' | transloco }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding: var(--spacing-lg); }
    .page-header { display: flex; justify-content: space-between; align-items: center;
      margin-bottom: var(--spacing-lg); }
    .page-title { margin: 0; font-size: 1.5rem; }
    .filter-bar { display: flex; gap: var(--spacing-md); margin-bottom: var(--spacing-md); }
    .filter-select { padding: 6px 10px; border: 1px solid var(--color-border);
      border-radius: var(--radius-sm); font-size: 0.875rem; }
    .state-msg { padding: var(--spacing-md); color: var(--color-text-muted); }
    .alert-error { padding: var(--spacing-sm) var(--spacing-md); background: #fee2e2;
      color: #991b1b; border-radius: var(--radius-sm); }
    .empty-state { text-align: center; padding: var(--spacing-xl);
      color: var(--color-text-muted); border: 1px dashed var(--color-border);
      border-radius: var(--radius-md); }
    .table-wrapper { overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
    .data-table th { background: var(--color-surface-alt, #f8f9fa); padding: 10px 12px;
      text-align: left; font-weight: 600; border-bottom: 2px solid var(--color-border); }
    .data-table td { padding: 10px 12px; border-bottom: 1px solid var(--color-border);
      vertical-align: middle; }
    .data-table tbody tr:hover { background: var(--color-surface-hover, #f5f5f5); }
    .col-right { text-align: right; }
    .link-primary { color: var(--color-accent); text-decoration: none; font-weight: 500; }
    .link-primary:hover { text-decoration: underline; }
    .btn-link { background: none; border: none; color: var(--color-accent); cursor: pointer;
      font-size: 0.875rem; padding: 0; text-decoration: none; }
    .status-badge { padding: 2px 8px; border-radius: var(--radius-full); font-size: 0.75rem;
      font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
    .status-draft { background: #fef3c7; color: #92400e; }
    .status-issued { background: #dbeafe; color: #1e40af; }
    .status-overdue { background: #fee2e2; color: #991b1b; }
    .status-paid { background: #dcfce7; color: #166534; }
    .status-cancelled { background: #f3f4f6; color: #6b7280; }
    .pagination-bar { display: flex; justify-content: center; align-items: center;
      gap: var(--spacing-md); margin-top: var(--spacing-md); padding: var(--spacing-sm) 0; }
    .page-info { color: var(--color-text-muted); font-size: 0.875rem; }
  `]
})
export class InvoiceListComponent implements OnInit {
  invoices: InvoiceListItem[] = [];
  loading = false;
  error: string | null = null;
  statusFilter: InvoiceStatus | '' = '';
  currentPage = 0;
  totalPages = 0;
  readonly pageSize = 20;

  constructor(
    private invoiceService: InvoiceService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  applyFilter(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.loading = true;
    this.error = null;
    this.invoiceService.list({
      status: this.statusFilter || undefined,
      page,
      size: this.pageSize
    }).subscribe({
      next: (response) => {
        this.invoices = response.content;
        this.currentPage = response.number;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load invoices';
        this.loading = false;
      }
    });
  }
}
