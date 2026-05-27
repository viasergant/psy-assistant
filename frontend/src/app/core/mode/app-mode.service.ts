import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, catchError, firstValueFrom, of, tap } from 'rxjs';

export type AppMode = 'SOLO' | 'CLINIC';

interface MetaResponse {
  mode: string;
  version: string;
}

/**
 * Singleton service that fetches the deployment mode from GET /api/v1/meta
 * on application startup via APP_INITIALIZER.
 *
 * This is the ONLY Angular class that calls GET /api/v1/meta.
 * Mode conditioning for UI belongs in templates (*appIfMode) and route guards (ModeGuard).
 */
@Injectable({ providedIn: 'root' })
export class AppModeService {
  private readonly http = inject(HttpClient);

  private readonly modeSubject = new BehaviorSubject<AppMode | null>(null);

  readonly mode$ = this.modeSubject.asObservable();

  get mode(): AppMode | null {
    return this.modeSubject.getValue();
  }

  /**
   * Fetches the deployment mode from /api/v1/meta and stores it.
   * Called by APP_INITIALIZER before the app renders.
   * Errors are swallowed so the app still boots if the endpoint is unreachable.
   */
  async init(): Promise<void> {
    await firstValueFrom(
      this.http.get<MetaResponse>('/api/v1/meta').pipe(
        tap(res => this.modeSubject.next(res.mode as AppMode)),
        catchError(() => of(null))
      )
    );
  }
}
