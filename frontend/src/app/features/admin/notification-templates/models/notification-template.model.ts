export type NotificationEventType =
  | 'APPOINTMENT_REMINDER'
  | 'APPOINTMENT_CANCELLATION'
  | 'NO_SHOW_FOLLOWUP'
  | 'WELCOME'
  | 'PASSWORD_RESET';

export type NotificationChannel = 'EMAIL' | 'SMS';

export type NotificationLanguage = 'EN' | 'UK';

export type TemplateStatus = 'ACTIVE' | 'INACTIVE';

export interface NotificationTemplate {
  id: string;
  eventType: NotificationEventType;
  channel: NotificationChannel;
  language: NotificationLanguage;
  subject: string | null;
  body: string;
  status: TemplateStatus;
  hasUnknownVariables: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTemplateRequest {
  eventType: NotificationEventType;
  channel: NotificationChannel;
  language: NotificationLanguage;
  subject: string | null;
  body: string;
}

export interface UpdateTemplateRequest {
  subject: string | null;
  body: string;
}

export interface PreviewResponse {
  renderedSubject: string | null;
  renderedBody: string;
}

export interface DeactivateResponse {
  template: NotificationTemplate;
  noActiveReplacement: boolean;
}

export interface TemplateFilters {
  eventType?: NotificationEventType | '';
  channel?: NotificationChannel | '';
  language?: NotificationLanguage | '';
  status?: TemplateStatus | '';
}

export const EVENT_TYPE_OPTIONS: NotificationEventType[] = [
  'APPOINTMENT_REMINDER',
  'APPOINTMENT_CANCELLATION',
  'NO_SHOW_FOLLOWUP',
  'WELCOME',
  'PASSWORD_RESET'
];

export const CHANNEL_OPTIONS: NotificationChannel[] = ['EMAIL', 'SMS'];

export const LANGUAGE_OPTIONS: NotificationLanguage[] = ['EN', 'UK'];

export const SUPPORTED_VARIABLES = [
  '{{client_name}}',
  '{{therapist_name}}',
  '{{appointment_datetime}}',
  '{{appointment_date}}',
  '{{appointment_time}}',
  '{{location}}',
  '{{organization_name}}'
];

export const SMS_SINGLE_SEGMENT = 160;
