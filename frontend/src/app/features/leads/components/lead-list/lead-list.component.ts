import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
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
    TranslocoPipe,
    CreateLeadDialogComponent,
    EditLeadDialogComponent,
    Select,
    TableModule,
  ],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>{{ 'leads.title' | transloco }}</h1>
        <button class="btn-primary" (click)="openCreate()">{{ 'leads.list.newLead' | transloco }}</button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" [attr.aria-label]="'leads.list.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="statusFilter">{{ 'leads.list.statusLabel' | transloco }}</label>
          <p-select
            inputId="statusFilter"
            [(ngModel)]="statusFilter"
            [options]="statusFilterOptions"
            optionLabel="label"
            optionValue="value"
            (onChange)="applyFilters()"
            styleClass="w-full"
          />
        </div>

        <div class="filter-group filter-group-toggle">
          <label for="archivedToggle">
            <input
              id="archivedToggle"
              type="checkbox"
              [(ngModel)]="includeArchived"
              (change)="applyFilters()"
            />
            {{ 'leads.list.showArchived' | transloco }}
          </label>
        </div>
      </div>

      <!-- Error State -->
      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <!-- Table -->
      <p-table
        [value]="leads"
        [lazy]="true"
        (onLazyLoad)="onLazyLoad($event)"
        [paginator]="true"
        [rows]="pageSize"
        [first]="first"
        [totalRecords]="totalElements"
        [rowsPerPageOptions]="[10, 20, 50]"
        [showCurrentPageReport]="true"
        [sortField]="sortField"
        [sortOrder]="sortOrder"
        [loading]="loading"
        responsiveLayout="scroll"
        styleClass="p-datatable-sm"
      >
        <ng-template pTemplate="header">
          <tr>
            <th pSortableColumn="fullName">
              {{ 'leads.list.tableHeaders.name' | transloco }}
              <p-sortIcon field="fullName" />
            </th>
            <th>{{ 'leads.list.tableHeaders.contact' | transloco }}</th>
            <th>{{ 'leads.list.tableHeaders.source' | transloco }}</th>
            <th pSortableColumn="status">
              {{ 'leads.list.tableHeaders.status' | transloco }}
              <p-sortIcon field="status" />
            </th>
            <th>{{ 'leads.list.tableHeaders.owner' | transloco }}</th>
            <th>{{ 'leads.list.lastContact' | transloco }}</th>
            <th pSortableColumn="createdAt">
              {{ 'leads.list.tableHeaders.created' | transloco }}
              <p-sortIcon field="createdAt" />
            </th>
            <th><span class="sr-only">{{ 'leads.list.tableHeaders.actions' | transloco }}</span></th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-lead>
          <tr [class.row-archived]="lead.status === 'INACTIVE'">
            <td>{{ lead.fullName }}</td>
            <td>{{ lead.primaryContact ?? '—' }}</td>
            <td>{{ lead.source ?? '—' }}</td>
            <td>
              <span class="badge" [class]="'badge-' + lead.status.toLowerCase()">
                {{ statusKey(lead.status) | transloco }}
              </span>
            </td>
            <td>{{ lead.ownerName ?? '—' }}</td>
            <td>{{ lead.lastContactDate ? (lead.lastContactDate | date:'dd MMM yyyy') : '—' }}</td>
            <td>{{ lead.createdAt | date:'dd MMM yyyy' }}</td>
            <td class="actions-cell">
              <button class="btn-action" (click)="openEdit(lead.id)"
                      [attr.aria-label]="'leads.list.editAriaLabel' | transloco:{name: lead.fullName}">
                {{ 'leads.list.edit' | transloco }}
              </button>
              <button
                class="btn-action btn-danger"
                *ngIf="lead.status !== 'INACTIVE' && lead.status !== 'CONVERTED'"
                (click)="archive(lead)"
                [attr.aria-label]="'leads.list.archiveAriaLabel' | transloco:{name: lead.fullName}">
                {{ 'leads.list.archive' | transloco }}
              </button>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="8" class="state-msg">{{ 'leads.list.noLeadsFiltered' | transloco }}</td>
          </tr>
        </ng-template>
      </p-table>
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
    .sr-only {
      position: absolute; width: 1px; height: 1px;
      padding: 0; margin: -1px; overflow: hidden;
      clip: rect(0,0,0,0); white-space: nowrap; border: 0;
    }
    :host ::ng-deep .p-datatable .p-datatable-tbody > tr.row-archived > td {
      color: #94A3B8 !important;
    }
  `]
})
export class LeadListComponent implements OnInit {
  readonly allStatuses = ALL_STATUSES;
  readonly statusLabels = LEAD_STATUS_LABELS;
  readonly statusTransKeys: Record<LeadStatus, string> = {
    NEW: 'leads.list.statusNew',
    CONTACTED: 'leads.list.statusContacted',
    QUALIFIED: 'leads.list.statusQualified',
    CONVERTED: 'leads.list.statusConverted',
    INACTIVE: 'leads.list.statusInactive',
  };

  leads: LeadSummary[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  sortField = 'createdAt';
  sortDir = 'desc';
  first = 0;

  get sortOrder(): number {
    return this.sortDir === 'asc' ? 1 : -1;
  }

  statusFilter: LeadStatus | '' = '';
  includeArchived = false;
  statusFilterOptions: { label: string; value: string }[] = [];

  loading = false;
  loadError: string | null = null;

  showCreate = false;
  editTarget: LeadDetail | null = null;

  constructor(private leadService: LeadService, private transloco: TranslocoService) {}

  ngOnInit(): void {
    this.statusFilterOptions = [
      { label: this.transloco.translate('leads.list.statusAll'), value: '' },
      ...this.allStatuses.map(s => ({ label: this.transloco.translate(this.statusTransKeys[s]), value: s })),
    ];
  }

  statusKey(status: string): string {
    return this.statusTransKeys[status as LeadStatus] ?? status;
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.pageSize = event.rows ?? 20;
    this.currentPage = Math.floor(this.first / this.pageSize);
    if (event.sortField) {
      this.sortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField as string;
      this.sortDir = (event.sortOrder ?? -1) === 1 ? 'asc' : 'desc';
    }
    this.loadPage();
  }

  applyFilters(): void {
    this.first = 0;
    this.currentPage = 0;
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
        this.first = page.page * this.pageSize;
      },
      error: () => {
        this.loading = false;
        this.loadError = 'Failed to load leads. Please refresh the page.';
      }
    });
  }
}
