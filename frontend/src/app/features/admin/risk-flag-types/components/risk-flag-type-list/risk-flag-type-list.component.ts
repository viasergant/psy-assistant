import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { RiskFlagType } from '../../models/risk-flag-type-admin.model';
import { RiskFlagTypeAdminService } from '../../services/risk-flag-type-admin.service';
import { RiskFlagTypeFormDialogComponent } from '../risk-flag-type-form-dialog/risk-flag-type-form-dialog.component';

@Component({
  selector: 'app-risk-flag-type-list',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, RiskFlagTypeFormDialogComponent],
  template: `
    <div class="page">
      <header class="page-header">
        <h1>{{ 'admin.riskFlagTypes.title' | transloco }}</h1>
        <button class="btn-primary" (click)="openCreate()">
          + {{ 'admin.riskFlagTypes.list.createButton' | transloco }}
        </button>
      </header>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'admin.riskFlagTypes.list.loading' | transloco }}
      </div>
      <div *ngIf="!loading && loadError" class="alert-error" role="alert">{{ loadError }}</div>
      <div *ngIf="!loading && !loadError && flagTypes.length === 0" class="state-msg">
        {{ 'admin.riskFlagTypes.list.empty' | transloco }}
      </div>

      <div class="table-wrapper table-hoverable" *ngIf="!loading && !loadError && flagTypes.length > 0">
        <table [attr.aria-label]="'admin.riskFlagTypes.list.ariaList' | transloco">
          <thead>
            <tr>
              <th scope="col">{{ 'admin.riskFlagTypes.list.colName' | transloco }}</th>
              <th scope="col">{{ 'admin.riskFlagTypes.list.colDisplayOrder' | transloco }}</th>
              <th scope="col">{{ 'admin.riskFlagTypes.list.colActive' | transloco }}</th>
              <th scope="col" class="table-actions">
                <span class="sr-only">{{ 'admin.riskFlagTypes.list.colActions' | transloco }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let ft of flagTypes">
              <td>{{ ft.name }}</td>
              <td>{{ ft.displayOrder }}</td>
              <td>
                <span class="badge" [ngClass]="ft.active ? 'badge-active' : 'badge-inactive'">
                  {{ (ft.active ? 'admin.riskFlagTypes.list.statusYes' : 'admin.riskFlagTypes.list.statusNo') | transloco }}
                </span>
              </td>
              <td class="table-actions">
                <button
                  *ngIf="ft.active"
                  class="btn-table-action text-muted"
                  [title]="'admin.riskFlagTypes.list.actionDeactivate' | transloco"
                  (click)="deactivate(ft)"
                >
                  ✕
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="actionError" class="alert-error" role="alert">
        {{ actionError }}
        <button class="btn-ghost btn-sm" (click)="actionError = null">✕</button>
      </div>

      <app-risk-flag-type-form-dialog
        *ngIf="showForm"
        (saved)="onSaved()"
        (cancelled)="showForm = false">
      </app-risk-flag-type-form-dialog>
    </div>
  `
})
export class RiskFlagTypeListComponent implements OnInit {
  flagTypes: RiskFlagType[] = [];
  loading = false;
  loadError: string | null = null;
  actionError: string | null = null;
  showForm = false;

  constructor(private svc: RiskFlagTypeAdminService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.loadError = null;
    this.svc.listAll().subscribe({
      next: (data) => {
        this.flagTypes = data;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Failed to load flag types.';
        this.loading = false;
      }
    });
  }

  openCreate(): void {
    this.showForm = true;
  }

  onSaved(): void {
    this.showForm = false;
    this.load();
  }

  deactivate(ft: RiskFlagType): void {
    this.actionError = null;
    this.svc.deactivate(ft.id).subscribe({
      next: () => this.load(),
      error: () => {
        this.actionError = 'Failed to deactivate flag type.';
      }
    });
  }
}
