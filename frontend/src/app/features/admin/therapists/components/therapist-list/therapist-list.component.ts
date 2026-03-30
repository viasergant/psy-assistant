import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TherapistManagementService } from '../../services/therapist-management.service';
import {
  TherapistProfile,
  EMPLOYMENT_STATUS_OPTIONS,
  EMPLOYMENT_STATUS_LABELS,
  EmploymentStatus
} from '../../models/therapist.model';
import { CreateTherapistDialogComponent } from '../../../therapists/components/create-therapist-dialog/create-therapist-dialog.component';
import { EditTherapistDialogComponent } from '../../../therapists/components/edit-therapist-dialog/edit-therapist-dialog.component';

/**
 * Admin therapist list page.
 *
 * Features:
 * - Server-side paginated table (page size 20)
 * - Employment status and active-status filter chips
 * - Action menu per row: Edit, View Profile, Deactivate/Reactivate
 * - Dialogs for create and edit
 */
@Component({
  selector: 'app-therapist-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CreateTherapistDialogComponent,
    EditTherapistDialogComponent
  ],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>Therapist Management</h1>
        <button class="btn-primary" (click)="openCreate()">+ Create therapist</button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" aria-label="Therapist filters">
        <div class="filter-group">
          <label for="employmentFilter">Employment Status</label>
          <select id="employmentFilter" [(ngModel)]="employmentFilter" (change)="applyFilters()">
            <option value="">All statuses</option>
            <option *ngFor="let status of employmentStatuses" [value]="status">
              {{ statusLabels[status] }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label for="activeFilter">Active Status</label>
          <select id="activeFilter" [(ngModel)]="activeFilter" (change)="applyFilters()">
            <option value="">All</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </div>
      </div>

      <!-- Loading / empty / error states -->
      <div *ngIf="loading" class="state-msg" aria-live="polite">Loading…</div>
      <div *ngIf="!loading && loadError" class="alert-error" role="alert">{{ loadError }}</div>
      <div *ngIf="!loading && !loadError && therapists.length === 0" class="state-msg">
        No therapists match the current filters.
      </div>

      <!-- Table -->
      <div class="table-wrapper" *ngIf="!loading && therapists.length > 0">
        <table aria-label="Therapist list">
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Email</th>
              <th scope="col">Phone</th>
              <th scope="col">Employment Status</th>
              <th scope="col">Specializations</th>
              <th scope="col">Status</th>
              <th scope="col">Created</th>
              <th scope="col"><span class="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let t of therapists">
              <td class="name-cell">{{ t.name }}</td>
              <td>{{ t.email }}</td>
              <td>{{ t.phone || '—' }}</td>
              <td>
                <span class="badge" [class.badge-active]="t.employmentStatus === 'ACTIVE'">
                  {{ statusLabels[t.employmentStatus] }}
                </span>
              </td>
              <td class="specs-cell">
                <span *ngFor="let spec of t.specializations" class="spec-chip">
                  {{ spec.name }}
                </span>
                <span *ngIf="t.specializations.length === 0" class="text-muted">—</span>
              </td>
              <td>
                <span class="badge" [class.badge-active]="t.active" [class.badge-inactive]="!t.active">
                  {{ t.active ? 'Active' : 'Inactive' }}
                </span>
              </td>
              <td>{{ t.createdAt | date:'dd MMM yyyy' }}</td>
              <td class="actions-cell">
                <button class="btn-action" (click)="openEdit(t)" [attr.aria-label]="'Edit ' + t.name">
                  Edit
                </button>
                <button
                  class="btn-action"
                  [class.btn-danger]="t.active"
                  (click)="toggleActive(t)"
                  [attr.aria-label]="(t.active ? 'Deactivate ' : 'Reactivate ') + t.name">
                  {{ t.active ? 'Deactivate' : 'Reactivate' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <nav class="pagination" aria-label="Therapist list pagination" *ngIf="totalPages > 1">
        <button (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 0"
                aria-label="Previous page">
          &lsaquo;
        </button>
        <span>Page {{ currentPage + 1 }} of {{ totalPages }}</span>
        <button (click)="goToPage(currentPage + 1)" [disabled]="currentPage === totalPages - 1"
                aria-label="Next page">
          &rsaquo;
        </button>
      </nav>
      <p class="total-count" *ngIf="!loading">
        {{ totalElements }} therapist{{ totalElements !== 1 ? 's' : '' }} found
      </p>
    </div>

    <!-- Dialogs -->
    <app-create-therapist-dialog
      *ngIf="showCreate"
      (created)="onCreated()"
      (cancelled)="showCreate = false">
    </app-create-therapist-dialog>

    <app-edit-therapist-dialog
      *ngIf="showEdit && editingTherapist"
      [therapist]="editingTherapist"
      (updated)="onUpdated()"
      (cancelled)="showEdit = false">
    </app-edit-therapist-dialog>
  `,
  styles: [`
    .page {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }
    h1 {
      font-size: 1.875rem;
      font-weight: 600;
      color: var(--color-text-primary, #0F172A);
      margin: 0;
    }
    .btn-primary {
      padding: .7rem 1.5rem;
      background: var(--color-accent, #0EA5A0);
      color: #fff;
      border: none;
      border-radius: 8px;
      font-size: .9375rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      font-family: inherit;
    }
    .btn-primary:hover {
      background: var(--color-accent-hover, #0C9490);
      box-shadow: 0 4px 14px rgba(14,165,160,.3);
      transform: translateY(-1px);
    }
    .filters {
      display: flex;
      gap: 1.5rem;
      margin-bottom: 2rem;
      padding: 1.25rem;
      background: var(--color-surface, #fff);
      border-radius: 12px;
      border: 1.5px solid var(--color-border, #E2E8F0);
    }
    .filter-group {
      display: flex;
      flex-direction: column;
      gap: .4rem;
    }
    .filter-group label {
      font-size: .875rem;
      font-weight: 500;
      color: var(--color-text-secondary, #64748B);
    }
    .filter-group select {
      padding: .55rem .875rem;
      border: 1.5px solid var(--color-border, #E2E8F0);
      border-radius: 8px;
      font-size: .9375rem;
      min-width: 180px;
      font-family: inherit;
      color: var(--color-text-primary, #0F172A);
      background: #fff;
      cursor: pointer;
      transition: border-color 0.15s;
    }
    .filter-group select:hover {
      border-color: #9CA3AF;
    }
    .filter-group select:focus {
      outline: none;
      border-color: var(--color-accent, #0EA5A0);
      box-shadow: 0 0 0 3px rgba(14,165,160,.15);
    }
    .state-msg {
      padding: 3rem 2rem;
      text-align: center;
      color: var(--color-text-secondary, #64748B);
      font-size: .9375rem;
    }
    .alert-error {
      padding: .875rem 1.125rem;
      background: var(--color-error-bg, #FEF2F2);
      border: 1px solid var(--color-error-border, #FECACA);
      border-radius: 10px;
      color: var(--color-error, #DC2626);
      margin-bottom: 1.5rem;
      font-size: .875rem;
    }
    .table-wrapper {
      background: var(--color-surface, #fff);
      border-radius: 12px;
      border: 1.5px solid var(--color-border, #E2E8F0);
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0,0,0,.04);
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    thead {
      background: #F8FAFC;
      border-bottom: 1.5px solid var(--color-border, #E2E8F0);
    }
    th {
      text-align: left;
      padding: .875rem 1rem;
      font-size: .8125rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .03em;
      color: var(--color-text-secondary, #64748B);
    }
    td {
      padding: 1rem;
      font-size: .9375rem;
      color: var(--color-text-primary, #0F172A);
      border-bottom: 1px solid #F1F5F9;
    }
    tbody tr:last-child td {
      border-bottom: none;
    }
    tbody tr:hover {
      background: #FAFBFC;
    }
    .name-cell {
      font-weight: 500;
    }
    .specs-cell {
      display: flex;
      flex-wrap: wrap;
      gap: .375rem;
      align-items: center;
    }
    .spec-chip {
      padding: .25rem .625rem;
      background: #EFF6FF;
      color: #1E40AF;
      border-radius: 6px;
      font-size: .8125rem;
      font-weight: 500;
    }
    .text-muted {
      color: var(--color-text-muted, #94A3B8);
    }
    .badge {
      display: inline-block;
      padding: .35rem .75rem;
      border-radius: 6px;
      font-size: .8125rem;
      font-weight: 500;
      white-space: nowrap;
    }
    .badge-active {
      background: #DCFCE7;
      color: #166534;
    }
    .badge-inactive {
      background: #F3F4F6;
      color: #6B7280;
    }
    .actions-cell {
      display: flex;
      gap: .5rem;
      justify-content: flex-end;
    }
    .btn-action {
      padding: .45rem .875rem;
      border: 1.5px solid var(--color-border, #E2E8F0);
      background: #fff;
      border-radius: 6px;
      font-size: .8125rem;
      font-weight: 500;
      cursor: pointer;
      color: var(--color-text-primary, #0F172A);
      transition: all 0.15s;
      font-family: inherit;
    }
    .btn-action:hover {
      background: #F8FAFC;
      border-color: #CBD5E1;
    }
    .btn-danger {
      color: var(--color-error, #DC2626);
      border-color: #FCA5A5;
    }
    .btn-danger:hover {
      background: #FEF2F2;
      border-color: var(--color-error, #DC2626);
    }
    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 1rem;
      margin-top: 2rem;
    }
    .pagination button {
      padding: .5rem .875rem;
      border: 1.5px solid var(--color-border, #E2E8F0);
      background: #fff;
      border-radius: 6px;
      font-size: .9375rem;
      cursor: pointer;
      font-family: inherit;
      transition: all 0.15s;
    }
    .pagination button:hover:not(:disabled) {
      background: #F8FAFC;
      border-color: var(--color-accent, #0EA5A0);
    }
    .pagination button:disabled {
      opacity: .4;
      cursor: not-allowed;
    }
    .pagination span {
      font-size: .9375rem;
      color: var(--color-text-secondary, #64748B);
    }
    .total-count {
      text-align: center;
      margin-top: .75rem;
      font-size: .875rem;
      color: var(--color-text-muted, #94A3B8);
    }
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0,0,0,0);
      white-space: nowrap;
      border-width: 0;
    }
  `]
})
export class TherapistListComponent implements OnInit {
  readonly employmentStatuses = EMPLOYMENT_STATUS_OPTIONS;
  readonly statusLabels = EMPLOYMENT_STATUS_LABELS;

  therapists: TherapistProfile[] = [];
  loading = false;
  loadError: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filters
  employmentFilter: EmploymentStatus | '' = '';
  activeFilter: string = '';

  // Dialogs
  showCreate = false;
  showEdit = false;
  editingTherapist: TherapistProfile | null = null;

  constructor(private therapistService: TherapistManagementService) {}

  ngOnInit(): void {
    this.loadTherapists();
  }

  loadTherapists(): void {
    this.loading = true;
    this.loadError = null;

    const active = this.activeFilter === '' ? undefined : this.activeFilter === 'true';
    const employment = this.employmentFilter === '' ? undefined : this.employmentFilter;

    this.therapistService.getTherapists(this.currentPage, this.pageSize, employment, active).subscribe({
      next: (page) => {
        this.therapists = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Failed to load therapists. Please try again.';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadTherapists();
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadTherapists();
  }

  openCreate(): void {
    this.showCreate = true;
  }

  openEdit(therapist: TherapistProfile): void {
    this.editingTherapist = therapist;
    this.showEdit = true;
  }

  onCreated(): void {
    this.showCreate = false;
    this.loadTherapists();
  }

  onUpdated(): void {
    this.showEdit = false;
    this.editingTherapist = null;
    this.loadTherapists();
  }

  toggleActive(therapist: TherapistProfile): void {
    if (!confirm(`Are you sure you want to ${therapist.active ? 'deactivate' : 'reactivate'} ${therapist.name}?`)) {
      return;
    }

    this.therapistService.toggleActive(therapist.id, therapist.version).subscribe({
      next: () => {
        this.loadTherapists();
      },
      error: () => {
        alert('Failed to update therapist status. Please try again.');
      }
    });
  }
}
