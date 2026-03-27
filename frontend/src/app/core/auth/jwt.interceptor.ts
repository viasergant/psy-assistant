import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Shared flag that prevents multiple concurrent refresh calls (refresh storm guard).
 * Declared at module scope so all interceptor invocations share the same instance.
 */
const refreshing$ = new BehaviorSubject<boolean>(false);

/**
 * HTTP interceptor that attaches the Bearer token to every request and
 * silently refreshes it when a 401 is received.
 *
 * Refresh storm guard: if a refresh is already in progress, subsequent 401
 * responses wait for it to complete instead of firing new refresh requests.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const addToken = (request: HttpRequest<unknown>, token: string | null) => {
    if (!token) return request;
    return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  };

  return next(addToken(req, authService.token)).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !req.url.includes('/api/v1/auth/')
      ) {
        if (refreshing$.getValue()) {
          // Another refresh is in flight — wait for it to finish, then retry
          return refreshing$.pipe(
            filter(isRefreshing => !isRefreshing),
            take(1),
            switchMap(() => next(addToken(req, authService.token)))
          );
        }

        refreshing$.next(true);

        return authService.refreshToken().pipe(
          switchMap(response => {
            refreshing$.next(false);
            return next(addToken(req, response.accessToken));
          }),
          catchError(refreshError => {
            refreshing$.next(false);
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
