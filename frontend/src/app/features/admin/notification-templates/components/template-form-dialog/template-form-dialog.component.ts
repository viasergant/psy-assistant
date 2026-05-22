import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Select } from 'primeng/select';
import { NotificationTemplateService } from '../../services/notification-template.service';
import {
  NotificationTemplate,
  NotificationEventType,
  NotificationChannel,
  NotificationLanguage,
  EVENT_TYPE_OPTIONS,
  CHANNEL_OPTIONS,
  LANGUAGE_OPTIONS,
  SUPPORTED_VARIABLES,
  SMS_SINGLE_SEGMENT
} from '../../models/notification-template.model';

@Component({
  selector: 'app-template-form-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoPipe, Select],
  styleUrl: './template-form-dialog.component.scss',
  template: `
    <div class="dialog-overlay" role="dialog"
         [attr.aria-label]="(editTemplate ? 'admin.notificationTemplates.form.editTitle' :
                                             'admin.notificationTemplates.form.createTitle') | transloco">
      <div class="dialog dialog-lg">
        <div class="dialog-header">
          <h2>
            {{ (editTemplate ? 'admin.notificationTemplates.form.editTitle' :
                               'admin.notificationTemplates.form.createTitle') | transloco }}
          </h2>
          <button class="dialog-close" (click)="cancel()">✕</button>
        </div>

        <div class="dialog-content">
          <form #f="ngForm" (ngSubmit)="submit(f.valid ?? false)" novalidate>

            <!-- Meta fields: disabled for edit -->
            <div class="field-row" *ngIf="!editTemplate">
              <div class="field">
                <label for="eventType">
                  {{ 'admin.notificationTemplates.form.eventType' | transloco }}
                  <span class="required">*</span>
                </label>
                <p-select
                  inputId="eventType"
                  name="eventType"
                  [(ngModel)]="eventType"
                  [options]="eventTypeSelectOptions"
                  optionLabel="label"
                  optionValue="value"
                  required
                  styleClass="w-full"
                />
              </div>
              <div class="field">
                <label for="channel">
                  {{ 'admin.notificationTemplates.form.channel' | transloco }}
                  <span class="required">*</span>
                </label>
                <p-select
                  inputId="channel"
                  name="channel"
                  [(ngModel)]="channel"
                  [options]="channelSelectOptions"
                  optionLabel="label"
                  optionValue="value"
                  required
                  (onChange)="onChannelChange()"
                  styleClass="w-full"
                />
              </div>
              <div class="field">
                <label for="language">
                  {{ 'admin.notificationTemplates.form.language' | transloco }}
                  <span class="required">*</span>
                </label>
                <p-select
                  inputId="language"
                  name="language"
                  [(ngModel)]="language"
                  [options]="languageSelectOptions"
                  optionLabel="label"
                  optionValue="value"
                  required
                  styleClass="w-full"
                />
              </div>
            </div>

            <!-- Meta readonly display for edit -->
            <div class="meta-row" *ngIf="editTemplate">
              <span class="meta-item">
                <strong>{{ 'admin.notificationTemplates.form.eventType' | transloco }}:</strong>
                {{ 'admin.notificationTemplates.eventTypes.' + editTemplate.eventType | transloco }}
              </span>
              <span class="meta-item">
                <strong>{{ 'admin.notificationTemplates.form.channel' | transloco }}:</strong>
                {{ 'admin.notificationTemplates.channels.' + editTemplate.channel | transloco }}
              </span>
              <span class="meta-item">
                <strong>{{ 'admin.notificationTemplates.form.language' | transloco }}:</strong>
                {{ 'admin.notificationTemplates.languages.' + editTemplate.language | transloco }}
              </span>
            </div>

            <!-- Subject (email only) -->
            <div class="field" *ngIf="effectiveChannel === 'EMAIL'">
              <label for="subject">{{ 'admin.notificationTemplates.form.subject' | transloco }}</label>
              <input id="subject" name="subject" type="text" [(ngModel)]="subject"
                     [placeholder]="'admin.notificationTemplates.form.subjectPlaceholder' | transloco" />
            </div>

            <!-- Body -->
            <div class="field">
              <div class="field-label-row">
                <label for="body">
                  {{ 'admin.notificationTemplates.form.body' | transloco }}
                  <span class="required">*</span>
                </label>
                <span *ngIf="effectiveChannel === 'SMS'" class="char-count"
                      [ngClass]="{ 'char-count-warn': body.length > smsSegmentSize }">
                  {{ body.length }} / {{ smsSegmentSize }}
                  <span *ngIf="body.length > smsSegmentSize" class="multi-segment-label">
                    ({{ 'admin.notificationTemplates.form.multiSegment' | transloco }}
                    {{ smsSegmentCount }} {{ 'admin.notificationTemplates.form.segments' | transloco }})
                  </span>
                </span>
              </div>
              <textarea id="body" name="body" [(ngModel)]="body" required rows="10"
                        [placeholder]="'admin.notificationTemplates.form.bodyPlaceholder' | transloco">
              </textarea>
            </div>

            <!-- Variable helper -->
            <div class="var-helper">
              <p class="var-helper-label">
                {{ 'admin.notificationTemplates.form.supportedVars' | transloco }}:
              </p>
              <div class="var-chips">
                <button type="button" class="var-chip" *ngFor="let v of supportedVariables"
                        (click)="insertVariable(v)" [title]="'admin.notificationTemplates.form.insertVar' | transloco">
                  {{ v }}
                </button>
              </div>
            </div>

            <!-- Unknown variable warning -->
            <div *ngIf="unknownVarsWarning.length > 0" class="alert-warning" role="alert">
              ⚠ {{ 'admin.notificationTemplates.form.unknownVarsWarning' | transloco }}:
              <strong>{{ unknownVarsWarning.join(', ') }}</strong>
            </div>

            <!-- Error -->
            <div *ngIf="saveError" class="alert-error" role="alert">{{ saveError }}</div>

          </form>
        </div>

        <div class="dialog-actions">
          <button type="button" class="btn-secondary" (click)="cancel()">
            {{ 'admin.notificationTemplates.form.cancel' | transloco }}
          </button>
          <button type="button" class="btn-primary" [disabled]="saving" (click)="submit(true)">
            {{ saving ? ('admin.notificationTemplates.form.saving' | transloco) :
                        ('admin.notificationTemplates.form.save' | transloco) }}
          </button>
        </div>
      </div>
    </div>
  `
})
export class TemplateFormDialogComponent implements OnInit, OnChanges {
  @Input() editTemplate: NotificationTemplate | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  eventType: NotificationEventType | '' = '';
  channel: NotificationChannel | '' = '';
  language: NotificationLanguage | '' = '';
  subject = '';
  body = '';

  saving = false;
  saveError: string | null = null;
  unknownVarsWarning: string[] = [];

  readonly eventTypeOptions = EVENT_TYPE_OPTIONS;
  readonly channelOptions = CHANNEL_OPTIONS;
  readonly languageOptions = LANGUAGE_OPTIONS;
  readonly supportedVariables = SUPPORTED_VARIABLES;
  readonly smsSegmentSize = SMS_SINGLE_SEGMENT;
  eventTypeSelectOptions: { label: string; value: string }[] = [];
  channelSelectOptions: { label: string; value: string }[] = [];
  languageSelectOptions: { label: string; value: string }[] = [];

  constructor(private svc: NotificationTemplateService, private transloco: TranslocoService) {}

  ngOnInit(): void {
    this.eventTypeSelectOptions = [
      { label: '— ' + this.transloco.translate('admin.notificationTemplates.form.select') + ' —', value: '' },
      ...this.eventTypeOptions.map(e => ({ label: this.transloco.translate('admin.notificationTemplates.eventTypes.' + e), value: e })),
    ];
    this.channelSelectOptions = [
      { label: '— ' + this.transloco.translate('admin.notificationTemplates.form.select') + ' —', value: '' },
      ...this.channelOptions.map(c => ({ label: this.transloco.translate('admin.notificationTemplates.channels.' + c), value: c })),
    ];
    this.languageSelectOptions = [
      { label: '— ' + this.transloco.translate('admin.notificationTemplates.form.select') + ' —', value: '' },
      ...this.languageOptions.map(l => ({ label: this.transloco.translate('admin.notificationTemplates.languages.' + l), value: l })),
    ];
    this.populate();
  }

  ngOnChanges(): void {
    this.populate();
  }

  get effectiveChannel(): NotificationChannel | '' {
    return this.editTemplate ? this.editTemplate.channel : this.channel;
  }

  get smsSegmentCount(): number {
    return Math.ceil(this.body.length / SMS_SINGLE_SEGMENT);
  }

  private populate(): void {
    if (this.editTemplate) {
      this.subject = this.editTemplate.subject ?? '';
      this.body = this.editTemplate.body;
    } else {
      this.eventType = '';
      this.channel = '';
      this.language = '';
      this.subject = '';
      this.body = '';
    }
    this.unknownVarsWarning = [];
    this.saveError = null;
  }

  onChannelChange(): void {
    if (this.channel === 'SMS') { this.subject = ''; }
  }

  insertVariable(v: string): void {
    this.body += v;
    this.validateVars();
  }

  private validateVars(): void {
    const tokenRegex = /\{\{([a-z_]+)\}\}/g;
    const supported = SUPPORTED_VARIABLES.map(s => s.replace(/\{\{|\}\}/g, ''));
    const found: string[] = [];
    let match: RegExpExecArray | null;
    const text = (this.subject || '') + ' ' + this.body;
    tokenRegex.lastIndex = 0;
    while ((match = tokenRegex.exec(text)) !== null) {
      if (!supported.includes(match[1]) && !found.includes(match[1])) {
        found.push(match[1]);
      }
    }
    this.unknownVarsWarning = found;
  }

  cancel(): void {
    this.cancelled.emit();
  }

  submit(valid: boolean): void {
    if (!valid) { return; }
    this.validateVars();
    this.saveError = null;
    this.saving = true;

    const obs = this.editTemplate
      ? this.svc.update(this.editTemplate.id, { subject: this.subject || null, body: this.body })
      : this.svc.create({
          eventType: this.eventType as NotificationEventType,
          channel: this.channel as NotificationChannel,
          language: this.language as NotificationLanguage,
          subject: this.subject || null,
          body: this.body
        });

    obs.subscribe({
      next: () => {
        this.saving = false;
        this.saved.emit();
      },
      error: (err) => {
        this.saving = false;
        if (err.status === 409) {
          this.saveError = 'An active template already exists for this event type, channel, and language combination.';
        } else {
          this.saveError = 'Failed to save template. Please try again.';
        }
      }
    });
  }
}
