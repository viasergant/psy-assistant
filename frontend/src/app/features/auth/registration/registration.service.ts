import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface RegistrationPayload {
  orgName: string;
  ownerEmail: string;
  password: string;
  captchaToken?: string;
}

/** Calls the public self-service registration endpoints (saas mode only). */
@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/register';

  /** Starts registration; the server answers 202 whether or not the email is already known. */
  register(payload: RegistrationPayload): Observable<void> {
    return this.http.post<void>(this.base, payload);
  }

  /** Confirms the email address using the token from the emailed link. */
  confirm(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/confirm`, { token });
  }

  /** Requests a fresh confirmation email. */
  resend(email: string, captchaToken?: string): Observable<void> {
    return this.http.post<void>(`${this.base}/resend`, { email, captchaToken });
  }
}
