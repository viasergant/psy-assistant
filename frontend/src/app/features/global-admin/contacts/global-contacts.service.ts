import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PlatformContacts } from '../../contacts/contacts.model';

/** Update payload; blank values clear the field. */
export interface PlatformContactsUpdate {
  phone: string;
  email: string;
  telegramChannels: string[];
}

/** HTTP client for the global administrator contacts endpoints. */
@Injectable({ providedIn: 'root' })
export class GlobalContactsService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/global-admin/contacts';

  getContacts(): Observable<PlatformContacts> {
    return this.http.get<PlatformContacts>(this.base);
  }

  updateContacts(payload: PlatformContactsUpdate): Observable<PlatformContacts> {
    return this.http.put<PlatformContacts>(this.base, payload);
  }
}
