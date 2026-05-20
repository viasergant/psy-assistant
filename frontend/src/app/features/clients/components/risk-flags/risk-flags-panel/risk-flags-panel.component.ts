import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { RiskFlag } from '../models/risk-flag.model';
import { RiskFlagService } from '../services/risk-flag.service';
import { RiskFlagFormDialogComponent } from '../risk-flag-form-dialog/risk-flag-form-dialog.component';
import { RiskFlagResolveDialogComponent } from '../risk-flag-resolve-dialog/risk-flag-resolve-dialog.component';

@Component({
  selector: 'app-risk-flags-panel',
  standalone: true,
  imports: [CommonModule, TranslocoModule, RiskFlagFormDialogComponent, RiskFlagResolveDialogComponent],
  template: `
    <div class="risk-flags-panel">
      <div class="panel-header">
        <h3 class="panel-title">Risk Flags</h3>
        <button *ngIf="canManage"
                type="button"
                class="btn-primary"
                (click)="openAddDialog()">
          + Add Flag
        </button>
      </div>

      <div *ngIf="loading" class="state-msg" aria-live="polite">Loading risk flags…</div>

      <div *ngIf="error" class="alert-error" role="alert">{{ error }}</div>

      <div *ngIf="!loading && !error && flags.length === 0" class="empty-state">
        No active risk flags.
      </div>

      <div *ngIf="!loading && flags.length > 0" class="flags-list">
        <div *ngFor="let flag of flags" class="flag-card">
          <div class="flag-card-header">
            <span class="flag-type-chip">{{ flag.flagTypeName }}</span>
            <span class="flag-review-date">Review: {{ flag.reviewDate }}</span>
          </div>

          <div *ngIf="canReadNotes && flag.clinicalNote != null" class="clinical-note-block">
            <span class="clinical-note-label">Clinical Note</span>
            <p class="clinical-note-text">{{ flag.clinicalNote }}</p>
          </div>

          <div class="flag-card-footer">
            <button *ngIf="canManage && flag.status === 'ACTIVE'"
                    type="button"
                    class="btn-resolve"
                    (click)="openResolveDialog(flag)">
              Resolve
            </button>
          </div>
        </div>
      </div>

      <app-risk-flag-form-dialog
        *ngIf="showAddDialog"
        [clientId]="clientId"
        (saved)="onFlagAdded()"
        (cancelled)="showAddDialog = false">
      </app-risk-flag-form-dialog>

      <app-risk-flag-resolve-dialog
        *ngIf="showResolveDialog && resolvingFlagId"
        [clientId]="clientId"
        [flagId]="resolvingFlagId"
        (resolved)="onFlagResolved()"
        (cancelled)="closeResolveDialog()">
      </app-risk-flag-resolve-dialog>
    </div>
  `,
  styles: [`
    .risk-flags-panel { margin-top: 2rem; }
    .panel-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 1rem;
    }
    .panel-title { margin: 0; font-size: 1rem; font-weight: 600; color: #374151; }
    .flags-list { display: flex; flex-direction: column; gap: .75rem; }
    .flag-card {
      border: 1px solid #FECACA; border-radius: 8px;
      padding: 1rem; background: #FFF5F5;
    }
    .flag-card-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: .5rem;
    }
    .flag-type-chip {
      display: inline-block;
      background: #FEE2E2; color: #991B1B;
      border-radius: 4px; padding: 2px 8px;
      font-size: .75rem; font-weight: 600;
    }
    .flag-review-date { font-size: .8rem; color: #64748B; }
    .clinical-note-block {
      background: #FEF2F2; border-radius: 6px;
      padding: .6rem .75rem; margin-bottom: .5rem;
    }
    .clinical-note-label {
      display: block; font-size: .75rem; font-weight: 600;
      color: #991B1B; margin-bottom: .25rem;
    }
    .clinical-note-text {
      margin: 0; font-size: .875rem; color: #374151; white-space: pre-wrap;
    }
    .flag-card-footer { display: flex; justify-content: flex-end; }
    .btn-resolve {
      border: 1px solid #991B1B; background: transparent; color: #991B1B;
      border-radius: 6px; padding: .35rem .75rem; font-size: .8rem;
      font-weight: 600; cursor: pointer;
    }
    .btn-resolve:hover { background: #FEE2E2; }
    .btn-primary {
      border-radius: 8px; padding: .45rem .85rem; border: 1px solid transparent;
      font-weight: 600; cursor: pointer; background: #0EA5A0; color: #fff;
      font-size: .875rem;
    }
    .btn-primary:hover { background: #0C9490; }
    .empty-state { color: #64748B; padding: 1rem 0; text-align: center; font-size: .9rem; }
    .state-msg { color: #64748B; padding: .75rem 0; }
    .alert-error {
      padding: .65rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; font-size: .875rem;
    }
  `]
})
export class RiskFlagsPanelComponent implements OnInit {
  @Input({ required: true }) clientId!: string;
  /** True when the current user has MANAGE_RISK_FLAGS permission. */
  @Input() canManage = false;
  /** True when the current user has READ_RISK_FLAG_NOTES permission. */
  @Input() canReadNotes = false;

  flags: RiskFlag[] = [];
  loading = false;
  error: string | null = null;
  showAddDialog = false;
  showResolveDialog = false;
  resolvingFlagId: string | null = null;

  constructor(private riskFlagService: RiskFlagService) {}

  ngOnInit(): void {
    this.loadFlags();
  }

  openAddDialog(): void {
    this.showAddDialog = true;
  }

  openResolveDialog(flag: RiskFlag): void {
    this.resolvingFlagId = flag.id;
    this.showResolveDialog = true;
  }

  closeResolveDialog(): void {
    this.showResolveDialog = false;
    this.resolvingFlagId = null;
  }

  onFlagAdded(): void {
    this.showAddDialog = false;
    this.loadFlags();
  }

  onFlagResolved(): void {
    this.closeResolveDialog();
    this.loadFlags();
  }

  private loadFlags(): void {
    this.loading = true;
    this.error = null;

    const source$ = this.canReadNotes
      ? this.riskFlagService.listAll(this.clientId)
      : this.riskFlagService.listActive(this.clientId);

    source$.subscribe({
      next: (flags) => {
        // When showing all (supervisor), display only active flags in this panel
        // to keep the view focused; resolved flags are available via listAll
        this.flags = this.canReadNotes
          ? flags.filter(f => f.status === 'ACTIVE')
          : flags;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load risk flags. Please try again.';
        this.loading = false;
      }
    });
  }
}
