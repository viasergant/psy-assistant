import { Injectable, inject } from '@angular/core';
import { Observable, of, shareReplay, switchMap } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AppModeService } from './app-mode.service';
import { TherapistManagementService } from '../../features/admin/therapists/services/therapist-management.service';

/**
 * Resolves the single therapist's profile ID in solo deployment mode.
 *
 * In solo mode there is exactly one therapist but the SYSTEM_ADMINISTRATOR's
 * JWT carries no therapistProfileId claim. This service fetches and caches
 * that ID so every component can resolve it without making duplicate requests.
 *
 * Returns null in clinic/saas modes or when no therapist exists.
 */
@Injectable({ providedIn: 'root' })
export class SoloTherapistService {
  private readonly modeService = inject(AppModeService);
  private readonly therapistService = inject(TherapistManagementService);

  /** Emits the solo therapist's profile ID once and replays it to late subscribers. */
  readonly profileId$: Observable<string | null> = this.modeService.mode$.pipe(
    switchMap(mode => {
      if (mode !== 'SOLO') return of(null);
      return this.therapistService.getTherapists(0, 1, undefined, true).pipe(
        map(page => page.content[0]?.id ?? null),
        catchError(() => of(null)),
      );
    }),
    shareReplay(1),
  );
}
