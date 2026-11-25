import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import {
  CreateServiceRequest,
  ServiceCatalogItem,
  ServiceStatus,
} from '../../models/service-catalog.model';
import { ServiceCatalogService } from '../../services/service-catalog.service';
import { ServiceCatalogFormComponent } from '../service-catalog-form/service-catalog-form.component';

@Component({
  selector: 'app-service-catalog-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    TranslocoModule,
    ServiceCatalogFormComponent,
    Select,
    TableModule,
  ],
  template: `
    <div class="page-container">

      <!-- Header -->
      <div class="page-header">
        <h1 class="page-title">{{ 'billing.catalog.title' | transloco }}</h1>
        <button type="button" class="btn-primary" (click)="openCreateDialog()">
          + {{ 'billing.catalog.actions.addService' | transloco }}
        </button>
      </div>

      <!-- Filter bar -->
      <div class="filter-bar">
        <p-select
          [(ngModel)]="statusFilter"
          [options]="catalogStatusOptions"
          optionLabel="label"
          optionValue="value"
          (ngModelChange)="loadServices()"
          styleClass="w-full"
        />

        <div class="filter-stats" *ngIf="!loading && services.length > 0">
          <span class="badge badge-active badge-sm">
            {{ activeCount }} {{ 'billing.catalog.status.ACTIVE' | transloco }}
          </span>
          <span class="badge badge-inactive badge-sm">
            {{ inactiveCount }} {{ 'billing.catalog.status.INACTIVE' | transloco }}
          </span>
        </div>
      </div>

      <!-- Error -->
      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <!-- Table -->
      <p-table
        [value]="services"
        [loading]="loading"
        styleClass="p-datatable-sm"
        responsiveLayout="scroll"
      >
        <ng-template pTemplate="header">
          <tr>
            <th>{{ 'billing.catalog.fields.name' | transloco }}</th>
            <th>{{ 'billing.catalog.fields.serviceType' | transloco }}</th>
            <th class="col-right">{{ 'billing.catalog.fields.durationMin' | transloco }}</th>
            <th class="col-right">{{ 'billing.catalog.fields.currentPrice' | transloco }}</th>
            <th>{{ 'billing.catalog.fields.status' | transloco }}</th>
            <th>{{ 'billing.catalog.fields.actions' | transloco }}</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-svc>
          <tr>
            <td>
              <a [routerLink]="[svc.id]" class="link-primary">
                {{ svc.name }}
              </a>
            </td>
            <td>{{ 'sessions.types.' + svc.sessionType.code | transloco }}</td>
            <td class="col-right">{{ svc.durationMin }} {{ 'billing.catalog.fields.min' | transloco }}</td>
            <td class="col-right price-cell">{{ svc.currentPrice | number:'1.2-2' }}</td>
            <td>
              <span class="badge"
                    [class.badge-active]="svc.status === 'ACTIVE'"
                    [class.badge-inactive]="svc.status === 'INACTIVE'">
                {{ 'billing.catalog.status.' + svc.status | transloco }}
              </span>
            </td>
            <td>
              <div class="row-actions">
                <a [routerLink]="[svc.id]"
                   class="btn-ghost btn-sm"
                   [attr.aria-label]="'billing.catalog.actions.view' | transloco">
                  {{ 'billing.catalog.actions.view' | transloco }}
                </a>
                <button type="button" class="btn-ghost btn-sm"
                        (click)="openEditDialog(svc)"
                        [attr.aria-label]="'common.actions.edit' | transloco">
                  {{ 'common.actions.edit' | transloco }}
                </button>
                <button type="button"
                        class="btn-ghost btn-sm"
                        [class.text-danger]="svc.status === 'ACTIVE'"
                        (click)="toggleStatus(svc)"
                        [disabled]="togglingId === svc.id"
                        [attr.aria-label]="(svc.status === 'ACTIVE' ? 'billing.catalog.actions.deactivate' : 'billing.catalog.actions.activate') | transloco">
                  <ng-container *ngIf="togglingId !== svc.id">
                    {{ (svc.status === 'ACTIVE' ? 'billing.catalog.actions.deactivate' : 'billing.catalog.actions.activate') | transloco }}
                  </ng-container>
                  <ng-container *ngIf="togglingId === svc.id">
                    {{ 'common.status.loading' | transloco }}
                  </ng-container>
                </button>
              </div>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="7">
              <div class="empty-state">
                <p>{{ 'billing.catalog.list.empty' | transloco }}</p>
                <button type="button" class="btn-primary" (click)="openCreateDialog()">
                  + {{ 'billing.catalog.actions.addService' | transloco }}
                </button>
              </div>
            </td>
          </tr>
        </ng-template>
      </p-table>

    </div>

    <!-- Create / Edit dialog -->
    <app-service-catalog-form
      *ngIf="showForm"
      [editMode]="formMode === 'edit'"
      [service]="selectedService"
      [saving]="formSaving"
      [serverError]="formError"
      (submitted)="onFormSubmit($event)"
      (cancelled)="closeForm()"
    />
  `,
  styles: [`
    .filter-bar {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      margin-bottom: var(--spacing-lg);
      flex-wrap: wrap;
    }

    .filter-stats {
      display: flex;
      gap: var(--spacing-sm);
    }

    .row-actions {
      display: flex;
      gap: var(--spacing-xs, 0.25rem);
      flex-wrap: wrap;
      align-items: center;
    }

    .price-cell {
      font-variant-numeric: tabular-nums;
      font-weight: 500;
    }

    .text-danger {
      color: var(--color-error);
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--spacing-lg);
      padding: var(--spacing-xl) 0;
      color: var(--color-text-secondary);
      text-align: center;
    }
  `],
})
export class ServiceCatalogListComponent implements OnInit {
  services: ServiceCatalogItem[] = [];
  loading = false;
  loadError: string | null = null;

  statusFilter: ServiceStatus | '' = '';
  catalogStatusOptions: { label: string; value: string }[] = [];

  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  selectedService: ServiceCatalogItem | null = null;
  formSaving = false;
  formError: string | null = null;

  togglingId: string | null = null;

  get activeCount(): number {
    return this.services.filter(s => s.status === 'ACTIVE').length;
  }

  get inactiveCount(): number {
    return this.services.filter(s => s.status === 'INACTIVE').length;
  }

  constructor(
    private catalogService: ServiceCatalogService,
    private router: Router,
    private route: ActivatedRoute,
    private transloco: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.catalogStatusOptions = [
      { label: this.transloco.translate('billing.catalog.filters.allStatuses'), value: '' },
      { label: this.transloco.translate('billing.catalog.status.ACTIVE'), value: 'ACTIVE' },
      { label: this.transloco.translate('billing.catalog.status.INACTIVE'), value: 'INACTIVE' },
    ];
    this.loadServices();
  }

  loadServices(): void {
    this.loading = true;
    this.loadError = null;
    const status = this.statusFilter || undefined;
    this.catalogService.list(status as ServiceStatus | undefined).subscribe({
      next: (data) => {
        this.services = data;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'billing.catalog.list.loadError';
        this.loading = false;
      },
    });
  }

  openCreateDialog(): void {
    this.formMode = 'create';
    this.selectedService = null;
    this.formError = null;
    this.showForm = true;
  }

  openEditDialog(svc: ServiceCatalogItem): void {
    this.formMode = 'edit';
    this.selectedService = svc;
    this.formError = null;
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.selectedService = null;
    this.formError = null;
  }

  onFormSubmit(payload: CreateServiceRequest | object): void {
    this.formSaving = true;
    this.formError = null;

    const obs = this.formMode === 'create'
      ? this.catalogService.create(payload as CreateServiceRequest)
      : this.catalogService.update(this.selectedService!.id, payload as never);

    obs.subscribe({
      next: (saved) => {
        this.formSaving = false;
        this.closeForm();
        if (this.formMode === 'create') {
          this.router.navigate([saved.id], { relativeTo: this.route });
        } else {
          this.loadServices();
        }
      },
      error: (err) => {
        this.formSaving = false;
        this.formError = err?.error?.message ?? 'billing.catalog.error.saveFailed';
      },
    });
  }

  toggleStatus(svc: ServiceCatalogItem): void {
    this.togglingId = svc.id;
    const newStatus = svc.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.catalogService.updateStatus(svc.id, { status: newStatus }).subscribe({
      next: (updated) => {
        const idx = this.services.findIndex(s => s.id === updated.id);
        if (idx !== -1) {
          this.services[idx] = updated;
        }
        this.togglingId = null;
      },
      error: () => {
        this.togglingId = null;
      },
    });
  }
}
