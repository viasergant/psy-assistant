import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { NotificationTemplateService } from '../../services/notification-template.service';
import { NotificationTemplate, PreviewResponse } from '../../models/notification-template.model';

@Component({
  selector: 'app-template-preview-dialog',
  standalone: true,
  imports: [CommonModule, TranslocoPipe],
  styleUrl: './template-preview-dialog.component.scss',
  template: `
    <div class="dialog-overlay" role="dialog"
         [attr.aria-label]="'admin.notificationTemplates.preview.title' | transloco">
      <div class="dialog dialog-preview">
        <div class="dialog-header">
          <div class="preview-header-info">
            <h2>{{ 'admin.notificationTemplates.preview.title' | transloco }}</h2>
            <div class="preview-meta">
              <span class="badge badge-info">
                {{ 'admin.notificationTemplates.channels.' + template.channel | transloco }}
              </span>
              <span class="badge badge-inactive">
                {{ 'admin.notificationTemplates.languages.' + template.language | transloco }}
              </span>
              <span>{{ 'admin.notificationTemplates.eventTypes.' + template.eventType | transloco }}</span>
            </div>
          </div>
          <button class="dialog-close" (click)="close()">✕</button>
        </div>

        <div class="dialog-content">
          <!-- Loading -->
          <div *ngIf="loading" class="state-msg" aria-live="polite">
            {{ 'admin.notificationTemplates.preview.loading' | transloco }}
          </div>

          <!-- Error -->
          <div *ngIf="!loading && loadError" class="alert-error" role="alert">{{ loadError }}</div>

          <!-- Preview content -->
          <ng-container *ngIf="!loading && !loadError && preview">
            <!-- Subject (email only) -->
            <div *ngIf="template.channel === 'EMAIL' && preview.renderedSubject" class="preview-section">
              <p class="preview-section-label">
                {{ 'admin.notificationTemplates.preview.subject' | transloco }}
              </p>
              <div class="preview-subject">{{ preview.renderedSubject }}</div>
            </div>

            <!-- Body -->
            <div class="preview-section">
              <p class="preview-section-label">
                {{ 'admin.notificationTemplates.preview.body' | transloco }}
              </p>

              <!-- HTML email preview in sandboxed iframe -->
              <div *ngIf="template.channel === 'EMAIL'" class="preview-iframe-wrapper">
                <iframe
                  #previewFrame
                  class="preview-iframe"
                  [srcdoc]="preview.renderedBody"
                  sandbox="allow-same-origin"
                  title="Email preview"
                  aria-label="Rendered email body preview">
                </iframe>
              </div>

              <!-- SMS plain text preview -->
              <div *ngIf="template.channel === 'SMS'" class="preview-sms">
                <pre class="sms-body">{{ preview.renderedBody }}</pre>
                <p class="sms-char-count">
                  {{ preview.renderedBody.length }} {{ 'admin.notificationTemplates.preview.chars' | transloco }}
                </p>
              </div>
            </div>

            <!-- Sample data note -->
            <p class="sample-note">
              {{ 'admin.notificationTemplates.preview.sampleNote' | transloco }}
            </p>
          </ng-container>
        </div>

        <div class="dialog-actions">
          <button class="btn-secondary" (click)="close()">
            {{ 'admin.notificationTemplates.preview.close' | transloco }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class TemplatePreviewDialogComponent implements OnInit {
  @Input() template!: NotificationTemplate;
  @Output() closed = new EventEmitter<void>();

  preview: PreviewResponse | null = null;
  loading = false;
  loadError: string | null = null;

  constructor(private svc: NotificationTemplateService) {}

  ngOnInit(): void {
    this.loading = true;
    this.svc.preview(this.template.id).subscribe({
      next: (p) => {
        this.preview = p;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'Failed to load preview.';
        this.loading = false;
      }
    });
  }

  close(): void {
    this.closed.emit();
  }
}
