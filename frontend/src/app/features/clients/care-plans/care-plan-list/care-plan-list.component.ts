import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { jwtDecode } from 'jwt-decode';
import { AuthService } from '../../../../core/auth/auth.service';
import { CarePlanStatus, CarePlanSummary } from '../models/care-plan.model';
import { CarePlanService } from '../services/care-plan.service';
import { CarePlanFormDialogComponent } from '../care-plan-form-dialog/care-plan-form-dialog.component';

interface JwtPayload {
  roles: string[];
}

@Component({
  selector: 'app-care-plan-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslocoModule, CarePlanFormDialogComponent],
  template: `
    <div class="care-plans-panel">
      <div class="panel-header">
        <h3>{{ 'carePlans.title' | transloco }}</h3>
        <div class="header-actions">
          <select class="status-filter"
                  [(ngModel)]="statusFilter"
                  (ngModelChange)="onFilterChange($event)"
                  [attr.aria-label]="'carePlans.list.filterByStatus' | transloco">
            <option value="">{{ 'carePlans.list.allStatuses' | transloco }}</option>
            <option value="ACTIVE">{{ 'carePlans.status.active' | transloco }}</option>
            <option value="CLOSED">{{ 'carePlans.status.closed' | transloco }}</option>
            <option value="ARCHIVED">{{ 'carePlans.status.archived' | transloco }}</option>
          </select>
          <button *ngIf="canManage"
                  type="button"
                  class="btn-primary"
                  (click)="openCreateDialog()">
            {{ 'carePlans.list.newPlan' | transloco }}
          </button>
        </div>
      </div>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'carePlans.list.loading' | transloco }}
      </div>

      <div *ngIf="error" class="alert-error" role="alert">{{ error }}</div>

      <div *ngIf="!loading && !error && plans.length === 0" class="empty-state">
        {{ 'carePlans.list.empty' | transloco }}
      </div>

      <ul *ngIf="!loading && plans.length > 0" class="care-plan-list">
        <li *ngFor="let plan of plans" class="care-plan-item">
          <a [routerLink]="['/clients', clientId, 'care-plans', plan.id]" class="plan-link">
            <div class="plan-main">
              <span class="plan-title">{{ plan.title }}</span>
              <span class="badge-status" [ngClass]="'status-' + plan.status.toLowerCase()">
                {{ 'carePlans.status.' + plan.status.toLowerCase() | transloco }}
              </span>
            </div>
            <div class="plan-meta">
              <span>{{ 'carePlans.list.goals' | transloco: { count: plan.goalCount } }}</span>
              <span class="plan-date">{{ plan.updatedAt | date:'mediumDate' }}</span>
            </div>
          </a>
        </li>
      </ul>

      <app-care-plan-form-dialog
        *ngIf="showCreateDialog"
        [clientId]="clientId"
        (saved)="onPlanCreated()"
        (cancelled)="showCreateDialog = false">
      </app-care-plan-form-dialog>
    </div>
  `,
  styles: [`
    .care-plans-panel { margin-top: var(--spacing-lg); }
    .panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--spacing-md); }
    .panel-header h3 { margin: 0; }
    .header-actions { display: flex; gap: var(--spacing-sm); align-items: center; }
    .status-filter { padding: 6px 10px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.875rem; }
    .care-plan-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: var(--spacing-sm); }
    .care-plan-item { border: 1px solid var(--color-border); border-radius: var(--radius-md); }
    .plan-link { display: block; padding: var(--spacing-md); text-decoration: none; color: inherit; }
    .plan-link:hover { background: var(--color-surface-hover); border-radius: var(--radius-md); }
    .plan-main { display: flex; justify-content: space-between; align-items: center; }
    .plan-title { font-weight: 600; }
    .plan-meta { display: flex; justify-content: space-between; margin-top: var(--spacing-xs); font-size: 0.85rem; color: var(--color-text-muted); }
    .badge-status { font-size: 0.75rem; padding: 2px 8px; border-radius: var(--radius-full); font-weight: 500; }
    .status-active { background: #dcfce7; color: #166534; }
    .status-closed { background: #fee2e2; color: #991b1b; }
    .status-archived { background: #f3f4f6; color: #6b7280; }
    .empty-state { text-align: center; padding: var(--spacing-xl); color: var(--color-text-muted); }
    .state-msg { padding: var(--spacing-md); color: var(--color-text-muted); }
    .alert-error { padding: var(--spacing-sm) var(--spacing-md); background: #fee2e2; color: #991b1b; border-radius: var(--radius-sm); }
  `]
})
export class CarePlanListComponent implements OnInit {
  @Input({ required: true }) clientId!: string;

  plans: CarePlanSummary[] = [];
  loading = false;
  error: string | null = null;
  statusFilter: CarePlanStatus | '' = '';
  showCreateDialog = false;
  canManage = false;

  constructor(
    private carePlanService: CarePlanService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.canManage = this.hasAuthority('MANAGE_CARE_PLANS');
    this.loadPlans();
  }

  onFilterChange(_value?: string): void {
    this.loadPlans();
  }

  openCreateDialog(): void {
    this.showCreateDialog = true;
  }

  onPlanCreated(): void {
    this.showCreateDialog = false;
    this.loadPlans();
  }

  private loadPlans(): void {
    this.loading = true;
    this.error = null;
    const status = this.statusFilter || undefined;
    this.carePlanService.listByClient(this.clientId, status as CarePlanStatus | undefined).subscribe({
      next: plans => {
        this.plans = plans;
        this.loading = false;
      },
      error: () => {
        this.error = 'carePlans.list.loadError';
        this.loading = false;
      }
    });
  }

  private hasAuthority(authority: string): boolean {
    const token = this.authService.token;
    if (!token) return false;
    try {
      const decoded = jwtDecode<JwtPayload>(token);
      return (decoded.roles || []).includes(authority);
    } catch {
      return false;
    }
  }
}
