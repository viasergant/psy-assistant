import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { NgxChartsModule, Color, ScaleType } from '@swimlane/ngx-charts';
import { OutcomeMeasureChartData } from '../../models/care-plan.model';
import { OutcomeMeasureService } from '../../services/outcome-measure.service';

interface ChartSeries {
  name: string;
  series: { name: string; value: number }[];
}

@Component({
  selector: 'app-outcome-measure-chart',
  standalone: true,
  imports: [CommonModule, TranslocoModule, NgxChartsModule],
  template: `
    <div class="chart-section">
      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'carePlans.charts.loading' | transloco }}
      </div>
      <div *ngIf="error" class="alert-error" role="alert">{{ error | transloco }}</div>

      <div *ngIf="!loading && chartData && chartData.series.length >= 2">
        <h4 class="chart-title">{{ chartData.displayName }}</h4>

        <!-- ngx-charts line chart -->
        <div class="chart-wrapper"
             role="img"
             [attr.aria-label]="'carePlans.charts.outcomeAriaLabel' | transloco : { measure: chartData.displayName }">
          <ngx-charts-line-chart
            [view]="[560, 300]"
            [results]="chartSeries"
            [xAxis]="true"
            [yAxis]="true"
            [legend]="true"
            [showXAxisLabel]="true"
            [showYAxisLabel]="true"
            [xAxisLabel]="'carePlans.charts.dateAxis' | transloco"
            [yAxisLabel]="'carePlans.charts.scoreAxis' | transloco"
            [scheme]="colorScheme"
            [animations]="false"
          >
          </ngx-charts-line-chart>
        </div>

        <!-- Screen-reader accessible table (WCAG 2.1 AA) -->
        <table class="sr-only" [attr.aria-label]="chartData.displayName + ' data'">
          <caption class="sr-only">{{ chartData.displayName }}</caption>
          <thead>
            <tr>
              <th scope="col">{{ 'carePlans.outcomeMeasures.assessmentDate' | transloco }}</th>
              <th scope="col">{{ 'carePlans.outcomeMeasures.score' | transloco }}</th>
              <th scope="col">{{ 'carePlans.outcomeMeasures.alert' | transloco }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let point of chartData.series">
              <td>{{ point.date }}</td>
              <td>{{ point.score }}</td>
              <td>{{ point.thresholdBreached ? ('carePlans.outcomeMeasures.thresholdBreached' | transloco) : '' }}</td>
            </tr>
          </tbody>
        </table>

        <p *ngIf="chartData.alertThreshold != null" class="threshold-note">
          {{ 'carePlans.charts.thresholdNote' | transloco : { threshold: chartData.alertThreshold } }}
        </p>
      </div>

      <div *ngIf="!loading && (!chartData || chartData.series.length < 2)" class="empty-state">
        {{ 'carePlans.charts.insufficientData' | transloco }}
      </div>
    </div>
  `,
  styles: [`
    .chart-section { display: flex; flex-direction: column; gap: var(--spacing-md); }
    .chart-title { font-size: 0.95rem; font-weight: 600; margin: 0 0 var(--spacing-sm); }
    .chart-wrapper { overflow-x: auto; }
    .threshold-note { font-size: 0.8rem; color: var(--color-text-muted); margin: var(--spacing-xs) 0 0; }
    .empty-state { text-align: center; padding: var(--spacing-xl); color: var(--color-text-muted); }
    .state-msg { color: var(--color-text-muted); font-size: 0.875rem; }
    .alert-error { padding: var(--spacing-xs) var(--spacing-sm); background: #fee2e2; color: #991b1b; border-radius: var(--radius-sm); font-size: 0.875rem; }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; }
  `]
})
export class OutcomeMeasureChartComponent implements OnChanges {
  @Input() planId!: string;
  @Input() measureCode!: string;

  chartData: OutcomeMeasureChartData | null = null;
  chartSeries: ChartSeries[] = [];
  loading = false;
  error: string | null = null;

  readonly colorScheme: Color = {
    name: 'psy',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#3b82f6', '#ef4444']
  };

  constructor(private outcomeMeasureService: OutcomeMeasureService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['planId'] || changes['measureCode']) && this.planId && this.measureCode) {
      this.loadChartData();
    }
  }

  private loadChartData(): void {
    this.loading = true;
    this.error = null;
    this.outcomeMeasureService.getChartData(this.planId, this.measureCode).subscribe({
      next: (data) => {
        this.chartData = data;
        this.chartSeries = this.buildChartSeries(data);
        this.loading = false;
      },
      error: () => {
        this.error = 'carePlans.charts.loadError';
        this.loading = false;
      }
    });
  }

  private buildChartSeries(data: OutcomeMeasureChartData): ChartSeries[] {
    const scoreSeries: ChartSeries = {
      name: data.displayName,
      series: data.series.map(p => ({ name: p.date, value: p.score }))
    };

    const result: ChartSeries[] = [scoreSeries];

    // Add a horizontal threshold line as a second constant series for visual reference
    if (data.alertThreshold != null && data.series.length >= 2) {
      const thresholdSeries: ChartSeries = {
        name: 'Alert threshold',
        series: data.series.map(p => ({ name: p.date, value: data.alertThreshold! }))
      };
      result.push(thresholdSeries);
    }

    return result;
  }
}
