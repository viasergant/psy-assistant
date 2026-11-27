import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { UserManagementService } from '../../services/user-management.service';
import { TherapistManagementService } from '../../../therapists/services/therapist-management.service';
import { ASSIGNABLE_ROLES, ROLE_LABELS, UserRole, UserSummary, UserPage } from '../../models/user.model';
import { CreateUserDialogComponent } from '../create-user-dialog/create-user-dialog.component';
import { EditUserDialogComponent } from '../edit-user-dialog/edit-user-dialog.component';
import { PasswordResetDialogComponent } from '../password-reset-dialog/password-reset-dialog.component';
import { CreateTherapistDialogComponent } from '../../../therapists/components/create-therapist-dialog/create-therapist-dialog.component';

/**
 * Admin user list page.
 *
 * Features:
 * - Server-side paginated table (page size 20)
 * - Role and active-status filter chips
 * - Action menu per row: Edit, Reset Password, Deactivate/Reactivate
 * - Dialogs for create, edit, and password reset
 */
@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslocoPipe,
    CreateUserDialogComponent,
    EditUserDialogComponent,
    PasswordResetDialogComponent,
    CreateTherapistDialogComponent,
    Select,
    TableModule,
  ],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>{{ 'admin.users.list.pageTitle' | transloco }}</h1>
        <button class="btn-primary" (click)="openCreate()">{{ 'admin.users.list.createButton' | transloco }}</button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" [attr.aria-label]="'admin.users.list.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="roleFilter">{{ 'admin.users.list.roleLabel' | transloco }}</label>
          <p-select
            inputId="roleFilter"
            [(ngModel)]="roleFilter"
            [options]="roleFilterOptions"
            optionLabel="label"
            optionValue="value"
            (onChange)="applyFilters()"
            styleClass="w-full"
          />
        </div>

        <div class="filter-group">
          <label for="statusFilter">{{ 'admin.users.list.statusLabel' | transloco }}</label>
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
      </div>

      <!-- Error states -->
      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>
      <div *ngIf="errorMessage" class="alert-error" role="alert">
        {{ errorMessage }}
        <button class="close-btn" (click)="errorMessage = null" [attr.aria-label]="'admin.users.list.closeError' | transloco">&times;</button>
      </div>

      <!-- Table -->
      <p-table
        [value]="users"
        [lazy]="true"
        (onLazyLoad)="onLazyLoad($event)"
        [paginator]="true"
        paginatorDropdownAppendTo="body"
        [rows]="pageSize"
        [first]="first"
        [totalRecords]="totalElements"
        [rowsPerPageOptions]="[10, 20, 50]"
        [showCurrentPageReport]="true"
        [sortField]="sortField"
        [sortOrder]="sortDir === 'asc' ? 1 : -1"
        [loading]="loading"
        responsiveLayout="scroll"
        styleClass="p-datatable-sm"
      >
        <ng-template pTemplate="header">
          <tr>
            <th pSortableColumn="fullName">
              {{ 'admin.users.list.tableHeaders.name' | transloco }}
              <p-sortIcon field="fullName" />
            </th>
            <th>{{ 'admin.users.list.tableHeaders.email' | transloco }}</th>
            <th pSortableColumn="role">
              {{ 'admin.users.list.tableHeaders.role' | transloco }}
              <p-sortIcon field="role" />
            </th>
            <th>{{ 'admin.users.list.tableHeaders.status' | transloco }}</th>
            <th pSortableColumn="createdAt">
              {{ 'admin.users.list.tableHeaders.created' | transloco }}
              <p-sortIcon field="createdAt" />
            </th>
            <th><span class="sr-only">{{ 'admin.users.list.tableHeaders.actions' | transloco }}</span></th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-u>
          <tr>
            <td>
              <button
                class="name-link"
                (click)="navigateToProfile(u)"
                [attr.aria-label]="'admin.users.list.viewProfileAria' | transloco: { name: (u.fullName ?? u.email) }">
                {{ u.fullName ?? '—' }}
              </button>
            </td>
            <td>{{ u.email }}</td>
            <td>
              <span
                *ngFor="let r of (u.roles ?? [u.role]); let last = last"
                class="badge"
                [class.badge-admin]="r === 'SYSTEM_ADMINISTRATOR' || r === 'ADMIN'"
                [style.margin-right]="last ? '0' : '4px'">
                {{ 'roles.' + r | transloco }}
              </span>
            </td>
            <td>
              <span class="badge" [class.badge-active]="u.active" [class.badge-inactive]="!u.active">
                {{ u.active ? ('admin.users.list.statusActive' | transloco) : ('admin.users.list.statusInactive' | transloco) }}
              </span>
            </td>
            <td>{{ u.createdAt | date:'dd MMM yyyy' }}</td>
            <td class="actions-cell">
              <button class="btn-action" (click)="openEdit(u)" [attr.aria-label]="'admin.users.list.editAria' | transloco: { email: u.email }">
                {{ 'admin.users.list.editAction' | transloco }}
              </button>
              <button class="btn-action" (click)="openPasswordReset(u)"
                      [attr.aria-label]="'admin.users.list.resetPwdAria' | transloco: { email: u.email }">
                {{ 'admin.users.list.resetPwdAction' | transloco }}
              </button>
              <button
                class="btn-action"
                [class.btn-danger]="u.active"
                (click)="toggleActive(u)"
                [attr.aria-label]="(u.active ? ('admin.users.list.deactivateAria' | transloco: { email: u.email }) : ('admin.users.list.reactivateAria' | transloco: { email: u.email }))">
                {{ u.active ? ('admin.users.list.deactivateAction' | transloco) : ('admin.users.list.reactivateAction' | transloco) }}
              </button>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="6" class="state-msg">{{ 'admin.users.list.noResults' | transloco }}</td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <!-- Dialogs (rendered outside table for z-index) -->
    <app-create-user-dialog
      *ngIf="showCreate"
      (created)="onCreated()"
      (cancelled)="showCreate = false"
      (redirectToTherapistWizard)="onRedirectToTherapistWizard($event)">
    </app-create-user-dialog>

    <app-create-therapist-dialog
      *ngIf="showTherapistWizard"
      [prefilledData]="therapistPrefilledData"
      (created)="onTherapistCreated()"
      (cancelled)="showTherapistWizard = false">
    </app-create-therapist-dialog>

    <app-edit-user-dialog
      *ngIf="editTarget"
      [user]="editTarget"
      (updated)="onUpdated($event)"
      (cancelled)="editTarget = null">
    </app-edit-user-dialog>

    <app-password-reset-dialog
      *ngIf="resetTarget"
      [user]="resetTarget"
      (confirmed)="resetTarget = null"
      (cancelled)="resetTarget = null">
    </app-password-reset-dialog>
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
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }
    .close-btn {
      background: none;
      border: none;
      color: #DC2626;
      font-size: 1.5rem;
      line-height: 1;
      cursor: pointer;
      padding: 0;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0.6;
      transition: opacity 0.15s ease;
    }
    .close-btn:hover {
      opacity: 1;
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
    .sort-btn {
      background: none;
      border: none;
      cursor: pointer;
      font-weight: 600;
      font-size: inherit;
      padding: 0;
      color: inherit;
      text-transform: uppercase;
      letter-spacing: .03em;
    }
    .sort-btn:hover {
      color: var(--color-accent, #0EA5A0);
    }
    .badge {
      display: inline-block;
      padding: .35rem .75rem;
      border-radius: 6px;
      font-size: .8125rem;
      font-weight: 500;
      white-space: nowrap;
    }
    .badge-admin {
      background: #EFF6FF;
      color: #1E40AF;
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
    .name-link {
      background: none;
      border: none;
      padding: 0;
      color: var(--color-accent, #0EA5A0);
      cursor: pointer;
      font-size: inherit;
      text-decoration: none;
      font-weight: 500;
      transition: color 0.12s ease;
    }
    .name-link:hover {
      color: var(--color-accent-hover, #0C9490);
      text-decoration: underline;
    }
    .name-link:active {
      color: #0A8480;
    }
  `]
})
export class UserListComponent implements OnInit {
  readonly assignableRoles = ASSIGNABLE_ROLES;
  readonly roleLabels = ROLE_LABELS;

  users: UserSummary[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  first = 0;
  sortField = 'createdAt';
  sortDir = 'desc';

  roleFilter: UserRole | '' = '';
  statusFilter: '' | 'true' | 'false' = '';
  roleFilterOptions: { label: string; value: string }[] = [];
  statusFilterOptions: { label: string; value: string }[] = [];

  loading = false;
  loadError: string | null = null;
  errorMessage: string | null = null;

  showCreate = false;
  editTarget: UserSummary | null = null;
  resetTarget: UserSummary | null = null;

  showTherapistWizard = false;
  therapistPrefilledData: { fullName: string; email: string } | undefined;

  constructor(
    private userService: UserManagementService,
    private therapistService: TherapistManagementService,
    private router: Router,
    private route: ActivatedRoute,
    private transloco: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.roleFilterOptions = [
      { label: this.transloco.translate('admin.users.list.filterAllRoles'), value: '' },
      ...this.assignableRoles.map(r => ({ label: this.transloco.translate('roles.' + r), value: r })),
    ];
    this.statusFilterOptions = [
      { label: this.transloco.translate('admin.users.list.filterAllStatuses'), value: '' },
      { label: this.transloco.translate('admin.users.list.statusActive'), value: 'true' },
      { label: this.transloco.translate('admin.users.list.statusInactive'), value: 'false' },
    ];
    // Read initial filter state from query params if present
    this.route.queryParams.subscribe(params => {
      if (params['role']) {
        this.roleFilter = params['role'];
      }
      if (params['status']) {
        this.statusFilter = params['status'];
      }
    });
    
    this.loadPage();
  }

  /** Reload page when a filter changes. */
  onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.pageSize);
    this.pageSize = event.rows ?? this.pageSize;
    this.first = event.first ?? 0;
    if (event.sortField) {
      this.sortField = event.sortField as string;
    }
    this.sortDir = event.sortOrder === 1 ? 'asc' : 'desc';
    this.loadPage();
  }

  applyFilters(): void {
    // If therapist role is selected, navigate to dedicated therapist management page
    if (this.roleFilter === 'THERAPIST') {
      const queryParams: any = {};
      if (this.statusFilter !== '') {
        queryParams.status = this.statusFilter;
      }
      this.router.navigate(['/admin/therapists'], { queryParams });
      return;
    }

    this.currentPage = 0;
    this.first = 0;
    this.loadPage();
  }

  /** Navigate to a specific page. */
  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadPage();
  }

  /** Opens the create-user dialog. */
  openCreate(): void {
    this.showCreate = true;
  }

  /** Opens the edit dialog for a user. */
  openEdit(user: UserSummary): void {
    this.editTarget = user;
  }

  /** Opens the password reset confirmation dialog. */
  openPasswordReset(user: UserSummary): void {
    this.resetTarget = user;
  }

  /**
   * Navigates to the appropriate profile page based on user role.
   * - THERAPIST: Navigate to therapist profile
   * - Others: Currently no dedicated profile page, could be extended in future
   */
  navigateToProfile(user: UserSummary): void {
    // Clear any previous error messages
    this.errorMessage = null;

    // Use the roles array when available; fall back to the legacy single role field
    const allRoles = user.roles && user.roles.length > 0 ? user.roles : [user.role];
    const normalizedRoles = allRoles.map(r => r === 'USER' ? 'THERAPIST' : r);

    if (normalizedRoles.includes('THERAPIST')) {
      // Look up therapist profile by email and navigate to their profile
      this.therapistService.getTherapistByEmail(user.email).subscribe({
        next: (therapist) => {
          this.router.navigate(['/admin/therapists', therapist.id]);
        },
        error: (err) => {
          console.error('Failed to find therapist profile for user:', user.email, err);
          this.errorMessage = `No therapist profile found for ${user.fullName ?? user.email}. Please create a therapist profile first.`;
        }
      });
    } else {
      // For other roles, you could navigate to a general user profile page
      // or show user details in a dialog. For now, we'll just do nothing.
      // Future enhancement: navigate to a user detail page
      this.errorMessage = `Profile view is only available for therapists. User ${user.fullName ?? user.email} has role(s): ${normalizedRoles.join(', ')}.`;
    }
  }

  /**
   * Deactivates an active user or reactivates an inactive one via the PATCH endpoint.
   * Self-deactivation is prevented server-side (400 → error toast implied by dialog).
   */
  toggleActive(user: UserSummary): void {
    const patch = { active: !user.active };
    this.userService.updateUser(user.id, patch).subscribe({
      next: (updated) => {
        const idx = this.users.findIndex(u => u.id === updated.id);
        if (idx !== -1) {
          this.users = [
            ...this.users.slice(0, idx),
            updated,
            ...this.users.slice(idx + 1)
          ];
        }
      },
      error: () => {
        this.loadPage(); // re-sync on error
      }
    });
  }

  /** Called after a new user is successfully created. */
  onCreated(): void {
    this.showCreate = false;
    this.loadPage(); // refresh list to include new entry
  }

  /** 
   * Redirects from user creation to therapist wizard when THERAPIST role is selected.
   * Closes the user dialog and opens the therapist wizard with prefilled data.
   */
  onRedirectToTherapistWizard(data: { fullName: string; email: string }): void {
    this.showCreate = false;
    this.therapistPrefilledData = data;
    this.showTherapistWizard = true;
  }

  /**
   * Called after a therapist is successfully created via the wizard.
   * Refreshes the user list to show the newly created therapist user.
   */
  onTherapistCreated(): void {
    this.showTherapistWizard = false;
    this.therapistPrefilledData = undefined;
    this.loadPage(); // refresh list to include new entry
  }

  /** Called after a user is successfully updated. */
  onUpdated(user: UserSummary): void {
    this.editTarget = null;
    const idx = this.users.findIndex(u => u.id === user.id);
    if (idx !== -1) {
      this.users = [
        ...this.users.slice(0, idx),
        user,
        ...this.users.slice(idx + 1)
      ];
    }
  }

  // ---- private ----------------------------------------------------------

  private loadPage(): void {
    this.loading = true;
    this.loadError = null;

    const params = {
      page: this.currentPage,
      size: this.pageSize,
      sort: `${this.sortField},${this.sortDir}`,
      ...(this.roleFilter ? { role: this.roleFilter } : {}),
      ...(this.statusFilter !== '' ? { active: this.statusFilter === 'true' } : {})
    };

    this.userService.listUsers(params).subscribe({
      next: (page: UserPage) => {
        this.loading = false;
        this.users = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.currentPage = page.page;
      },
      error: () => {
        this.loading = false;
        this.loadError = 'Failed to load users. Please refresh the page.';
      }
    });
  }
}
