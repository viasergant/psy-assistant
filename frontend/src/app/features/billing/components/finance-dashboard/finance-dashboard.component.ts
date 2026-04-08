import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { FinanceDashboard } from '../../models/finance-dashboard.model';
import { FinanceDashboardService } from '../../services/finance-dashboard.service';

@Component({
  selector: 'app-finance-dashboard',
  standalone: true,
  imports: [CommonModule, TranslocoModule],
  template: `
    <div class="page-container">
      <h1 class="page-title">{{ 'billing.dashboard.title' | transloco }}</h1>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'common.loading' | transloco }}
      </div>
      <div *ngIf="error" class="alert-error" role="alert">{{ error }}</div>

      <ng-container *ngIf="dashboard && !loading">

        <!-- KPI cards -->
        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-label">{{ 'billing.dashboard.totalOutstanding' | transloco }}</div>
            <div class="kpi-value">{{ dashboard.totalOutstandingAmount | number:'1.2-2' }}</div>
          </div>
          <div class="kpi-card kpi-card--alert">
            <div class="kpi-label">{{ 'billing.dashboard.totalOverdue' | transloco }}</div>
            <div class="kpi-value">{{ dashboard.totalOverdueAmount | number:'1.2-2' }}</div>
          </div>
          <div class="kpi-card kpi-card--success">
            <div class="kpi-label">{{ 'billing.dashboard.collectedThisMonth' | transloco }}</div>
            <div class="kpi-value">{{ dashboard.collectedThisMonthAmount | number:'1.2-2' }}</div>
          </div>
        </div>

        <!-- Aging table -->
        <section aria-labelledby="aging-heading" style="margin-top: var(--spacing-xl)">
          <h2 id="aging-heading" class="section-title">
            {{ 'billing.dashboard.aging.title' | transloco }}
          </h2>
          <div class="table-wrapper">
            <table aria-label="{{ 'billing.dashboard.aging.title' | transloco }}">
              <thead>
                <tr>
                  <th scope="col">{{ 'billing.dashboard.aging.title' | transloco }}</th>
                  <th scope="col" class="text-right">{{ 'billing.dashboard.aging.amount' | transloco }}</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{{ 'billing.dashboard.aging.current030' | transloco }}</td>
                  <td class="text-right">{{ dashboard.aging.current030 | number:'1.2-2' }}</td>
                </tr>
                <tr>
                  <td>{{ 'billing.dashboard.aging.past3160' | transloco }}</td>
                  <td class="text-right">{{ dashboard.aging.past3160 | number:'1.2-2' }}</td>
                </tr>
                <tr>
                  <td>{{ 'billing.dashboard.aging.past60plus' | transloco }}</td>
                  <td class="text-right">{{ dashboard.aging.past60plus | number:'1.2-2' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- As-of timestamp -->
        <p class="meta-text" aria-live="polite">
          {{ 'billing.dashboard.asOf' | transloco : { time: (dashboard.asOf | date:'medium') } }}
        </p>
      </ng-container>
    </div>
  `,
})
export class FinanceDashboardComponent implements OnInit {
  dashboard: FinanceDashboard | null = null;
  loading = false;
  error: string | null = null;

  constructor(private dashboardService: FinanceDashboardService) {}

  ngOnInit(): void {
    this.loading = true;
    this.dashboardService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'billing.dashboard.loadError';
        this.loading = false;
      },
    });
  }
}
