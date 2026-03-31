import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
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
    CreateUserDialogComponent,
    EditUserDialogComponent,
    PasswordResetDialogComponent,
    CreateTherapistDialogComponent,
  ],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>User Management</h1>
        <button class="btn-primary" (click)="openCreate()">+ Create user</button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" [attr.aria-label]="'admin.users.list.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="roleFilter">{{ 'admin.users.list.roleLabel' | transloco }}</label>
          <select id="roleFilter" [(ngModel)]="roleFilter" (change)="applyFilters()">
            <option value="">All roles</option>
            <option *ngFor="let r of assignableRoles" [value]="r">{{ roleLabels[r] }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label for="statusFilter">{{ 'admin.users.list.statusLabel' | transloco }}</label>
          <select id="statusFilter" [(ngModel)]="statusFilter" (change)="applyFilters()">
            <option value="">All statuses</option>
            <option value="true">{{ 'admin.users.list.statusActive' | transloco }}</option>
            <option value="false">{{ 'admin.users.list.statusInactive' | transloco }}</option>
          </select>
        </div>
      </div>

      <!-- Loading / empty / error states -->
      <div *ngIf="loading" class="state-msg" aria-live="polite">Loading…</div>
      <div *ngIf="!loading && loadError" class="alert-error" role="alert">{{ loadError }}</div>
      <div *ngIf="!loading && errorMessage" class="alert-error" role="alert">
        {{ errorMessage }}
        <button class="close-btn" (click)="errorMessage = null" [attr.aria-label]="'admin.users.list.closeError' | transloco">&times;</button>
      </div>
      <div *ngIf="!loading && !loadError && users.length === 0" class="state-msg">
        No users match the current filters.
      </div>

      <!-- Table -->
      <div class="table-wrapper" *ngIf="!loading && users.length > 0">
        <table [attr.aria-label]="'admin.users.list.ariaList' | transloco">
          <thead>
            <tr>
              <th scope="col">
                <button class="sort-btn" (click)="sort('fullName')" type="button"
                        [attr.aria-sort]="ariaSort('fullName')">
                  Name
                </button>
              </th>
              <th scope="col">{{ 'admin.users.list.tableHeaders.email' | transloco }}</th>
              <th scope="col">
                <button class="sort-btn" (click)="sort('role')" type="button"
                        [attr.aria-sort]="ariaSort('role')">
                  Role
                </button>
              </th>
              <th scope="col">{{ 'admin.users.list.tableHeaders.status' | transloco }}</th>
              <th scope="col">
                <button class="sort-btn" (click)="sort('createdAt')" type="button"
                        [attr.aria-sort]="ariaSort('createdAt')">
                  Created
                </button>
              </th>
              <th scope="col"><span class="sr-only">{{ 'admin.users.list.tableHeaders.actions' | transloco }}</span></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let u of users">
              <td>
                <button 
                  class="name-link" 
                  (click)="navigateToProfile(u)"
                  [attr.aria-label]="'View profile for ' + (u.fullName ?? u.email)">
                  {{ u.fullName ?? '—' }}
                </button>
              </td>
              <td>{{ u.email }}</td>
              <td>
                <span class="badge" [class.badge-admin]="u.role === 'SYSTEM_ADMINISTRATOR' || u.role === 'ADMIN'">
                  {{ roleLabels[u.role] || u.role }}
                </span>
              </td>
              <td>
                <span class="badge" [class.badge-active]="u.active" [class.badge-inactive]="!u.active">
                  {{ u.active ? 'Active' : 'Inactive' }}
                </span>
              </td>
              <td>{{ u.createdAt | date:'dd MMM yyyy' }}</td>
              <td class="actions-cell">
                <button class="btn-action" (click)="openEdit(u)" aria-label="Edit {{ u.email }}">
                  Edit
                </button>
                <button class="btn-action" (click)="openPasswordReset(u)"
                        aria-label="Reset password for {{ u.email }}">
                  Reset pwd
                </button>
                <button
                  class="btn-action"
                  [class.btn-danger]="u.active"
                  (click)="toggleActive(u)"
                  [attr.aria-label]="(u.active ? 'Deactivate ' : 'Reactivate ') + u.email">
                  {{ u.active ? 'Deactivate' : 'Reactivate' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <nav class="pagination" [attr.aria-label]="'admin.users.list.ariaPagination' | transloco" *ngIf="totalPages > 1">
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
        {{ totalElements }} user{{ totalElements !== 1 ? 's' : '' }} found
      </p>
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
  sortField = 'createdAt';
  sortDir = 'desc';

  roleFilter: UserRole | '' = '';
  statusFilter: '' | 'true' | 'false' = '';

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
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
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
    this.loadPage();
  }

  /** Toggle sort direction or switch sort field. */
  sort(field: string): void {
    if (this.sortField === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDir = 'asc';
    }
    this.currentPage = 0;
    this.loadPage();
  }

  /** Returns the aria-sort attribute value for a column. */
  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    if (this.sortField !== field) return 'none';
    return this.sortDir === 'asc' ? 'ascending' : 'descending';
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
    
    // Normalize role handling for legacy values
    const role = user.role === 'USER' ? 'THERAPIST' : user.role;
    
    if (role === 'THERAPIST') {
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
      this.errorMessage = `Profile view is only available for therapists. User ${user.fullName ?? user.email} has role: ${role}.`;
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
