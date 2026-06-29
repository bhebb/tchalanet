import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { APPLICATION_API_URL_PATTERN } from '@tch/shared-config';

import { AUTH_CLIENT } from './auth-client';

const AUTH_RETRY_TOKEN = new HttpContextToken<boolean>(() => false);

export const authBearerInterceptor: HttpInterceptorFn = (req, next) => {
  if (!APPLICATION_API_URL_PATTERN.test(req.url)) {
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
