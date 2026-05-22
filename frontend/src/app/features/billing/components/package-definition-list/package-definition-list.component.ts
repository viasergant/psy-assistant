import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { CreatePackageDefinitionRequest, PackageDefinition, PackageDefinitionStatus } from '../../models/package.model';
import { PackageService } from '../../services/package.service';
import { PackageDefinitionFormComponent } from '../package-definition-form/package-definition-form.component';

@Component({
  selector: 'app-package-definition-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule, PackageDefinitionFormComponent, Select, TableModule],
  template: `
    <div class="page-container">

      <div class="page-header">
        <h1 class="page-title">{{ 'billing.packages.title' | transloco }}</h1>
        <button type="button" class="btn-primary" (click)="openCreate()">
          + {{ 'billing.packages.actions.create' | transloco }}
        </button>
      </div>

      <div class="filter-bar">
        <p-select
          [(ngModel)]="statusFilter"
          [options]="pkgStatusOptions"
          optionLabel="label"
          optionValue="value"
          (ngModelChange)="loadDefinitions()"
          styleClass="w-full"
        />
      </div>

      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <p-table
        [value]="definitions"
        [loading]="loading"
        styleClass="p-datatable-sm"
        responsiveLayout="scroll"
      >
        <ng-template pTemplate="header">
          <tr>
            <th>{{ 'billing.packages.fields.name' | transloco }}</th>
            <th>{{ 'billing.packages.fields.serviceType' | transloco }}</th>
            <th class="col-right">{{ 'billing.packages.fields.sessionQty' | transloco }}</th>
            <th class="col-right">{{ 'billing.packages.fields.price' | transloco }}</th>
            <th class="col-right">{{ 'billing.packages.fields.perSession' | transloco }}</th>
            <th>{{ 'billing.packages.fields.status' | transloco }}</th>
            <th>{{ 'billing.catalog.fields.actions' | transloco }}</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-pkg>
          <tr>
            <td class="fw-medium">{{ pkg.name }}</td>
            <td>{{ 'billing.catalog.serviceTypes.' + pkg.serviceType | transloco }}</td>
            <td class="col-right">{{ pkg.sessionQty }}</td>
            <td class="col-right price-cell">{{ pkg.price | number:'1.2-2' }}</td>
            <td class="col-right muted">{{ pkg.perSessionDisplay | number:'1.2-2' }}</td>
            <td>
              <span class="badge"
                    [class.badge-active]="pkg.status === 'ACTIVE'"
                    [class.badge-inactive]="pkg.status === 'ARCHIVED'">
                {{ 'billing.packages.status.' + pkg.status | transloco }}
              </span>
            </td>
            <td class="action-cell">
              <button *ngIf="pkg.status === 'ACTIVE'"
                      type="button" class="btn-link btn-sm text-danger"
                      (click)="archive(pkg)">
                {{ 'billing.packages.actions.archive' | transloco }}
              </button>
              <button *ngIf="pkg.status === 'ARCHIVED'"
                      type="button" class="btn-link btn-sm"
                      (click)="restore(pkg)">
                {{ 'billing.packages.actions.restore' | transloco }}
              </button>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="7">
              <div class="empty-state">
                <p>{{ 'billing.packages.list.empty' | transloco }}</p>
                <button type="button" class="btn-primary" (click)="openCreate()">
                  + {{ 'billing.packages.actions.create' | transloco }}
                </button>
              </div>
            </td>
          </tr>
        </ng-template>
      </p-table>

      <app-package-definition-form
        *ngIf="showForm"
        (saved)="onSaved($event)"
        (cancel)="showForm = false"
      />
    </div>
  `,
})
export class PackageDefinitionListComponent implements OnInit {
  definitions: PackageDefinition[] = [];
  loading = false;
  loadError = '';
  showForm = false;
  statusFilter: PackageDefinitionStatus | '' = '';
  pkgStatusOptions: { label: string; value: string }[] = [];

  constructor(private packageService: PackageService, private transloco: TranslocoService) {}

  ngOnInit(): void {
    this.pkgStatusOptions = [
      { label: this.transloco.translate('billing.packages.filters.allStatuses'), value: '' },
      { label: this.transloco.translate('billing.packages.status.ACTIVE'), value: 'ACTIVE' },
      { label: this.transloco.translate('billing.packages.status.ARCHIVED'), value: 'ARCHIVED' },
    ];
    this.loadDefinitions();
  }

  loadDefinitions(): void {
    this.loading = true;
    this.loadError = '';
    this.packageService
      .listDefinitions(this.statusFilter || undefined)
      .subscribe({
        next: (defs) => {
          this.definitions = defs;
          this.loading = false;
        },
        error: () => {
          this.loadError = 'billing.packages.list.loadError';
          this.loading = false;
        },
      });
  }

  openCreate(): void {
    this.showForm = true;
  }

  onSaved(req: CreatePackageDefinitionRequest): void {
    this.packageService.createDefinition(req).subscribe({
      next: (created) => {
        this.definitions = [...this.definitions, created];
        this.showForm = false;
      },
      error: () => {
        // error is displayed inside form component
      },
    });
  }

  archive(pkg: PackageDefinition): void {
    this.packageService
      .updateDefinitionStatus(pkg.id, { status: 'ARCHIVED' })
      .subscribe((updated) => this.replaceItem(updated));
  }

  restore(pkg: PackageDefinition): void {
    this.packageService
      .updateDefinitionStatus(pkg.id, { status: 'ACTIVE' })
      .subscribe((updated) => this.replaceItem(updated));
  }

  private replaceItem(updated: PackageDefinition): void {
    this.definitions = this.definitions.map((d) => (d.id === updated.id ? updated : d));
  }
}
