import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ALL_STATUSES,
  LeadDetail,
  LeadPage,
  LeadStatus,
  LeadSummary,
  LEAD_STATUS_LABELS,
} from '../../models/lead.model';
import { LeadService } from '../../services/lead.service';
import { CreateLeadDialogComponent } from '../create-lead-dialog/create-lead-dialog.component';
import { EditLeadDialogComponent } from '../edit-lead-dialog/edit-lead-dialog.component';

/**
 * Lead list page.
 *
 * Features:
 * - Server-side paginated table (page size 20)
 * - Status filter and "Show archived" toggle
 * - Action menu per row: Edit, Archive
 * - Dialogs for create and edit
 */
@Component({
  selector: 'app-lead-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CreateLeadDialogComponent,
    EditLeadDialogComponent,
  ],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>{{ 'leads.title' | transloco }}</h1>
        <button class="btn-primary" (click)="openCreate()">+ New lead</button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" [attr.aria-label]="'leads.list.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="statusFilter">{{ 'leads.list.statusLabel' | transloco }}</label>
          <select id="statusFilter" [(ngModel)]="statusFilter" (change)="applyFilters()">
            <option value="">All statuses</option>
            <option *ngFor="let s of allStatuses" [value]="s">{{ statusLabels[s] }}</option>
          </select>
        </div>

        <div class="filter-group filter-group-toggle">
          <label for="archivedToggle">
            <input
              id="archivedToggle"
              type="checkbox"
              [(ngModel)]="includeArchived"
              (change)="applyFilters()"
            />
            Show archived
          </label>
        </div>
      </div>

      <!-- Loading / empty / error states -->
      <div *ngIf="loading" class="state-msg" aria-live="polite">Loading…</div>
      <div *ngIf="!loading && loadError" class="alert-error" role="alert">{{ loadError }}</div>
      <div *ngIf="!loading && !loadError && leads.length === 0" class="state-msg">
        No leads match the current filters.
      </div>

      <!-- Table -->
      <div class="table-wrapper" *ngIf="!loading && leads.length > 0">
        <table [attr.aria-label]="'leads.list.ariaList' | transloco">
          <thead>
            <tr>
              <th scope="col">
                <button class="sort-btn" (click)="sortBy('fullName')" type="button"
                        [attr.aria-sort]="ariaSort('fullName')">{{ 'leads.list.tableHeaders.name' | transloco }}</button>
              </th>
              <th scope="col">{{ 'leads.list.tableHeaders.contact' | transloco }}</th>
              <th scope="col">{{ 'leads.list.tableHeaders.source' | transloco }}</th>
              <th scope="col">
                <button class="sort-btn" (click)="sortBy('status')" type="button"
                        [attr.aria-sort]="ariaSort('status')">{{ 'leads.list.tableHeaders.status' | transloco }}</button>
              </th>
              <th scope="col">{{ 'leads.list.tableHeaders.owner' | transloco }}</th>
              <th scope="col">Last contact</th>
              <th scope="col">
                <button class="sort-btn" (click)="sortBy('createdAt')" type="button"
                        [attr.aria-sort]="ariaSort('createdAt')">{{ 'leads.list.tableHeaders.created' | transloco }}</button>
              </th>
              <th scope="col"><span class="sr-only">{{ 'leads.list.tableHeaders.actions' | transloco }}</span></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let lead of leads" [class.row-archived]="lead.status === 'INACTIVE'">
              <td>{{ lead.fullName }}</td>
              <td>{{ lead.primaryContact ?? '—' }}</td>
              <td>{{ lead.source ?? '—' }}</td>
              <td>
                <span class="badge" [class]="'badge-' + lead.status.toLowerCase()">
                  {{ statusLabels[lead.status] }}
                </span>
              </td>
              <td>{{ lead.ownerName ?? '—' }}</td>
              <td>{{ lead.lastContactDate ? (lead.lastContactDate | date:'dd MMM yyyy') : '—' }}</td>
              <td>{{ lead.createdAt | date:'dd MMM yyyy' }}</td>
              <td class="actions-cell">
                <button class="btn-action" (click)="openEdit(lead.id)"
                        [attr.aria-label]="'Edit ' + lead.fullName">
                  Edit
                </button>
                <button
                  class="btn-action btn-danger"
                  *ngIf="lead.status !== 'INACTIVE' && lead.status !== 'CONVERTED'"
                  (click)="archive(lead)"
                  [attr.aria-label]="'Archive ' + lead.fullName">
                  Archive
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <nav class="pagination" [attr.aria-label]="'leads.list.ariaPagination' | transloco" *ngIf="totalPages > 1">
        <button (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 1"
                [attr.aria-label]="'common.pagination.previousPage' | transloco">&lsaquo;</button>
        <span>Page {{ currentPage }} of {{ totalPages }}</span>
        <button (click)="goToPage(currentPage + 1)" [disabled]="currentPage === totalPages"
                [attr.aria-label]="'common.pagination.nextPage' | transloco">&rsaquo;</button>
      </nav>
      <p class="total-count" *ngIf="!loading">
        {{ totalElements }} lead{{ totalElements !== 1 ? 's' : '' }} found
      </p>
    </div>

    <!-- Dialogs -->
    <app-create-lead-dialog
      *ngIf="showCreate"
      (created)="onCreated()"
      (cancelled)="showCreate = false">
    </app-create-lead-dialog>

    <app-edit-lead-dialog
      *ngIf="editTarget"
      [lead]="editTarget"
      (updated)="onUpdated($event)"
      (converted)="onConverted()"
      (cancelled)="editTarget = null">
    </app-edit-lead-dialog>
  `,
  styles: [`
    .page { padding: 2rem; max-width: 1200px; margin: 0 auto; }
    .page-header {
      display: flex; align-items: center; justify-content: space-between;
      margin-bottom: 1.5rem;
    }
    h1 { margin: 0; font-size: 1.5rem; }
    .btn-primary {
      padding: .5rem 1.25rem; background: #0EA5A0; color: #fff;
      border: none; border-radius: 8px; cursor: pointer; font-size: .9375rem; font-weight: 600;
      transition: background 0.15s ease, box-shadow 0.15s ease;
    }
    .btn-primary:hover { background: #0C9490; box-shadow: 0 4px 12px rgba(14,165,160,.28); }
    .filters { display: flex; gap: 1rem; margin-bottom: 1.5rem; flex-wrap: wrap; align-items: flex-end; }
    .filter-group { display: flex; flex-direction: column; gap: .35rem; }
    .filter-group-toggle { flex-direction: row; align-items: center; }
    .filter-group-toggle label {
      display: flex; align-items: center; gap: .5rem;
      font-size: .9375rem; cursor: pointer; font-weight: 500;
    }
    .filter-group label { font-size: .8125rem; font-weight: 500; color: #374151; }
    select {
      appearance: none; padding: .5rem .875rem; border: 1.5px solid #D1D5DB;
      border-radius: 8px; font-size: .9375rem; font-family: inherit;
      color: #0F172A; background: #fff; outline: none;
    }
    .state-msg { color: #64748B; padding: 2rem 0; text-align: center; }
    .alert-error {
      padding: .75rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; margin-bottom: 1rem; font-size: .875rem;
    }
    .table-wrapper { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: .95rem; }
    th {
      text-align: left; padding: .75rem 1rem;
      background: #f7fafc; border-bottom: 2px solid #e2e8f0; white-space: nowrap;
    }
    td { padding: .75rem 1rem; border-bottom: 1px solid #e2e8f0; }
    tr:last-child td { border-bottom: none; }
    tr.row-archived td { color: #94A3B8; }
    .sort-btn {
      background: none; border: none; cursor: pointer;
      font-weight: 600; font-size: inherit; padding: 0; color: #0F172A;
    }
    .sort-btn:hover { color: #0EA5A0; }
    .badge {
      display: inline-block; padding: .2rem .6rem;
      border-radius: 999px; font-size: .8rem; font-weight: 500;
      background: #e2e8f0; color: #2d3748;
    }
    .badge-new { background: #EFF6FF; color: #1D4ED8; }
    .badge-contacted { background: #F0FDF4; color: #15803D; }
    .badge-qualified { background: #FFFBEB; color: #B45309; }
    .badge-converted { background: #F0FDF4; color: #166534; }
    .badge-inactive { background: #F1F5F9; color: #64748B; }
    .actions-cell { white-space: nowrap; }
    .btn-action {
      padding: .3rem .75rem; margin-right: .4rem;
      border: 1.5px solid #D1D5DB; border-radius: 6px;
      background: #fff; cursor: pointer; font-size: .8125rem;
      color: #374151; font-weight: 500;
      transition: background 0.12s ease, border-color 0.12s ease;
    }
    .btn-action:hover { background: #F9FAFB; border-color: #9CA3AF; }
    .btn-danger { border-color: #FECACA; color: #DC2626; }
    .btn-danger:hover { background: #FEF2F2; border-color: #FCA5A5; }
    .pagination { display: flex; align-items: center; gap: 1rem; margin-top: 1.5rem; }
    .pagination button {
      padding: .4rem .9rem; border: 1.5px solid #D1D5DB;
      border-radius: 6px; background: #fff; cursor: pointer; font-size: 1.1rem; color: #374151;
    }
    .pagination button:hover:not(:disabled) { background: #F9FAFB; border-color: #9CA3AF; }
    .pagination button:disabled { opacity: .35; cursor: not-allowed; }
    .total-count { color: #64748B; font-size: .875rem; margin-top: .5rem; }
    .sr-only {
      position: absolute; width: 1px; height: 1px;
      padding: 0; margin: -1px; overflow: hidden;
      clip: rect(0,0,0,0); white-space: nowrap; border: 0;
    }
  `]
})
export class LeadListComponent implements OnInit {
  readonly allStatuses = ALL_STATUSES;
  readonly statusLabels = LEAD_STATUS_LABELS;

  leads: LeadSummary[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  sortField = 'createdAt';
  sortDir = 'desc';

  statusFilter: LeadStatus | '' = '';
  includeArchived = false;

  loading = false;
  loadError: string | null = null;

  showCreate = false;
  editTarget: LeadDetail | null = null;

  constructor(private leadService: LeadService) {}

  ngOnInit(): void {
    this.loadPage();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadPage();
  }

  sortBy(field: string): void {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDir = 'asc';
    }
    this.currentPage = 0;
    this.loadPage();
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    if (this.sortField !== field) return 'none';
    return this.sortDir === 'asc' ? 'ascending' : 'descending';
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadPage();
  }

  openCreate(): void {
    this.showCreate = true;
  }

  openEdit(id: string): void {
    this.leadService.getLead(id).subscribe({
      next: (lead) => { this.editTarget = lead; },
      error: () => { this.loadError = 'Failed to load lead details.'; }
    });
  }

  archive(lead: LeadSummary): void {
    if (!confirm(`Archive "${lead.fullName}"? This lead will be hidden from the default view.`)) return;
    this.leadService.archiveLead(lead.id).subscribe({
      next: () => this.loadPage(),
      error: () => { this.loadError = 'Failed to archive lead. Please try again.'; }
    });
  }

  onCreated(): void {
    this.showCreate = false;
    this.loadPage();
  }

  onConverted(): void {
    this.editTarget = null;
    this.loadPage();
  }

  onUpdated(lead: LeadDetail): void {
    this.editTarget = null;
    const idx = this.leads.findIndex(l => l.id === lead.id);
    if (idx !== -1) {
      this.leads = [
        ...this.leads.slice(0, idx),
        {
          id: lead.id,
          fullName: lead.fullName,
          primaryContact: lead.contactMethods.find(c => c.isPrimary)?.value
            ?? lead.contactMethods[0]?.value ?? null,
          source: lead.source,
          status: lead.status,
          ownerName: lead.ownerName,
          lastContactDate: lead.lastContactDate,
          createdAt: lead.createdAt,
        },
        ...this.leads.slice(idx + 1)
      ];
    } else {
      this.loadPage();
    }
  }

  private loadPage(): void {
    this.loading = true;
    this.loadError = null;

    const params = {
      page: this.currentPage,
      size: this.pageSize,
      sort: `${this.sortField},${this.sortDir}`,
      ...(this.statusFilter ? { status: this.statusFilter } : {}),
      includeArchived: this.includeArchived,
    };

    this.leadService.listLeads(params).subscribe({
      next: (page: LeadPage) => {
        this.loading = false;
        this.leads = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.currentPage = page.page;
      },
      error: () => {
        this.loading = false;
        this.loadError = 'Failed to load leads. Please refresh the page.';
      }
    });
  }
}
