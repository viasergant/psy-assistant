import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface TelegramSettings {
  available: boolean;
  linked: boolean;
  linkedAt: string | null;
  timezone: string;
  language: string;
  digestEnabled: boolean;
  /** Local time, HH:mm. */
  digestTime: string;
  /** Weekday bit mask, bit 0 = Monday. */
  digestDays: number;
  skipEmptyDigest: boolean;
  reminderEnabled: boolean;
  reminderMinutesBefore: number;
  includeClientNames: boolean;
}

export type TelegramSettingsUpdate = Omit<TelegramSettings, 'available' | 'linked' | 'linkedAt'>;

/** Calls the self-service Telegram endpoints; every call acts on the authenticated user only. */
@Injectable({ providedIn: 'root' })
export class TelegramSettingsService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/telegram';

  get(): Observable<TelegramSettings> {
    return this.http.get<TelegramSettings>(`${this.base}/settings`);
  }

  update(settings: TelegramSettingsUpdate): Observable<TelegramSettings> {
    return this.http.put<TelegramSettings>(`${this.base}/settings`, settings);
  }

  createLink(): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`${this.base}/link`, {});
  }

  unlink(): Observable<void> {
    return this.http.delete<void>(`${this.base}/link`);
  }

  sendTest(): Observable<void> {
    return this.http.post<void>(`${this.base}/test`, {});
  }
}
