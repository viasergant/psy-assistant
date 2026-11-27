import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Observable, Subject, debounceTime, interval, startWith, takeUntil } from 'rxjs';
import { ASSIGNABLE_ROLES, UserRole } from '../../../../admin/users/models/user.model';
import { GlobalUser, GlobalUserStats } from '../../models/global-user.model';
import { GlobalUserService } from '../../services/global-user.service';

/** How often the summary counts are refreshed. */
const STATS_REFRESH_MS = 60_000;

interface FilterOption<T> {
  label: string;
  value: T;
}

/**
 * List of all registered users across all organizations with suspend, reactivate
 * and delete actions (global administrator only).
 */
@Component({
  selector: 'app-global-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoPipe, Select, TableModule],
  template: `
    <div class="page">
      <header class="page-header">
        <h2>{{ 'globalAdmin.users.pageTitle' | transloco }}</h2>
      </header>

      <div class="stats" role="status" aria-live="polite">
        <div class="stat">
          <span class="stat-value">{{ stats?.total ?? '—' }}</span>
          <span class="stat-label">{{ 'globalAdmin.users.stats.total' | transloco }}</span>
        </div>
        <div class="stat stat-online">
          <span class="stat-value">{{ stats?.online ?? '—' }}</span>
          <span class="stat-label">{{ 'globalAdmin.users.stats.online' | transloco }}</span>
        </div>
        <div class="stat stat-blocked">
          <span class="stat-value">{{ stats?.blocked ?? '—' }}</span>
          <span class="stat-label">{{ 'globalAdmin.users.stats.blocked' | transloco }}</span>
        </div>
      </div>

      <div class="filters" role="group" [attr.aria-label]="'globalAdmin.users.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="gaSearch">{{ 'globalAdmin.users.searchLabel' | transloco }}</label>
          <input
            id="gaSearch"
            type="search"
            class="search-input"
            [(ngModel)]="search"
            (ngModelChange)="search$.next($event)"
            [placeholder]="'globalAdmin.users.searchPlaceholder' | transloco"
          />
        </div>
        <div class="filter-group">
          <label for="gaRole">{{ 'globalAdmin.users.roleLabel' | transloco }}</label>
          <p-select
            inputId="gaRole"
            [(ngModel)]="roleFilter"
            [options]="roleOptions"
            optionLabel="label"
            optionValue="value"
            (onChange)="applyFilters()"
          />
        </div>
        <div class="filter-group">
          <label for="gaStatus">{{ 'globalAdmin.users.statusLabel' | transloco }}</label>
          <p-select
            inputId="gaStatus"
            [(ngModel)]="statusFilter"
            [options]="statusOptions"
            optionLabel="label"
            optionValue="value"
            (onChange)="applyFilters()"
          />
        </div>
      </div>

      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>
      <div *ngIf="actionError" class="alert-error" role="alert">{{ actionError }}</div>

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
              {{ 'globalAdmin.users.tableHeaders.name' | transloco }}
              <p-sortIcon field="fullName" />
            </th>
            <th pSortableColumn="email">
              {{ 'globalAdmin.users.tableHeaders.email' | transloco }}
              <p-sortIcon field="email" />
            </th>
            <th>{{ 'globalAdmin.users.tableHeaders.roles' | transloco }}</th>
            <th>{{ 'globalAdmin.users.tableHeaders.organization' | transloco }}</th>
            <th pSortableColumn="active">
              {{ 'globalAdmin.users.tableHeaders.status' | transloco }}
              <p-sortIcon field="active" />
            </th>
            <th pSortableColumn="createdAt">
              {{ 'globalAdmin.users.tableHeaders.created' | transloco }}
              <p-sortIcon field="createdAt" />
            </th>
            <th>{{ 'globalAdmin.users.tableHeaders.actions' | transloco }}</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-u>
          <tr>
            <td>{{ u.fullName ?? '—' }}</td>
            <td>{{ u.email }}</td>
            <td>
              <span *ngFor="let r of u.roles" class="badge">{{ 'roles.' + r | transloco }}</span>
            </td>
            <td>
              <ng-container *ngIf="u.orgName; else noOrg">
                {{ u.orgName }}
                <span class="badge badge-muted">{{ 'globalAdmin.users.orgType.' + u.orgType | transloco }}</span>
              </ng-container>
              <ng-template #noOrg>—</ng-template>
            </td>
            <td>
              <span
                class="presence"
                [class.presence-online]="u.presence === 'ONLINE'"
                [class.presence-offline]="u.presence === 'OFFLINE'"
                [class.presence-blocked]="u.presence === 'BLOCKED'"
                [attr.title]="u.lastSeenAt
                  ? (('globalAdmin.users.lastSeen' | transloco) + ' ' + (u.lastSeenAt | date:'dd MMM yyyy HH:mm'))
                  : ('globalAdmin.users.neverSeen' | transloco)"
              >
                <span class="presence-dot" aria-hidden="true"></span>
                {{ 'globalAdmin.users.presence.' + u.presence | transloco }}
                <ng-container *ngIf="u.blockReason">
                  ({{ 'globalAdmin.users.blockReason.' + u.blockReason | transloco }})
                </ng-container>
              </span>
            </td>
            <td>{{ u.createdAt | date:'dd MMM yyyy' }}</td>
            <td class="actions">
              <button
                type="button"
                class="btn-action"
                [disabled]="busyUserId === u.id"
                (click)="toggleSuspension(u)"
              >
                {{ (u.active ? 'globalAdmin.users.suspend' : 'globalAdmin.users.reactivate') | transloco }}
              </button>
              <button
                type="button"
                class="btn-action btn-danger"
                [disabled]="busyUserId === u.id"
                (click)="remove(u)"
              >
                {{ 'globalAdmin.users.delete' | transloco }}
              </button>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="7" class="state-msg">{{ 'globalAdmin.users.noResults' | transloco }}</td>
          </tr>
        </ng-template>
      </p-table>
    </div>
  `,
  styles: [`
    .page { padding: 2rem; max-width: 1400px; margin: 0 auto; }
    .page-header h2 { margin: 0 0 1.5rem; font-size: 1.5rem; font-weight: 600; }
    .filters {
      display: flex; flex-wrap: wrap; gap: 1.5rem; margin-bottom: 1.5rem; padding: 1.25rem;
      background: var(--color-surface, #fff); border-radius: 12px;
      border: 1.5px solid var(--color-border, #E2E8F0);
    }
    .filter-group { display: flex; flex-direction: column; gap: .4rem; }
    .filter-group label { font-size: .875rem; font-weight: 500; color: var(--color-text-secondary, #64748B); }
    .search-input {
      padding: .55rem .875rem; min-width: 240px; font-family: inherit; font-size: .9375rem;
      border: 1.5px solid var(--color-border, #E2E8F0); border-radius: 8px;
    }
    .state-msg { padding: 3rem 2rem; text-align: center; color: var(--color-text-secondary, #64748B); }
    .alert-error {
      padding: .875rem 1.125rem; margin-bottom: 1.5rem; border-radius: 10px; font-size: .875rem;
      background: var(--color-error-bg, #FEF2F2); border: 1px solid var(--color-error-border, #FECACA);
      color: var(--color-error, #DC2626);
    }
    .badge {
      display: inline-block; padding: .25rem .6rem; margin-right: 4px; border-radius: 6px;
      font-size: .8125rem; font-weight: 500; white-space: nowrap; background: #EFF6FF; color: #1E40AF;
    }
    .badge-muted { background: #F3F4F6; color: #6B7280; }
    .badge-active { background: #DCFCE7; color: #166534; }
    .badge-inactive { background: #F3F4F6; color: #6B7280; }
    .stats { display: flex; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem; }
    .stat {
      display: flex; flex-direction: column; min-width: 140px; padding: 1rem 1.25rem;
      background: var(--color-surface, #fff); border-radius: 12px;
      border: 1.5px solid var(--color-border, #E2E8F0);
    }
    .stat-value { font-size: 1.75rem; font-weight: 600; line-height: 1.2; }
    .stat-label { font-size: .875rem; color: var(--color-text-secondary, #64748B); }
    .stat-online .stat-value { color: #166534; }
    .stat-blocked .stat-value { color: var(--color-error, #DC2626); }
    .presence { display: inline-flex; align-items: center; gap: .4rem; font-size: .875rem; white-space: nowrap; }
    .presence-dot { width: .6rem; height: .6rem; border-radius: 50%; background: #9CA3AF; }
    .presence-online .presence-dot { background: #16A34A; }
    .presence-blocked { color: var(--color-error, #DC2626); }
    .presence-blocked .presence-dot { background: var(--color-error, #DC2626); }
    .actions { white-space: nowrap; }
    .btn-action {
      padding: .3rem .7rem; margin-right: 6px; font-family: inherit; font-size: .8125rem; cursor: pointer;
      border: 1.5px solid var(--color-border, #E2E8F0); border-radius: 6px; background: var(--color-surface, #fff);
    }
    .btn-action:disabled { opacity: .5; cursor: not-allowed; }
    .btn-danger { color: var(--color-error, #DC2626); border-color: var(--color-error-border, #FECACA); }
  `]
})
export class GlobalUserListComponent implements OnInit, OnDestroy {
  users: GlobalUser[] = [];
  totalElements = 0;
  pageSize = 20;
  first = 0;
  sortField = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';
  loading = false;
  loadError: string | null = null;
  stats: GlobalUserStats | null = null;
  actionError: string | null = null;
  busyUserId: string | null = null;

  search = '';
  roleFilter: UserRole | null = null;
  statusFilter: boolean | null = null;
  roleOptions: FilterOption<UserRole | null>[] = [];
  statusOptions: FilterOption<boolean | null>[] = [];

  readonly search$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly service: GlobalUserService,
    private readonly transloco: TranslocoService
  ) {}

  ngOnInit(): void {
    this.buildFilterOptions();
    this.search$.pipe(debounceTime(300), takeUntil(this.destroy$)).subscribe(() => this.applyFilters());
    interval(STATS_REFRESH_MS).pipe(startWith(0), takeUntil(this.destroy$)).subscribe(() => this.loadStats());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  applyFilters(): void {
    this.first = 0;
    this.load();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.pageSize = event.rows ?? this.pageSize;
    this.first = event.first ?? 0;
    if (typeof event.sortField === 'string') {
      this.sortField = event.sortField;
      this.sortDir = event.sortOrder === 1 ? 'asc' : 'desc';
    }
    this.load();
  }

  toggleSuspension(user: GlobalUser): void {
    const key = user.active ? 'suspend' : 'reactivate';
    if (!confirm(this.transloco.translate(`globalAdmin.users.confirm.${key}`, { name: user.email }))) {
      return;
    }
    const call = user.active ? this.service.suspendUser(user.id) : this.service.reactivateUser(user.id);
    this.runAction(user, call);
  }

  remove(user: GlobalUser): void {
    if (!confirm(this.transloco.translate('globalAdmin.users.confirm.delete', { name: user.email }))) {
      return;
    }
    this.runAction(user, this.service.deleteUser(user.id));
  }

  private runAction(user: GlobalUser, call: Observable<void>): void {
    this.busyUserId = user.id;
    this.actionError = null;
    call.subscribe({
      next: () => {
        this.busyUserId = null;
        this.load();
        this.loadStats();
      },
      error: (err: HttpErrorResponse) => {
        this.busyUserId = null;
        this.actionError = this.actionErrorMessage(err);
      },
    });
  }

  private actionErrorMessage(err: HttpErrorResponse): string {
    const code = err.error?.code;
    if (code === 'USER_HAS_HISTORY') {
      return this.transloco.translate('globalAdmin.users.errors.hasHistory');
    }
    if (code === 'SELF_DEACTIVATION_FORBIDDEN') {
      return this.transloco.translate('globalAdmin.users.errors.protectedAccount');
    }
    return this.transloco.translate('globalAdmin.users.errors.actionFailed');
  }

  private loadStats(): void {
    // Best effort: a failed refresh keeps the previous numbers instead of surfacing an error.
    this.service.getStats().subscribe({
      next: (stats) => (this.stats = stats),
      error: () => {},
    });
  }

  private load(): void {
    this.loading = true;
    this.loadError = null;
    this.service
      .listUsers({
        page: Math.floor(this.first / this.pageSize),
        size: this.pageSize,
        sort: `${this.sortField},${this.sortDir}`,
        role: this.roleFilter ?? undefined,
        active: this.statusFilter ?? undefined,
        search: this.search,
      })
      .subscribe({
        next: (page) => {
          this.users = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.loadError = this.transloco.translate('globalAdmin.users.loadError');
          this.loading = false;
        },
      });
  }

  private buildFilterOptions(): void {
    this.roleOptions = [
      { label: this.transloco.translate('globalAdmin.users.filterAllRoles'), value: null },
      ...ASSIGNABLE_ROLES.map((r) => ({ label: this.transloco.translate(`roles.${r}`), value: r })),
    ];
    this.statusOptions = [
      { label: this.transloco.translate('globalAdmin.users.filterAllStatuses'), value: null },
      { label: this.transloco.translate('globalAdmin.users.statusActive'), value: true },
      { label: this.transloco.translate('globalAdmin.users.statusInactive'), value: false },
    ];
  }
}
