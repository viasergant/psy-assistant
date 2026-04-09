import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import {
  PriceHistoryEntry,
  ServiceCatalogItem,
  TherapistOverride,
  UpdateDefaultPriceRequest,
  UpdateServiceRequest,
} from '../../models/service-catalog.model';
import { ServiceCatalogService } from '../../services/service-catalog.service';
import { ServiceCatalogFormComponent } from '../service-catalog-form/service-catalog-form.component';
import { UpdatePriceDialogComponent } from '../update-price-dialog/update-price-dialog.component';
import { TherapistOverrideDialogComponent, OverrideSubmission } from '../therapist-override-dialog/therapist-override-dialog.component';

@Component({
  selector: 'app-service-catalog-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslocoModule,
    ServiceCatalogFormComponent,
    UpdatePriceDialogComponent,
    TherapistOverrideDialogComponent,
  ],
  template: `
    <div class="page-container">

      <!-- Back -->
      <a [routerLink]="['..']" class="back-link">
        ← {{ 'billing.catalog.actions.backToList' | transloco }}
      </a>

      <!-- Loading -->
      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'billing.catalog.detail.loading' | transloco }}
      </div>
      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <ng-container *ngIf="service && !loading">

        <!-- Service header card -->
        <div class="detail-header">
          <div class="detail-title-row">
            <h1 class="page-title">{{ service.name }}</h1>
            <span class="badge"
                  [class.badge-active]="service.status === 'ACTIVE'"
                  [class.badge-inactive]="service.status === 'INACTIVE'">
              {{ 'billing.catalog.status.' + service.status | transloco }}
            </span>
          </div>

          <dl class="detail-meta-grid">
            <div class="meta-item">
              <dt>{{ 'billing.catalog.fields.category' | transloco }}</dt>
              <dd>{{ service.category }}</dd>
            </div>
            <div class="meta-item">
              <dt>{{ 'billing.catalog.fields.serviceType' | transloco }}</dt>
              <dd>{{ 'sessions.types.' + service.sessionType.code | transloco }}</dd>
            </div>
            <div class="meta-item">
              <dt>{{ 'billing.catalog.fields.durationMin' | transloco }}</dt>
              <dd>{{ service.durationMin }} {{ 'billing.catalog.fields.min' | transloco }}</dd>
            </div>
            <div class="meta-item">
              <dt>{{ 'billing.catalog.fields.currentPrice' | transloco }}</dt>
              <dd class="price-highlight">{{ service.currentPrice | number:'1.2-2' }}</dd>
            </div>
          </dl>
        </div>

        <!-- Action bar -->
        <div class="action-bar">
          <button type="button" class="btn-secondary" (click)="openEditDialog()">
            {{ 'common.actions.edit' | transloco }}
          </button>
          <button type="button" class="btn-secondary" (click)="openPriceDialog()">
            {{ 'billing.catalog.actions.updatePrice' | transloco }}
          </button>
          <button type="button"
                  [class.btn-danger]="service.status === 'ACTIVE'"
                  [class.btn-secondary]="service.status === 'INACTIVE'"
                  [disabled]="togglingStatus"
                  (click)="toggleStatus()">
            <ng-container *ngIf="!togglingStatus">
              {{ (service.status === 'ACTIVE' ? 'billing.catalog.actions.deactivate' : 'billing.catalog.actions.activate') | transloco }}
            </ng-container>
            <ng-container *ngIf="togglingStatus">
              {{ 'common.status.loading' | transloco }}
            </ng-container>
          </button>
        </div>

        <div *ngIf="actionError" class="alert-error" role="alert">{{ actionError }}</div>

        <!-- Price History section -->
        <section class="detail-section" aria-labelledby="price-history-heading">
          <div class="section-header">
            <h2 id="price-history-heading" class="section-title">
              {{ 'billing.catalog.priceHistory.title' | transloco }}
            </h2>
          </div>

          <div *ngIf="loadingHistory" class="state-msg" aria-live="polite">
            {{ 'common.status.loading' | transloco }}
          </div>

          <div *ngIf="!loadingHistory && priceHistory.length === 0" class="empty-hint">
            {{ 'billing.catalog.priceHistory.empty' | transloco }}
          </div>

          <div *ngIf="!loadingHistory && priceHistory.length > 0" class="table-wrapper">
            <table class="data-table table-compact"
                   [attr.aria-label]="'billing.catalog.priceHistory.title' | transloco">
              <thead>
                <tr>
                  <th scope="col" class="col-right">{{ 'billing.catalog.priceHistory.price' | transloco }}</th>
                  <th scope="col">{{ 'billing.catalog.priceHistory.effectiveFrom' | transloco }}</th>
                  <th scope="col">{{ 'billing.catalog.priceHistory.effectiveTo' | transloco }}</th>
                  <th scope="col">{{ 'billing.catalog.priceHistory.changedBy' | transloco }}</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let entry of priceHistory">
                  <td class="col-right price-cell">{{ entry.price | number:'1.2-2' }}</td>
                  <td>{{ entry.effectiveFrom | date:'mediumDate' }}</td>
                  <td>
                    <span *ngIf="entry.effectiveTo">{{ entry.effectiveTo | date:'mediumDate' }}</span>
                    <span *ngIf="!entry.effectiveTo" class="badge badge-accent badge-sm">
                      {{ 'billing.catalog.priceHistory.current' | transloco }}
                    </span>
                  </td>
                  <td>{{ entry.changedBy }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- Therapist Overrides section -->
        <section class="detail-section" aria-labelledby="overrides-heading">
          <div class="section-header">
            <h2 id="overrides-heading" class="section-title">
              {{ 'billing.catalog.override.sectionTitle' | transloco }}
            </h2>
            <button type="button" class="btn-secondary btn-sm" (click)="openAddOverrideDialog()">
              + {{ 'billing.catalog.override.add' | transloco }}
            </button>
          </div>

          <p class="section-hint">{{ 'billing.catalog.override.hint' | transloco }}</p>

          <div *ngIf="loadingOverrides" class="state-msg" aria-live="polite">
            {{ 'common.status.loading' | transloco }}
          </div>

          <div *ngIf="!loadingOverrides && overrides.length === 0" class="empty-hint">
            {{ 'billing.catalog.override.empty' | transloco }}
          </div>

          <div *ngIf="!loadingOverrides && overrides.length > 0" class="table-wrapper">
            <table class="data-table table-compact"
                   [attr.aria-label]="'billing.catalog.override.sectionTitle' | transloco">
              <thead>
                <tr>
                  <th scope="col">{{ 'billing.catalog.override.therapist' | transloco }}</th>
                  <th scope="col" class="col-right">{{ 'billing.catalog.override.price' | transloco }}</th>
                  <th scope="col">{{ 'billing.catalog.fields.actions' | transloco }}</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let ov of overrides">
                  <td>{{ ov.therapistName }}</td>
                  <td class="col-right price-cell">{{ ov.price | number:'1.2-2' }}</td>
                  <td>
                    <div class="row-actions">
                      <button type="button" class="btn-ghost btn-sm"
                              (click)="openEditOverrideDialog(ov)"
                              [attr.aria-label]="'common.actions.edit' | transloco">
                        {{ 'common.actions.edit' | transloco }}
                      </button>
                      <button type="button" class="btn-ghost btn-sm text-danger"
                              [disabled]="removingOverrideId === ov.therapistId"
                              (click)="removeOverride(ov)"
                              [attr.aria-label]="'billing.catalog.override.remove' | transloco">
                        <ng-container *ngIf="removingOverrideId !== ov.therapistId">
                          {{ 'billing.catalog.override.remove' | transloco }}
                        </ng-container>
                        <ng-container *ngIf="removingOverrideId === ov.therapistId">
                          {{ 'common.status.loading' | transloco }}
                        </ng-container>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

      </ng-container>
    </div>

    <!-- Edit service dialog -->
    <app-service-catalog-form
      *ngIf="showEditDialog"
      [editMode]="true"
      [service]="service"
      [saving]="editSaving"
      [serverError]="editError"
      (submitted)="onEditSubmit($event)"
      (cancelled)="closeEditDialog()"
    />

    <!-- Update price dialog -->
    <app-update-price-dialog
      *ngIf="showPriceDialog"
      [currentPrice]="service?.currentPrice ?? null"
      [saving]="priceSaving"
      [serverError]="priceError"
      (submitted)="onPriceSubmit($event)"
      (cancelled)="closePriceDialog()"
    />

    <!-- Override dialog (add / edit) -->
    <app-therapist-override-dialog
      *ngIf="showOverrideDialog"
      [existingOverrides]="overrides"
      [editOverride]="editingOverride"
      [saving]="overrideSaving"
      [serverError]="overrideError"
      (submitted)="onOverrideSubmit($event)"
      (cancelled)="closeOverrideDialog()"
    />
  `,
  styles: [`
    .detail-header {
      background: var(--color-surface);
      border: 1.5px solid var(--color-border);
      border-radius: var(--radius-lg);
      padding: var(--spacing-xl);
      margin-bottom: var(--spacing-xl);
      box-shadow: var(--shadow-sm);
    }

    .detail-title-row {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      margin-bottom: var(--spacing-md);
      flex-wrap: wrap;
    }

    .detail-title-row .page-title {
      margin: 0;
    }

    .detail-meta-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: var(--spacing-md);
      margin: 0;
    }

    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .meta-item dt {
      font-size: 0.8125rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      color: var(--color-text-secondary);
    }

    .meta-item dd {
      margin: 0;
      font-size: 0.9375rem;
      color: var(--color-text-primary);
    }

    .price-highlight {
      font-size: 1.125rem !important;
      font-weight: 600;
      color: var(--color-accent) !important;
      font-variant-numeric: tabular-nums;
    }

    .action-bar {
      display: flex;
      gap: var(--spacing-md);
      margin-bottom: var(--spacing-xl);
      flex-wrap: wrap;
    }

    .detail-section {
      margin-top: var(--spacing-xl);
    }

    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: var(--spacing-md);
      flex-wrap: wrap;
      gap: var(--spacing-sm);
    }

    .section-title {
      font-size: 1rem;
      font-weight: 600;
      color: var(--color-text-primary);
      margin: 0;
    }

    .section-hint {
      font-size: 0.875rem;
      color: var(--color-text-secondary);
      margin: 0 0 var(--spacing-md);
    }

    .empty-hint {
      color: var(--color-text-secondary);
      font-size: 0.9375rem;
      padding: var(--spacing-lg) 0;
    }

    .price-cell {
      font-variant-numeric: tabular-nums;
      font-weight: 500;
    }

    .row-actions {
      display: flex;
      gap: var(--spacing-xs, 0.25rem);
      align-items: center;
    }

    .text-danger {
      color: var(--color-error);
    }
  `],
})
export class ServiceCatalogDetailComponent implements OnInit {
  service: ServiceCatalogItem | null = null;
  loading = false;
  loadError: string | null = null;

  priceHistory: PriceHistoryEntry[] = [];
  loadingHistory = false;

  overrides: TherapistOverride[] = [];
  loadingOverrides = false;

  actionError: string | null = null;
  togglingStatus = false;

  // Edit service dialog
  showEditDialog = false;
  editSaving = false;
  editError: string | null = null;

  // Update price dialog
  showPriceDialog = false;
  priceSaving = false;
  priceError: string | null = null;

  // Override dialog
  showOverrideDialog = false;
  editingOverride: TherapistOverride | null = null;
  overrideSaving = false;
  overrideError: string | null = null;

  removingOverrideId: string | null = null;

  private serviceId!: string;

  constructor(
    private route: ActivatedRoute,
    private catalogService: ServiceCatalogService,
  ) {}

  ngOnInit(): void {
    this.serviceId = this.route.snapshot.paramMap.get('id')!;
    this.loadDetail();
  }

  private loadDetail(): void {
    this.loading = true;
    this.loadError = null;
    this.catalogService.getById(this.serviceId).subscribe({
      next: (svc) => {
        this.service = svc;
        this.loading = false;
        this.loadPriceHistory();
        this.loadOverrides();
      },
      error: () => {
        this.loadError = 'billing.catalog.detail.loadError';
        this.loading = false;
      },
    });
  }

  private loadPriceHistory(): void {
    this.loadingHistory = true;
    this.catalogService.getPriceHistory(this.serviceId).subscribe({
      next: (history) => {
        this.priceHistory = history;
        this.loadingHistory = false;
      },
      error: () => {
        this.loadingHistory = false;
      },
    });
  }

  private loadOverrides(): void {
    this.loadingOverrides = true;
    this.catalogService.getOverrides(this.serviceId).subscribe({
      next: (data) => {
        this.overrides = data;
        this.loadingOverrides = false;
      },
      error: () => {
        this.loadingOverrides = false;
      },
    });
  }

  toggleStatus(): void {
    if (!this.service) { return; }
    this.togglingStatus = true;
    this.actionError = null;
    const newStatus = this.service.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.catalogService.updateStatus(this.serviceId, { status: newStatus }).subscribe({
      next: (updated) => {
        this.service = updated;
        this.togglingStatus = false;
      },
      error: () => {
        this.actionError = 'billing.catalog.error.statusFailed';
        this.togglingStatus = false;
      },
    });
  }

  // --- Edit service ---
  openEditDialog(): void {
    this.editError = null;
    this.showEditDialog = true;
  }

  closeEditDialog(): void {
    this.showEditDialog = false;
    this.editError = null;
  }

  onEditSubmit(payload: UpdateServiceRequest | object): void {
    this.editSaving = true;
    this.editError = null;
    this.catalogService.update(this.serviceId, payload as UpdateServiceRequest).subscribe({
      next: (updated) => {
        this.service = updated;
        this.editSaving = false;
        this.closeEditDialog();
      },
      error: (err) => {
        this.editSaving = false;
        this.editError = err?.error?.message ?? 'billing.catalog.error.saveFailed';
      },
    });
  }

  // --- Update price ---
  openPriceDialog(): void {
    this.priceError = null;
    this.showPriceDialog = true;
  }

  closePriceDialog(): void {
    this.showPriceDialog = false;
    this.priceError = null;
  }

  onPriceSubmit(payload: UpdateDefaultPriceRequest): void {
    this.priceSaving = true;
    this.priceError = null;
    this.catalogService.updatePrice(this.serviceId, payload).subscribe({
      next: (updated) => {
        this.service = updated;
        this.priceSaving = false;
        this.closePriceDialog();
        this.loadPriceHistory();
      },
      error: (err) => {
        this.priceSaving = false;
        this.priceError = err?.error?.message ?? 'billing.catalog.error.priceFailed';
      },
    });
  }

  // --- Overrides ---
  openAddOverrideDialog(): void {
    this.editingOverride = null;
    this.overrideError = null;
    this.showOverrideDialog = true;
  }

  openEditOverrideDialog(ov: TherapistOverride): void {
    this.editingOverride = ov;
    this.overrideError = null;
    this.showOverrideDialog = true;
  }

  closeOverrideDialog(): void {
    this.showOverrideDialog = false;
    this.editingOverride = null;
    this.overrideError = null;
  }

  onOverrideSubmit(submission: OverrideSubmission): void {
    this.overrideSaving = true;
    this.overrideError = null;
    this.catalogService.upsertOverride(this.serviceId, submission.therapistId, submission.request).subscribe({
      next: () => {
        this.overrideSaving = false;
        this.closeOverrideDialog();
        this.loadOverrides();
      },
      error: (err) => {
        this.overrideSaving = false;
        this.overrideError = err?.error?.message ?? 'billing.catalog.error.overrideFailed';
      },
    });
  }

  removeOverride(ov: TherapistOverride): void {
    this.removingOverrideId = ov.therapistId;
    this.catalogService.removeOverride(this.serviceId, ov.therapistId).subscribe({
      next: () => {
        this.overrides = this.overrides.filter(o => o.therapistId !== ov.therapistId);
        this.removingOverrideId = null;
      },
      error: () => {
        this.removingOverrideId = null;
      },
    });
  }
}
