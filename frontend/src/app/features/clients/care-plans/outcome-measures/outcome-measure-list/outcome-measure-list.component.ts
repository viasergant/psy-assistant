import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { OutcomeMeasureEntry } from '../../models/care-plan.model';
import { OutcomeMeasureService } from '../../services/outcome-measure.service';
import { OutcomeMeasureEntryDialogComponent } from '../outcome-measure-entry-dialog/outcome-measure-entry-dialog.component';

@Component({
  selector: 'app-outcome-measure-list',
  standalone: true,
  imports: [CommonModule, DatePipe, TranslocoModule, OutcomeMeasureEntryDialogComponent],
  template: `
    <div class="om-section">

      <!-- Alert banner -->
      <div *ngIf="breachedCount > 0" class="alert-banner" role="alert">
        {{ 'carePlans.outcomeMeasures.thresholdAlert' | transloco : { count: breachedCount } }}
      </div>

      <div class="om-toolbar">
        <button *ngIf="canManage && isActive"
                type="button"
                class="btn btn-primary btn-sm"
                (click)="showDialog = true">
          + {{ 'carePlans.outcomeMeasures.record' | transloco }}
        </button>
      </div>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'carePlans.outcomeMeasures.loading' | transloco }}
      </div>
      <div *ngIf="error" class="alert-error" role="alert">{{ error | transloco }}</div>

      <div *ngIf="!loading && entries.length === 0" class="empty-state">
        {{ 'carePlans.outcomeMeasures.empty' | transloco }}
      </div>

      <div *ngIf="!loading && entries.length > 0" class="table-wrapper">
        <table class="data-table" [attr.aria-label]="'carePlans.outcomeMeasures.tableLabel' | transloco">
          <thead>
            <tr>
              <th scope="col">{{ 'carePlans.outcomeMeasures.measure' | transloco }}</th>
              <th scope="col">{{ 'carePlans.outcomeMeasures.score' | transloco }}</th>
              <th scope="col">{{ 'carePlans.outcomeMeasures.assessmentDate' | transloco }}</th>
              <th scope="col">{{ 'carePlans.outcomeMeasures.recordedBy' | transloco }}</th>
              <th scope="col">{{ 'carePlans.outcomeMeasures.alert' | transloco }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let entry of entries" [class.row-alert]="entry.thresholdBreached">
              <td>{{ entry.measureDisplayName }}</td>
              <td class="score-cell">{{ entry.score }}</td>
              <td>{{ entry.assessmentDate | date:'shortDate' }}</td>
              <td>{{ entry.recordedByName }}</td>
              <td>
                <span *ngIf="entry.thresholdBreached"
                      class="threshold-badge"
                      [class.badge-alert]="entry.alertSeverity === 'ALERT'"
                      [class.badge-warning]="entry.alertSeverity === 'WARNING'"
                      [attr.title]="entry.alertLabel">
                  ⚠ {{ entry.alertLabel }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-outcome-measure-entry-dialog
        [planId]="planId"
        [(visible)]="showDialog"
        (entryRecorded)="onEntryRecorded($event)"
      ></app-outcome-measure-entry-dialog>
    </div>
  `,
  styles: [`
    .om-section { display: flex; flex-direction: column; gap: var(--spacing-md); }
    .alert-banner { background: #fef3c7; border: 1px solid #f59e0b; color: #92400e; border-radius: var(--radius-sm); padding: var(--spacing-sm) var(--spacing-md); font-size: 0.875rem; font-weight: 500; }
    .om-toolbar { display: flex; justify-content: flex-end; }
    .btn-sm { padding: 4px 12px; font-size: 0.875rem; }
    .table-wrapper { overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
    .data-table th { padding: var(--spacing-sm) var(--spacing-md); text-align: left; font-weight: 600; background: var(--color-surface-muted, #f9fafb); border-bottom: 2px solid var(--color-border); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.04em; }
    .data-table td { padding: var(--spacing-sm) var(--spacing-md); border-bottom: 1px solid var(--color-border); vertical-align: top; }
    .data-table tr:last-child td { border-bottom: none; }
    .data-table tr:hover td { background: var(--color-surface-hover); }
    .row-alert td { background: #fff7ed; }
    .row-alert:hover td { background: #ffedd5; }
    .score-cell { font-weight: 700; font-size: 1rem; }
    .threshold-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 0.75rem; padding: 2px 8px; border-radius: var(--radius-full); font-weight: 500; }
    .badge-alert { background: #fee2e2; color: #991b1b; }
    .badge-warning { background: #fef3c7; color: #92400e; }
    .empty-state { text-align: center; padding: var(--spacing-xl); color: var(--color-text-muted); }
    .state-msg { color: var(--color-text-muted); font-size: 0.875rem; }
    .alert-error { padding: var(--spacing-xs) var(--spacing-sm); background: #fee2e2; color: #991b1b; border-radius: var(--radius-sm); font-size: 0.875rem; }
  `]
})
export class OutcomeMeasureListComponent implements OnChanges {
  @Input() planId!: string;
  @Input() canManage = false;
  @Input() isActive = false;
  @Output() chartRequested = new EventEmitter<string>();

  entries: OutcomeMeasureEntry[] = [];
  loading = false;
  error: string | null = null;
  showDialog = false;

  get breachedCount(): number {
    return this.entries.filter(e => e.thresholdBreached).length;
  }

  constructor(private outcomeMeasureService: OutcomeMeasureService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['planId'] && this.planId) {
      this.loadEntries();
    }
  }

  onEntryRecorded(entry: OutcomeMeasureEntry): void {
    this.entries.unshift(entry);
  }

  private loadEntries(): void {
    this.loading = true;
    this.error = null;
    this.outcomeMeasureService.getEntries(this.planId).subscribe({
      next: (entries) => {
        this.entries = Array.isArray(entries)
          ? entries
          : (entries as { content?: OutcomeMeasureEntry[] }).content ?? [];
        this.loading = false;
      },
      error: () => {
        this.error = 'carePlans.outcomeMeasures.loadError';
        this.loading = false;
      }
    });
  }
}
