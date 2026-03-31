import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { I18nService } from '../i18n/i18n.service';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  tokenType: string;
}

export interface FirstLoginPasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

/**
 * Manages the authenticated session using an in-memory BehaviorSubject.
 *
 * The access token is NEVER persisted to localStorage or sessionStorage.
 * After a page reload the APP_INITIALIZER calls /api/v1/auth/refresh using
 * the HttpOnly cookie to rehydrate the token silently.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiBase = '/api/v1/auth';
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly i18nService = inject(I18nService);

  /** In-memory access token — null means unauthenticated. */
  private readonly tokenSubject = new BehaviorSubject<string | null>(null);

  /** Observable stream of the current access token. */
  readonly token$ = this.tokenSubject.asObservable();

  /** Returns the current in-memory access token. */
  get token(): string | null {
    return this.tokenSubject.getValue();
  }

  /** Returns true when an access token is present in memory. */
  isAuthenticated(): boolean {
    return this.token !== null;
  }

  /**
   * Sends login credentials and stores the returned access token in memory.
   * Syncs locale preference with backend after successful login.
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiBase}/login`, credentials).pipe(
      tap(response => {
        this.tokenSubject.next(response.accessToken);
        // Sync locale with backend asynchronously (fire-and-forget)
        this.i18nService.syncWithBackend().catch(err => 
          console.error('[AuthService] Failed to sync locale after login:', err)
        );
      })
    );
  }

  /**
   * Exchanges the HttpOnly refresh-token cookie for a new access token.
   * Called by the APP_INITIALIZER on app boot and by the JwtInterceptor on 401.
   * Syncs locale preference with backend after successful refresh.
   */
  refreshToken(): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiBase}/refresh`, {}, { withCredentials: true })
      .pipe(
        tap(response => {
          this.tokenSubject.next(response.accessToken);
          // Sync locale with backend asynchronously (fire-and-forget)
          this.i18nService.syncWithBackend().catch(err => 
            console.error('[AuthService] Failed to sync locale after refresh:', err)
          );
        })
      ls: true })
      .pipe(tap(response => this.tokenSubject.next(response.accessToken)));
  }

  /**
   * Sets the in-memory access token directly (used by APP_INITIALIZER after
   * a successful silent refresh).
   */
  se Syncs locale preference with backend after successful change.
   */
  changePasswordFirstLogin(request: FirstLoginPasswordChangeRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiBase}/first-login-password-change`, request)
      .pipe(
        tap(response => {
          this.tokenSubject.next(response.accessToken);
          // Sync locale with backend asynchronously (fire-and-forget)
          this.i18nService.syncWithBackend().catch(err => 
            console.error('[AuthService] Failed to sync locale after password change:', err)
          );
        })
      
  logout(): void {
    this.http
      .post(`${this.apiBase}/logout`, {}, { withCredentials: true })
      .subscribe({ error: () => {} }); // fire-and-forget
    this.tokenSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  /**
   * Changes password on first login when user has mustChangePassword flag.
   * Returns new auth tokens after successful password change.
   */
  changePasswordFirstLogin(request: FirstLoginPasswordChangeRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiBase}/first-login-password-change`, request)
      .pipe(tap(response => this.tokenSubject.next(response.accessToken)));
  }
}
