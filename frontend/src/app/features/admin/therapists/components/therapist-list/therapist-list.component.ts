import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
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
import { TranslateSpecializationPipe } from '../../pipes/translate-specialization.pipe';

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
    TranslateSpecializationPipe,
    CreateTherapistDialogComponent,
    EditTherapistDialogComponent,
    Select,
    TableModule,
  ],
  styleUrl: './therapist-list.component.scss',
  template: `
    <div class="page">
      <header class="page-header">
        <h1>{{ 'admin.therapists.list.pageTitle' | transloco }}</h1>
        <button class="btn-primary" (click)="openCreate()">{{ 'admin.therapists.list.createButton' | transloco }}</button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" [attr.aria-label]="'admin.therapists.list.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="roleFilter">{{ 'admin.therapists.list.roleLabel' | transloco }}</label>
          <p-select
            inputId="roleFilter"
            [(ngModel)]="roleFilter"
            [options]="roleFilterOptions"
            optionLabel="label"
            optionValue="value"
            (onChange)="onRoleFilterChange()"
            styleClass="w-full"
          />
        </div>

        <div class="filter-group">
          <label for="statusFilter">{{ 'admin.therapists.list.statusLabel' | transloco }}</label>
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

        <div class="filter-group">
          <label for="employmentFilter">{{ 'admin.therapists.list.employmentStatusLabel' | transloco }}</label>
          <p-select
            inputId="employmentFilter"
            [(ngModel)]="employmentFilter"
            [options]="employmentFilterOptions"
            optionLabel="label"
            optionValue="value"
            (onChange)="applyFilters()"
            styleClass="w-full"
          />
        </div>
      </div>

      <!-- Error state -->
      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <!-- Table -->
      <p-table
        [value]="therapists"
        [lazy]="true"
        (onLazyLoad)="onLazyLoad($event)"
        [paginator]="true"
        [rows]="pageSize"
        [first]="first"
        [totalRecords]="totalElements"
        [rowsPerPageOptions]="[10, 20, 50]"
        [showCurrentPageReport]="true"
        [loading]="loading"
        responsiveLayout="scroll"
        styleClass="p-datatable-sm"
      >
        <ng-template pTemplate="header">
          <tr>
            <th>{{ 'admin.therapists.list.tableHeaders.name' | transloco }}</th>
            <th>{{ 'admin.therapists.list.tableHeaders.email' | transloco }}</th>
            <th>{{ 'admin.therapists.list.tableHeaders.phone' | transloco }}</th>
            <th>{{ 'admin.therapists.list.tableHeaders.employmentStatus' | transloco }}</th>
            <th>{{ 'admin.therapists.list.tableHeaders.specializations' | transloco }}</th>
            <th>{{ 'admin.therapists.list.tableHeaders.status' | transloco }}</th>
            <th>{{ 'admin.therapists.list.tableHeaders.created' | transloco }}</th>
            <th><span class="sr-only">{{ 'admin.therapists.list.tableHeaders.actions' | transloco }}</span></th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-t>
          <tr>
            <td class="name-cell">{{ t.name }}</td>
            <td>{{ t.email }}</td>
            <td>{{ t.phone || '—' }}</td>
            <td>
              <span class="badge" [class.badge-active]="t.employmentStatus === 'ACTIVE'">
                {{ 'admin.therapists.create.employmentStatuses.' + t.employmentStatus | transloco }}
              </span>
            </td>
            <td class="specs-cell">
              <span *ngFor="let spec of t.specializations" class="spec-chip">
                {{ spec.name | translateSpecialization }}
              </span>
              <span *ngIf="t.specializations.length === 0" class="text-muted">—</span>
            </td>
            <td>
              <span class="badge" [class.badge-active]="t.active" [class.badge-inactive]="!t.active">
                {{ t.active ? ('admin.therapists.list.statusActive' | transloco) : ('admin.therapists.list.statusInactive' | transloco) }}
              </span>
            </td>
            <td>{{ t.createdAt | date:'dd MMM yyyy' }}</td>
            <td class="actions-cell">
              <button class="btn-action" (click)="openEdit(t)" [attr.aria-label]="('admin.therapists.list.editAction' | transloco) + ' ' + t.name">
                {{ 'admin.therapists.list.editAction' | transloco }}
              </button>
              <button
                class="btn-action"
                [class.btn-danger]="t.active"
                (click)="toggleActive(t)"
                [attr.aria-label]="(t.active ? ('admin.therapists.list.deactivateAction' | transloco) : ('admin.therapists.list.reactivateAction' | transloco)) + ' ' + t.name">
                {{ t.active ? ('admin.therapists.list.deactivateAction' | transloco) : ('admin.therapists.list.reactivateAction' | transloco) }}
              </button>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="8" class="state-msg">{{ 'admin.therapists.list.noResults' | transloco }}</td>
          </tr>
        </ng-template>
      </p-table>
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
  first = 0;

  // Filters
  roleFilter: UserRole | '' = 'THERAPIST'; // Default to THERAPIST since we're on therapist page
  statusFilter: '' | 'true' | 'false' = '';
  employmentFilter: EmploymentStatus | '' = '';
  roleFilterOptions: { label: string; value: string }[] = [];
  statusFilterOptions: { label: string; value: string }[] = [];
  employmentFilterOptions: { label: string; value: string }[] = [];

  // Dialogs
  showCreate = false;
  showEdit = false;
  editingTherapist: TherapistProfile | null = null;

  constructor(
    private therapistService: TherapistManagementService,
    private router: Router,
    private route: ActivatedRoute,
    private transloco: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.roleFilterOptions = [
      { label: this.transloco.translate('admin.therapists.list.filterAllRoles'), value: '' },
      ...this.assignableRoles.map(r => ({ label: this.transloco.translate('roles.' + r), value: r })),
    ];
    this.statusFilterOptions = [
      { label: this.transloco.translate('admin.therapists.list.filterAllStatuses'), value: '' },
      { label: this.transloco.translate('admin.therapists.list.statusActive'), value: 'true' },
      { label: this.transloco.translate('admin.therapists.list.statusInactive'), value: 'false' },
    ];
    this.employmentFilterOptions = [
      { label: this.transloco.translate('admin.therapists.list.filterAllStatuses'), value: '' },
      ...this.employmentStatuses.map(s => ({ label: this.transloco.translate('admin.therapists.create.employmentStatuses.' + s), value: s })),
    ];
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

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.pageSize);
    this.pageSize = event.rows ?? this.pageSize;
    this.first = event.first ?? 0;
    this.loadTherapists();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.first = 0;
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
