import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { jwtDecode } from 'jwt-decode';
import { AuthService } from '../../../../core/auth/auth.service';
import { CarePlanDetail, GoalResponse, GoalStatus } from '../models/care-plan.model';
import { CarePlanService } from '../services/care-plan.service';

interface JwtPayload {
  roles: string[];
}

@Component({
  selector: 'app-care-plan-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule],
  template: `
    <div class="page">
      <a class="back-link" [routerLink]="['../']">&larr; {{ 'carePlans.detail.backToPlans' | transloco }}</a>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'carePlans.detail.loading' | transloco }}
      </div>

      <div *ngIf="error" class="alert-error" role="alert">{{ error }}</div>

      <div *ngIf="plan && !loading">
        <header class="page-header">
          <div>
            <h1>{{ plan.title }}</h1>
            <p class="sub" *ngIf="plan.description">{{ plan.description }}</p>
          </div>
          <span class="badge-status" [ngClass]="'status-' + plan.status.toLowerCase()">
            {{ 'carePlans.status.' + plan.status.toLowerCase() | transloco }}
          </span>
        </header>

        <div *ngIf="actionError" class="alert-error" role="alert">{{ actionError }}</div>

        <div *ngIf="canManage && plan.status === 'ACTIVE'" class="toolbar">
          <button type="button" class="btn-ghost" (click)="closePlan()">
            {{ 'carePlans.detail.close' | transloco }}
          </button>
        </div>

        <div *ngIf="canManage && plan.status === 'CLOSED'" class="toolbar">
          <button type="button" class="btn-ghost" (click)="archivePlan()">
            {{ 'carePlans.detail.archive' | transloco }}
          </button>
        </div>

        <section class="section">
          <h2 class="section-title">{{ 'carePlans.detail.goals' | transloco }}</h2>

          <div *ngIf="plan.goals.length === 0" class="empty-state">
            {{ 'carePlans.detail.noGoals' | transloco }}
          </div>

          <div *ngFor="let goal of plan.goals; let i = index" class="goal-card">
            <div class="goal-header" (click)="toggleGoal(i)" [attr.aria-expanded]="expandedGoals.has(i)">
              <span class="goal-priority">#{{ goal.priority }}</span>
              <span class="goal-description">{{ goal.description }}</span>
              <span class="badge-status" [ngClass]="goalStatusClass(goal.status)">
                {{ 'carePlans.goalStatus.' + goal.status.toLowerCase() | transloco }}
              </span>
              <span class="goal-toggle">{{ expandedGoals.has(i) ? '▲' : '▼' }}</span>
            </div>

            <div *ngIf="expandedGoals.has(i)" class="goal-body">
              <div *ngIf="goal.targetDate" class="goal-meta">
                {{ 'carePlans.detail.targetDate' | transloco }}: {{ goal.targetDate | date:'mediumDate' }}
              </div>

              <div *ngIf="canManage && plan.status === 'ACTIVE'" class="goal-status-actions">
                <label class="field-inline">
                  <span>{{ 'carePlans.detail.goalStatus' | transloco }}</span>
                  <select (change)="updateGoalStatus(goal, $any($event.target).value)">
                    <option *ngFor="let s of goalStatuses" [value]="s" [selected]="goal.status === s">
                      {{ 'carePlans.goalStatus.' + s.toLowerCase() | transloco }}
                    </option>
                  </select>
                </label>
              </div>

              <!-- Interventions -->
              <div class="sub-section" *ngIf="goal.interventions.length > 0">
                <h4>{{ 'carePlans.detail.interventions' | transloco }}</h4>
                <ul class="intervention-list">
                  <li *ngFor="let inv of goal.interventions" class="intervention-item">
                    <span class="inv-type badge-type">{{ inv.interventionType }}</span>
                    <span class="inv-desc">{{ inv.description }}</span>
                    <span *ngIf="inv.frequency" class="inv-freq">{{ inv.frequency }}</span>
                    <span class="badge-status" [ngClass]="'inv-' + inv.status.toLowerCase()">
                      {{ 'carePlans.interventionStatus.' + inv.status.toLowerCase() | transloco }}
                    </span>
                  </li>
                </ul>
              </div>

              <!-- Milestones -->
              <div class="sub-section" *ngIf="goal.milestones.length > 0">
                <h4>{{ 'carePlans.detail.milestones' | transloco }}</h4>
                <ul class="milestone-list">
                  <li *ngFor="let ms of goal.milestones" class="milestone-item" [class.achieved]="ms.achievedAt">
                    <span class="ms-check">{{ ms.achievedAt ? '✓' : '○' }}</span>
                    <span class="ms-desc">{{ ms.description }}</span>
                    <span *ngIf="ms.targetDate && !ms.achievedAt" class="ms-date">
                      {{ ms.targetDate | date:'shortDate' }}
                    </span>
                    <button *ngIf="canManage && plan.status === 'ACTIVE' && !ms.achievedAt"
                            type="button"
                            class="btn-ghost btn-xs"
                            (click)="achieveMilestone(goal.id, ms.id)">
                      {{ 'carePlans.detail.markAchieved' | transloco }}
                    </button>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: var(--spacing-lg); max-width: 900px; margin: 0 auto; }
    .back-link { color: var(--color-accent); text-decoration: none; font-size: 0.9rem; }
    .back-link:hover { text-decoration: underline; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin: var(--spacing-lg) 0; }
    .page-header h1 { margin: 0; }
    .sub { margin: 4px 0 0; color: var(--color-text-muted); }
    .toolbar { margin-bottom: var(--spacing-md); display: flex; gap: var(--spacing-sm); }
    .section { margin-top: var(--spacing-xl); }
    .section-title { font-size: 1.1rem; font-weight: 600; border-bottom: 1px solid var(--color-border); padding-bottom: var(--spacing-sm); }
    .goal-card { border: 1px solid var(--color-border); border-radius: var(--radius-md); margin-bottom: var(--spacing-sm); }
    .goal-header { display: flex; align-items: center; gap: var(--spacing-sm); padding: var(--spacing-md); cursor: pointer; user-select: none; }
    .goal-header:hover { background: var(--color-surface-hover); border-radius: var(--radius-md); }
    .goal-priority { font-weight: 700; color: var(--color-text-muted); min-width: 2rem; }
    .goal-description { flex: 1; font-weight: 500; }
    .goal-toggle { margin-left: auto; color: var(--color-text-muted); }
    .goal-body { padding: 0 var(--spacing-md) var(--spacing-md); border-top: 1px solid var(--color-border); }
    .goal-meta { font-size: 0.85rem; color: var(--color-text-muted); margin-top: var(--spacing-sm); }
    .goal-status-actions { margin-top: var(--spacing-sm); }
    .field-inline { display: flex; align-items: center; gap: var(--spacing-sm); font-size: 0.875rem; }
    .field-inline select { padding: 4px 8px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .sub-section { margin-top: var(--spacing-md); }
    .sub-section h4 { font-size: 0.875rem; font-weight: 600; margin: 0 0 var(--spacing-xs); color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .intervention-list, .milestone-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
    .intervention-item, .milestone-item { display: flex; align-items: center; gap: var(--spacing-sm); font-size: 0.875rem; padding: 6px 0; }
    .inv-type { font-size: 0.7rem; }
    .inv-desc { flex: 1; }
    .inv-freq { color: var(--color-text-muted); font-size: 0.8rem; }
    .ms-check { font-size: 1rem; min-width: 1.5rem; }
    .ms-desc { flex: 1; }
    .ms-date { color: var(--color-text-muted); font-size: 0.8rem; }
    .milestone-item.achieved .ms-desc { text-decoration: line-through; color: var(--color-text-muted); }
    .badge-status { font-size: 0.75rem; padding: 2px 8px; border-radius: var(--radius-full); font-weight: 500; }
    .badge-type { background: #ede9fe; color: #5b21b6; padding: 2px 6px; border-radius: var(--radius-sm); }
    .status-active { background: #dcfce7; color: #166534; }
    .status-closed { background: #fee2e2; color: #991b1b; }
    .status-archived { background: #f3f4f6; color: #6b7280; }
    .goal-pending { background: #fef9c3; color: #854d0e; }
    .goal-in_progress { background: #dbeafe; color: #1e40af; }
    .goal-achieved { background: #dcfce7; color: #166534; }
    .goal-abandoned { background: #f3f4f6; color: #6b7280; }
    .inv-active { background: #dbeafe; color: #1e40af; }
    .inv-completed { background: #dcfce7; color: #166534; }
    .inv-discontinued { background: #f3f4f6; color: #6b7280; }
    .btn-xs { padding: 2px 8px; font-size: 0.75rem; }
    .empty-state { text-align: center; padding: var(--spacing-xl); color: var(--color-text-muted); }
    .state-msg { padding: var(--spacing-md); color: var(--color-text-muted); }
    .alert-error { padding: var(--spacing-sm) var(--spacing-md); background: #fee2e2; color: #991b1b; border-radius: var(--radius-sm); margin-bottom: var(--spacing-md); }
  `]
})
export class CarePlanDetailComponent implements OnInit {
  plan: CarePlanDetail | null = null;
  loading = false;
  error: string | null = null;
  actionError: string | null = null;
  planId!: string;
  expandedGoals = new Set<number>();
  canManage = false;

  readonly goalStatuses: GoalStatus[] = ['PENDING', 'IN_PROGRESS', 'ACHIEVED', 'ABANDONED'];

  constructor(
    private route: ActivatedRoute,
    private carePlanService: CarePlanService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.planId = this.route.snapshot.paramMap.get('planId')!;
    this.canManage = this.hasAuthority('MANAGE_CARE_PLANS');
    this.loadPlan();
  }

  toggleGoal(index: number): void {
    if (this.expandedGoals.has(index)) {
      this.expandedGoals.delete(index);
    } else {
      this.expandedGoals.add(index);
    }
  }

  goalStatusClass(status: GoalStatus): string {
    return `goal-${status.toLowerCase().replace('_', '_')}`;
  }

  updateGoalStatus(goal: GoalResponse, status: GoalStatus): void {
    if (goal.status === status) return;
    this.actionError = null;
    this.carePlanService.updateGoalStatus(this.planId, goal.id, { status }).subscribe({
      next: updated => {
        goal.status = updated.status;
      },
      error: () => {
        this.actionError = 'carePlans.detail.updateError';
      }
    });
  }

  achieveMilestone(goalId: string, milestoneId: string): void {
    this.actionError = null;
    this.carePlanService.achieveMilestone(this.planId, goalId, milestoneId).subscribe({
      next: () => this.loadPlan(),
      error: () => {
        this.actionError = 'carePlans.detail.updateError';
      }
    });
  }

  closePlan(): void {
    this.actionError = null;
    this.carePlanService.close(this.planId).subscribe({
      next: updated => {
        this.plan = updated;
      },
      error: () => {
        this.actionError = 'carePlans.detail.updateError';
      }
    });
  }

  archivePlan(): void {
    this.actionError = null;
    this.carePlanService.archive(this.planId).subscribe({
      next: updated => {
        this.plan = updated;
      },
      error: () => {
        this.actionError = 'carePlans.detail.updateError';
      }
    });
  }

  private loadPlan(): void {
    this.loading = true;
    this.error = null;
    this.carePlanService.getDetail(this.planId).subscribe({
      next: plan => {
        this.plan = plan;
        this.loading = false;
      },
      error: () => {
        this.error = 'carePlans.detail.loadError';
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
