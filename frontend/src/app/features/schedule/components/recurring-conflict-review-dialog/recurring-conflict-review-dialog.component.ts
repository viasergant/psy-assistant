import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import {
  ConflictResolution,
  RecurringConflictCheckResponse
} from '../../models/schedule.model';

/**
 * Dialog that presents all generated recurring slots with conflict indicators.
 *
 * <p>Staff can review each slot, then choose to skip conflicting slots
 * (save the rest) or abort the entire operation.
 *
 * Accessibility: all interactive controls have ARIA labels; the table
 * has a caption for screen readers.
 */
@Component({
  selector: 'app-recurring-conflict-review-dialog',
  standalone: true,
  imports: [CommonModule, TranslocoPipe],
  template: `
    <div
      class="dialog-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="conflict-review-title"
    >
      <div class="dialog">
        <div class="dialog-header">
          <h2 id="conflict-review-title">
            {{ 'schedule.recurringConflict.title' | transloco }}
          </h2>
          <button
            type="button"
            class="close-btn"
            (click)="abort()"
            aria-label="Close conflict review dialog"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 5L5 15M5 5l10 10" />
            </svg>
          </button>
        </div>

        <div class="dialog-body">
          <!-- Summary banner -->
          <div class="summary-banner" [class.has-conflicts]="response.conflictCount > 0">
            <div class="summary-stat">
              <span class="stat-number clean">{{ response.cleanSlotCount }}</span>
              <span class="stat-label">
                {{ 'schedule.recurringConflict.cleanSlots' | transloco }}
              </span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-stat">
              <span class="stat-number conflict">{{ response.conflictCount }}</span>
              <span class="stat-label">
                {{ 'schedule.recurringConflict.conflictingSlots' | transloco }}
              </span>
            </div>
          </div>

          <!-- Slot table -->
          <div class="table-wrapper" role="region" aria-label="Generated appointment slots">
            <table aria-describedby="conflict-review-title">
              <caption class="sr-only">
                Recurring appointment slots with conflict status
              </caption>
              <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">{{ 'schedule.recurringConflict.columnDate' | transloco }}</th>
                  <th scope="col">{{ 'schedule.recurringConflict.columnStatus' | transloco }}</th>
                  <th scope="col">{{ 'schedule.recurringConflict.columnConflict' | transloco }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  *ngFor="let slot of response.generatedSlots"
                  [class.row-conflict]="slot.hasConflict"
                >
                  <td>{{ slot.index + 1 }}</td>
                  <td>{{ formatSlotTime(slot.startTime) }}</td>
                  <td>
                    <span
                      class="status-chip"
                      [class.chip-clean]="!slot.hasConflict"
                      [class.chip-conflict]="slot.hasConflict"
                      [attr.aria-label]="slot.hasConflict ? 'Has conflict' : 'Available'"
                    >
                      {{ slot.hasConflict
                          ? ('schedule.recurringConflict.statusConflict' | transloco)
                          : ('schedule.recurringConflict.statusClean' | transloco) }}
                    </span>
                  </td>
                  <td class="conflict-detail">
                    <span *ngIf="slot.hasConflict && slot.conflictDetails">
                      {{ slot.conflictDetails.clientName || ('schedule.recurringConflict.unknownClient' | transloco) }}
                      — {{ formatSlotTime(slot.conflictDetails.startTime) }}
                    </span>
                    <span *ngIf="!slot.hasConflict" class="text-muted">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Info message if no conflicts -->
          <div *ngIf="response.conflictCount === 0" class="no-conflict-msg" role="status">
            {{ 'schedule.recurringConflict.noConflicts' | transloco }}
          </div>
        </div>

        <!-- Actions -->
        <div class="dialog-footer">
          <button
            type="button"
            class="btn-secondary"
            (click)="abort()"
            aria-label="Abort series creation"
          >
            {{ 'common.actions.cancel' | transloco }}
          </button>

          <button
            *ngIf="response.conflictCount > 0"
            type="button"
            class="btn-warning"
            (click)="skipConflicts()"
            aria-label="Skip conflicting slots and save the rest"
          >
            {{ 'schedule.recurringConflict.skipConflictsButton' | transloco }}
            ({{ response.cleanSlotCount }})
          </button>

          <button
            type="button"
            class="btn-primary"
            (click)="confirmAll()"
            [disabled]="response.conflictCount > 0"
            [attr.aria-disabled]="response.conflictCount > 0"
            aria-label="Confirm and create all occurrences"
          >
            {{ response.conflictCount > 0
                ? ('schedule.recurringConflict.conflictsBlockConfirm' | transloco)
                : ('schedule.recurringConflict.confirmButton' | transloco) }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes fadeInUp {
      from { opacity: 0; transform: translateY(20px) scale(0.96); }
      to   { opacity: 1; transform: translateY(0) scale(1); }
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }

    .dialog-overlay {
      position: fixed;
      inset: 0;
      background: rgba(20, 30, 43, 0.65);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1100;
      animation: fadeIn 0.2s ease;
    }

    .dialog {
      background: var(--color-surface, #FFFFFF);
      border-radius: 12px;
      width: 720px;
      max-width: 95vw;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 24px 48px rgba(0, 0, 0, 0.18), 0 8px 16px rgba(0, 0, 0, 0.12);
      animation: fadeInUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      overflow: hidden;
    }

    .dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.75rem 2rem;
      border-bottom: 1px solid var(--color-border, #E2E8F0);
      background: linear-gradient(to bottom, #FFFFFF, #FAFBFC);
      flex-shrink: 0;
    }

    h2 {
      margin: 0;
      font-size: 1.375rem;
      font-weight: 700;
      color: var(--color-text-primary, #0F172A);
      letter-spacing: -0.01em;
    }

    .close-btn {
      padding: 0.5rem;
      border: none;
      background: transparent;
      color: var(--color-text-secondary, #64748B);
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
    }

    .close-btn:hover {
      background: var(--color-border, #E2E8F0);
      color: var(--color-text-primary, #0F172A);
    }

    .dialog-body {
      padding: 1.5rem 2rem;
      overflow-y: auto;
      flex: 1;
    }

    .summary-banner {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 2rem;
      padding: 1.25rem;
      background: #F0FDF4;
      border: 1.5px solid #86EFAC;
      border-radius: 10px;
      margin-bottom: 1.5rem;
    }

    .summary-banner.has-conflicts {
      background: #FFF7ED;
      border-color: #FDBA74;
    }

    .summary-divider {
      width: 1px;
      height: 40px;
      background: #D1D5DB;
    }

    .summary-stat {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
    }

    .stat-number {
      font-size: 2rem;
      font-weight: 800;
      line-height: 1;
    }

    .stat-number.clean { color: #16A34A; }
    .stat-number.conflict { color: #EA580C; }

    .stat-label {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--color-text-secondary, #64748B);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .table-wrapper {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9375rem;
    }

    th {
      text-align: left;
      padding: 0.75rem 1rem;
      font-size: 0.8125rem;
      font-weight: 700;
      color: var(--color-text-secondary, #64748B);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      border-bottom: 2px solid var(--color-border, #E2E8F0);
      background: #F8FAFC;
    }

    td {
      padding: 0.875rem 1rem;
      border-bottom: 1px solid #F1F5F9;
      color: var(--color-text-primary, #0F172A);
      vertical-align: middle;
    }

    .row-conflict td {
      background: #FFF7ED;
    }

    .status-chip {
      display: inline-flex;
      align-items: center;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.8125rem;
      font-weight: 600;
    }

    .chip-clean {
      background: #DCFCE7;
      color: #15803D;
    }

    .chip-conflict {
      background: #FEE2E2;
      color: #B91C1C;
    }

    .conflict-detail {
      font-size: 0.875rem;
      color: #92400E;
    }

    .text-muted {
      color: var(--color-text-secondary, #64748B);
    }

    .no-conflict-msg {
      padding: 1rem 1.25rem;
      background: #F0FDF4;
      border: 1.5px solid #86EFAC;
      border-radius: 8px;
      font-size: 0.9375rem;
      color: #15803D;
      font-weight: 500;
      text-align: center;
    }

    .dialog-footer {
      display: flex;
      gap: 0.875rem;
      justify-content: flex-end;
      padding: 1.25rem 2rem;
      border-top: 1px solid var(--color-border, #E2E8F0);
      background: #FAFBFC;
      flex-shrink: 0;
    }

    .btn-secondary,
    .btn-warning,
    .btn-primary {
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      font-size: 0.9375rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      border: none;
    }

    .btn-secondary {
      background: #F1F5F9;
      color: #374151;
      border: 1.5px solid var(--color-border, #E2E8F0);
    }

    .btn-secondary:hover {
      background: #E2E8F0;
    }

    .btn-warning {
      background: #F97316;
      color: #FFFFFF;
    }

    .btn-warning:hover {
      background: #EA580C;
      box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
    }

    .btn-primary {
      background: var(--color-accent, #0EA5A0);
      color: #FFFFFF;
    }

    .btn-primary:hover:not(:disabled) {
      background: var(--color-accent-hover, #0C9490);
      box-shadow: 0 4px 12px rgba(14, 165, 160, 0.28);
    }

    .btn-primary:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  `]
})
export class RecurringConflictReviewDialogComponent implements OnInit {
  @Input() response!: RecurringConflictCheckResponse;
  @Output() resolved = new EventEmitter<ConflictResolution>();
  @Output() cancelled = new EventEmitter<void>();

  ngOnInit(): void {}

  formatSlotTime(isoString: string): string {
    return new Date(isoString).toLocaleString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  skipConflicts(): void {
    this.resolved.emit('SKIP_CONFLICTS');
  }

  confirmAll(): void {
    this.resolved.emit('ABORT'); // Only reachable when conflictCount === 0, so ABORT == proceed
  }

  abort(): void {
    this.cancelled.emit();
  }
}
