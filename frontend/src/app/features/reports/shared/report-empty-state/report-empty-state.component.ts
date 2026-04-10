import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-report-empty-state',
  standalone: true,
  imports: [CommonModule, TranslocoPipe],
  template: `
    <div class="report-empty-state">
      <i class="pi pi-inbox empty-icon"></i>
      <p class="empty-message">{{ 'reports.emptyState.message' | transloco }}</p>
    </div>
  `,
  styles: [`
    .report-empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--spacing-xl);
      color: var(--color-text-secondary);
      gap: var(--spacing-md);

      .empty-icon {
        font-size: 2.5rem;
        opacity: 0.4;
      }

      .empty-message {
        font-size: 0.9375rem;
        margin: 0;
        text-align: center;
      }
    }
  `],
})
export class ReportEmptyStateComponent {}
