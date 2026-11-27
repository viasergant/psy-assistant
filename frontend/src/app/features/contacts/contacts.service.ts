import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PlatformContacts } from './contacts.model';

/** HTTP client for the public platform contacts endpoint. */
@Injectable({ providedIn: 'root' })
export class ContactsService {
  private readonly http = inject(HttpClient);

  /** Returns the configured contacts; available without authentication. */
  getContacts(): Observable<PlatformContacts> {
    return this.http.get<PlatformContacts>('/api/v1/contacts');
  }
}
