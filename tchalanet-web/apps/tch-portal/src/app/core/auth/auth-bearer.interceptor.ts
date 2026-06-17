import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AUTH_CLIENT } from './auth-client';

const AUTH_RETRY_TOKEN = new HttpContextToken<boolean>(() => false);

export const authBearerInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isTchalanetApiRequest(req.url)) {
    return next(req);
  }
  const auth = inject(AUTH_CLIENT);

  return from(auth.getAccessToken()).pipe(
    switchMap(token => {
      if (!token) {
        return next(req);
      }

      const authenticatedRequest = withBearerToken(req, token);
      return next(authenticatedRequest).pipe(
        catchError(error => {
          if (
            !(error instanceof HttpErrorResponse) ||
            error.status !== 401 ||
            req.context.get(AUTH_RETRY_TOKEN)
          ) {
            return throwError(() => error);
          }

          return from(auth.getAccessToken(true)).pipe(
            switchMap(refreshedToken => {
              if (!refreshedToken) {
                return throwError(() => error);
              }
              return next(
                withBearerToken(req, refreshedToken).clone({
                  context: req.context.set(AUTH_RETRY_TOKEN, true),
                }),
              );
            }),
          );
        }),
      );
    }),
  );
};

function withBearerToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}

function isTchalanetApiRequest(url: string): boolean {
  //url contains /api/v1/tenant, ou /api/v1/platform, ou /api/v1/admin
  return url.match(new RegExp(`^/?${environment.apiBasePath}/(tenant|platform|admin)/`)) !== null;
  // Alternatively, if all API endpoints are under /api/, you could simply check:
  //
}
