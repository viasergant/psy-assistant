import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { TherapistManagementService } from '../../services/therapist-management.service';
import {
  TherapistProfile,
  EMPLOYMENT_STATUS_OPTIONS,
  EMPLOYMENT_STATUS_LABELS,
  EmploymentStatus
} from '../../models/therapist.model';
import { ASSIGNABLE_ROLES, ROLE_LABELS, UserRole } from '../../../users/models/user.model';
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
    TranslocoPipe,
    CreateTherapistDialogComponent,
    EditTherapistDialogComponent
  ],
  styleUrl: './therapist-list.component.scss',
  template: `
    <div class="page">
      <header class="page-header">
        <h1>Therapist Management</h1>
        <button class="btn-primary" (click)="openCreate()">+ Create therapist</button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" [attr.aria-label]="'admin.therapists.list.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="roleFilter">{{ 'admin.therapists.list.roleLabel' | transloco }}</label>
          <select id="roleFilter" [(ngModel)]="roleFilter" (change)="onRoleFilterChange()">
            <option value="">All roles</option>
            <option *ngFor="let r of assignableRoles" [value]="r">{{ roleLabels[r] }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label for="statusFilter">{{ 'admin.therapists.list.statusLabel' | transloco }}</label>
          <select id="statusFilter" [(ngModel)]="statusFilter" (change)="applyFilters()">
            <option value="">All statuses</option>
            <option value="true">{{ 'admin.therapists.list.statusActive' | transloco }}</option>
            <option value="false">{{ 'admin.therapists.list.statusInactive' | transloco }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label for="employmentFilter">Employment Status</label>
          <select id="employmentFilter" [(ngModel)]="employmentFilter" (change)="applyFilters()">
            <option value="">All statuses</option>
            <option *ngFor="let status of employmentStatuses" [value]="status">
              {{ statusLabels[status] }}
            </option>
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
        <table [attr.aria-label]="'admin.therapists.list.ariaList' | transloco">
          <thead>
            <tr>
              <th scope="col">{{ 'admin.therapists.list.tableHeaders.name' | transloco }}</th>
              <th scope="col">{{ 'admin.therapists.list.tableHeaders.email' | transloco }}</th>
              <th scope="col">{{ 'admin.therapists.list.tableHeaders.phone' | transloco }}</th>
              <th scope="col">Employment Status</th>
              <th scope="col">{{ 'admin.therapists.list.tableHeaders.specializations' | transloco }}</th>
              <th scope="col">{{ 'admin.therapists.list.tableHeaders.status' | transloco }}</th>
              <th scope="col">{{ 'admin.therapists.list.tableHeaders.created' | transloco }}</th>
              <th scope="col"><span class="sr-only">{{ 'admin.therapists.list.tableHeaders.actions' | transloco }}</span></th>
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
      <nav class="pagination" [attr.aria-label]="'admin.therapists.list.ariaPagination' | transloco" *ngIf="totalPages > 1">
        <button (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 1"
                [attr.aria-label]="'common.pagination.previousPage' | transloco">
          &lsaquo;
        </button>
        <span>Page {{ currentPage + 1 }} of {{ totalPages }}</span>
        <button (click)="goToPage(currentPage + 1)" [disabled]="currentPage === totalPages - 1"
                [attr.aria-label]="'common.pagination.nextPage' | transloco">
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
})

export class TherapistListComponent implements OnInit {
  readonly employmentStatuses = EMPLOYMENT_STATUS_OPTIONS;
  readonly statusLabels = EMPLOYMENT_STATUS_LABELS;
  readonly assignableRoles = ASSIGNABLE_ROLES;
  readonly roleLabels = ROLE_LABELS;

  therapists: TherapistProfile[] = [];
  loading = false;
  loadError: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filters
  roleFilter: UserRole | '' = 'THERAPIST'; // Default to THERAPIST since we're on therapist page
  statusFilter: '' | 'true' | 'false' = '';
  employmentFilter: EmploymentStatus | '' = '';

  // Dialogs
  showCreate = false;
  showEdit = false;
  editingTherapist: TherapistProfile | null = null;

  constructor(
    private therapistService: TherapistManagementService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Read initial filter state from query params if present
    this.route.queryParams.subscribe(params => {
      if (params['status']) {
        this.statusFilter = params['status'];
      }
    });
    
    this.loadTherapists();
  }

  /**
   * Handles role filter changes. If a different role is selected or "All roles",
   * navigate to the main users page with appropriate filters.
   */
  onRoleFilterChange(): void {
    if (this.roleFilter === '' || this.roleFilter !== 'THERAPIST') {
      // Navigate to /admin/users with the selected role filter and preserve status filter
      const queryParams: any = {};
      if (this.roleFilter) {
        queryParams.role = this.roleFilter;
      }
      if (this.statusFilter !== '') {
        queryParams.status = this.statusFilter;
      }
      this.router.navigate(['/admin/users'], { queryParams });
    } else {
      // Stay on therapist page and reload
      this.applyFilters();
    }
  }

  loadTherapists(): void {
    this.loading = true;
    this.loadError = null;

    const active = this.statusFilter === '' ? undefined : this.statusFilter === 'true';
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
