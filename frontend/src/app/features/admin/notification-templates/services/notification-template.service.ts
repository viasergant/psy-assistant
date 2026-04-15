import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  NotificationTemplate,
  CreateTemplateRequest,
  UpdateTemplateRequest,
  PreviewResponse,
  DeactivateResponse,
  NotificationEventType,
  NotificationChannel,
  NotificationLanguage,
  TemplateStatus
} from '../models/notification-template.model';

@Injectable({ providedIn: 'root' })
export class NotificationTemplateService {
  private readonly baseUrl = '/api/admin/notification-templates';

  constructor(private http: HttpClient) {}

  list(filters: {
    eventType?: NotificationEventType | '';
    channel?: NotificationChannel | '';
    language?: NotificationLanguage | '';
    status?: TemplateStatus | '';
  } = {}): Observable<NotificationTemplate[]> {
    let params = new HttpParams();
    if (filters.eventType) { params = params.set('eventType', filters.eventType); }
    if (filters.channel) { params = params.set('channel', filters.channel); }
    if (filters.language) { params = params.set('language', filters.language); }
    if (filters.status) { params = params.set('status', filters.status); }
    return this.http.get<NotificationTemplate[]>(this.baseUrl, { params });
  }

  get(id: string): Observable<NotificationTemplate> {
    return this.http.get<NotificationTemplate>(`${this.baseUrl}/${id}`);
  }

  create(req: CreateTemplateRequest): Observable<NotificationTemplate> {
    return this.http.post<NotificationTemplate>(this.baseUrl, req);
  }

  update(id: string, req: UpdateTemplateRequest): Observable<NotificationTemplate> {
    return this.http.put<NotificationTemplate>(`${this.baseUrl}/${id}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  activate(id: string): Observable<NotificationTemplate> {
    return this.http.post<NotificationTemplate>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<DeactivateResponse> {
    return this.http.post<DeactivateResponse>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  preview(id: string): Observable<PreviewResponse> {
    return this.http.post<PreviewResponse>(`${this.baseUrl}/${id}/preview`, {});
  }
}
