import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ClientTelegramStatus {
  /** Whether Telegram is configured on this server. */
  available: boolean;
  linked: boolean;
}

/** Calls the per-client Telegram endpoints; access is enforced by the server's client-visibility rules. */
@Injectable({ providedIn: 'root' })
export class ClientTelegramService {
  private readonly http = inject(HttpClient);

  status(clientId: string): Observable<ClientTelegramStatus> {
    return this.http.get<ClientTelegramStatus>(this.url(clientId));
  }

  /** Creates a one-time invite link; session times are shown to the client in {@code timezone}. */
  createInvite(clientId: string, language: string, timezone: string): Observable<{ url: string }> {
    const params = new HttpParams().set('language', language).set('timezone', timezone);
    return this.http.post<{ url: string }>(`${this.url(clientId)}/link`, {}, { params });
  }

  unlink(clientId: string): Observable<void> {
    return this.http.delete<void>(this.url(clientId));
  }

  private url(clientId: string): string {
    return `/api/v1/clients/${encodeURIComponent(clientId)}/telegram`;
  }
}
