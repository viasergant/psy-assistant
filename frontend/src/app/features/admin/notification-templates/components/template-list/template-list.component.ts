import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { NotificationTemplateService } from '../../services/notification-template.service';
import {
  NotificationTemplate,
  NotificationEventType,
  NotificationChannel,
  NotificationLanguage,
  TemplateStatus,
  EVENT_TYPE_OPTIONS,
  CHANNEL_OPTIONS,
  LANGUAGE_OPTIONS
} from '../../models/notification-template.model';
import { TemplateFormDialogComponent } from '../template-form-dialog/template-form-dialog.component';
import { TemplatePreviewDialogComponent } from '../template-preview-dialog/template-preview-dialog.component';

@Component({
  selector: 'app-template-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslocoPipe,
    TemplateFormDialogComponent,
    TemplatePreviewDialogComponent
  ],
  styleUrl: './template-list.component.scss',
  template: `
    <div class="page">
      <header class="page-header">
        <h1>{{ 'admin.notificationTemplates.title' | transloco }}</h1>
        <button class="btn-primary" (click)="openCreate()">
          + {{ 'admin.notificationTemplates.list.createButton' | transloco }}
        </button>
      </header>

      <!-- Filters -->
      <div class="filters" role="group" [attr.aria-label]="'admin.notificationTemplates.list.ariaFilters' | transloco">
        <div class="filter-group">
          <label for="eventTypeFilter">{{ 'admin.notificationTemplates.list.filterEventType' | transloco }}</label>
          <select id="eventTypeFilter" [(ngModel)]="filterEventType" (change)="load()">
            <option value="">{{ 'admin.notificationTemplates.list.filterAll' | transloco }}</option>
            <option *ngFor="let e of eventTypeOptions" [value]="e">
              {{ 'admin.notificationTemplates.eventTypes.' + e | transloco }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label for="channelFilter">{{ 'admin.notificationTemplates.list.filterChannel' | transloco }}</label>
          <select id="channelFilter" [(ngModel)]="filterChannel" (change)="load()">
            <option value="">{{ 'admin.notificationTemplates.list.filterAll' | transloco }}</option>
            <option *ngFor="let c of channelOptions" [value]="c">
              {{ 'admin.notificationTemplates.channels.' + c | transloco }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label for="languageFilter">{{ 'admin.notificationTemplates.list.filterLanguage' | transloco }}</label>
          <select id="languageFilter" [(ngModel)]="filterLanguage" (change)="load()">
            <option value="">{{ 'admin.notificationTemplates.list.filterAll' | transloco }}</option>
            <option *ngFor="let l of languageOptions" [value]="l">
              {{ 'admin.notificationTemplates.languages.' + l | transloco }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label for="statusFilter">{{ 'admin.notificationTemplates.list.filterStatus' | transloco }}</label>
          <select id="statusFilter" [(ngModel)]="filterStatus" (change)="load()">
            <option value="">{{ 'admin.notificationTemplates.list.filterAll' | transloco }}</option>
            <option value="ACTIVE">{{ 'admin.notificationTemplates.statuses.ACTIVE' | transloco }}</option>
            <option value="INACTIVE">{{ 'admin.notificationTemplates.statuses.INACTIVE' | transloco }}</option>
          </select>
        </div>
      </div>

      <!-- States -->
      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'admin.notificationTemplates.list.loading' | transloco }}
      </div>
      <div *ngIf="!loading && loadError" class="alert-error" role="alert">{{ loadError }}</div>
      <div *ngIf="!loading && !loadError && templates.length === 0" class="state-msg">
        {{ 'admin.notificationTemplates.list.empty' | transloco }}
      </div>

      <!-- Table -->
      <div class="table-wrapper table-hoverable" *ngIf="!loading && !loadError && templates.length > 0">
        <table [attr.aria-label]="'admin.notificationTemplates.list.ariaList' | transloco">
          <thead>
            <tr>
              <th scope="col">{{ 'admin.notificationTemplates.list.colEventType' | transloco }}</th>
              <th scope="col">{{ 'admin.notificationTemplates.list.colChannel' | transloco }}</th>
              <th scope="col">{{ 'admin.notificationTemplates.list.colLanguage' | transloco }}</th>
              <th scope="col">{{ 'admin.notificationTemplates.list.colStatus' | transloco }}</th>
              <th scope="col">{{ 'admin.notificationTemplates.list.colUpdated' | transloco }}</th>
              <th scope="col" class="table-actions">
                <span class="sr-only">{{ 'admin.notificationTemplates.list.colActions' | transloco }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let t of templates">
              <td>{{ 'admin.notificationTemplates.eventTypes.' + t.eventType | transloco }}</td>
              <td>
                <span class="badge" [ngClass]="t.channel === 'EMAIL' ? 'badge-info' : 'badge-warning'">
                  {{ 'admin.notificationTemplates.channels.' + t.channel | transloco }}
                </span>
              </td>
              <td>{{ 'admin.notificationTemplates.languages.' + t.language | transloco }}</td>
              <td>
                <span class="badge" [ngClass]="t.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'">
                  {{ 'admin.notificationTemplates.statuses.' + t.status | transloco }}
                </span>
                <span *ngIf="t.hasUnknownVariables" class="badge badge-warning unknown-vars-badge"
                      [title]="'admin.notificationTemplates.list.unknownVarsTooltip' | transloco">
                  ⚠
                </span>
              </td>
              <td>{{ t.updatedAt | date:'dd MMM yyyy, HH:mm' }}</td>
              <td class="table-actions">
                <button class="btn-table-action" (click)="openPreview(t)"
                        [title]="'admin.notificationTemplates.list.actionPreview' | transloco">
                  👁
                </button>
                <button class="btn-table-action" (click)="openEdit(t)"
                        [disabled]="t.status === 'ACTIVE'"
                        [title]="'admin.notificationTemplates.list.actionEdit' | transloco">
                  ✏️
                </button>
                <button *ngIf="t.status === 'INACTIVE'" class="btn-table-action text-success"
                        (click)="activate(t)"
                        [title]="'admin.notificationTemplates.list.actionActivate' | transloco">
                  ✓
                </button>
                <button *ngIf="t.status === 'ACTIVE'" class="btn-table-action text-muted"
                        (click)="deactivate(t)"
                        [title]="'admin.notificationTemplates.list.actionDeactivate' | transloco">
                  ✕
                </button>
                <button class="btn-table-action text-danger"
                        [disabled]="t.status === 'ACTIVE'"
                        (click)="confirmDelete(t)"
                        [title]="'admin.notificationTemplates.list.actionDelete' | transloco">
                  🗑
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Deactivation warning banner -->
      <div *ngIf="deactivationWarning" class="alert-warning" role="alert">
        {{ 'admin.notificationTemplates.list.noActiveWarning' | transloco }}
        <button class="btn-ghost btn-sm" (click)="deactivationWarning = false">✕</button>
      </div>

      <!-- Action error banner -->
      <div *ngIf="actionError" class="alert-error" role="alert">
        {{ actionError }}
        <button class="btn-ghost btn-sm" (click)="actionError = null">✕</button>
      </div>

      <!-- Delete confirmation dialog -->
      <div *ngIf="deleteTarget" class="dialog-overlay" role="dialog"
           [attr.aria-label]="'admin.notificationTemplates.delete.title' | transloco">
        <div class="dialog">
          <div class="dialog-header">
            <h2>{{ 'admin.notificationTemplates.delete.title' | transloco }}</h2>
          </div>
          <div class="dialog-content">
            <p>{{ 'admin.notificationTemplates.delete.confirm' | transloco }}</p>
          </div>
          <div class="dialog-actions">
            <button class="btn-secondary" (click)="deleteTarget = null">
              {{ 'admin.notificationTemplates.delete.cancel' | transloco }}
            </button>
            <button class="btn-danger" [disabled]="deleting" (click)="doDelete()">
              {{ deleting ? ('admin.notificationTemplates.delete.deleting' | transloco) :
                           ('admin.notificationTemplates.delete.confirm_btn' | transloco) }}
            </button>
          </div>
        </div>
      </div>

      <!-- Form dialog -->
      <app-template-form-dialog
        *ngIf="showForm"
        [editTemplate]="editTarget"
        (saved)="onSaved()"
        (cancelled)="showForm = false">
      </app-template-form-dialog>

      <!-- Preview dialog -->
      <app-template-preview-dialog
        *ngIf="previewTarget"
        [template]="previewTarget"
        (closed)="previewTarget = null">
      </app-template-preview-dialog>
    </div>
  `
})
export class TemplateListComponent implements OnInit {
  templates: NotificationTemplate[] = [];
  loading = false;
  loadError: string | null = null;
  actionError: string | null = null;
  deactivationWarning = false;

  filterEventType: NotificationEventType | '' = '';
  filterChannel: NotificationChannel | '' = '';
  filterLanguage: NotificationLanguage | '' = '';
  filterStatus: TemplateStatus | '' = '';

  showForm = false;
  editTarget: NotificationTemplate | null = null;
  previewTarget: NotificationTemplate | null = null;
  deleteTarget: NotificationTemplate | null = null;
  deleting = false;

  readonly eventTypeOptions = EVENT_TYPE_OPTIONS;
  readonly channelOptions = CHANNEL_OPTIONS;
  readonly languageOptions = LANGUAGE_OPTIONS;

  constructor(private svc: NotificationTemplateService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.loadError = null;
    this.svc.list({
      eventType: this.filterEventType,
      channel: this.filterChannel,
      language: this.filterLanguage,
      status: this.filterStatus
    }).subscribe({
      next: (data) => {
        this.templates = data;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Failed to load templates.';
        this.loading = false;
      }
    });
  }

  openCreate(): void {
    this.editTarget = null;
    this.showForm = true;
  }

  openEdit(t: NotificationTemplate): void {
    this.editTarget = t;
    this.showForm = true;
  }

  openPreview(t: NotificationTemplate): void {
    this.previewTarget = t;
  }

  onSaved(): void {
    this.showForm = false;
    this.load();
  }

  activate(t: NotificationTemplate): void {
    this.actionError = null;
    this.svc.activate(t.id).subscribe({
      next: () => this.load(),
      error: (err) => {
        if (err.status === 409) {
          this.actionError = 'Activation conflict: another template was activated concurrently.';
        } else {
          this.actionError = 'Failed to activate template.';
        }
      }
    });
  }

  deactivate(t: NotificationTemplate): void {
    this.actionError = null;
    this.deactivationWarning = false;
    this.svc.deactivate(t.id).subscribe({
      next: (res) => {
        if (res.noActiveReplacement) {
          this.deactivationWarning = true;
        }
        this.load();
      },
      error: () => {
        this.actionError = 'Failed to deactivate template.';
      }
    });
  }

  confirmDelete(t: NotificationTemplate): void {
    this.deleteTarget = t;
  }

  doDelete(): void {
    if (!this.deleteTarget) { return; }
    this.deleting = true;
    this.svc.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteTarget = null;
        this.load();
      },
      error: () => {
        this.deleting = false;
        this.actionError = 'Failed to delete template.';
        this.deleteTarget = null;
      }
    });
  }
}
