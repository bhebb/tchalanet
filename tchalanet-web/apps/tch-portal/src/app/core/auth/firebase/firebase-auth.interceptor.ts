import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { FirebaseAuthService } from './firebase-auth.service';

const AUTH_RETRY_TOKEN = new HttpContextToken<boolean>(() => false);

export const firebaseAuthInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isTchalanetApiRequest(req.url)) {
    return next(req);
  }
  const auth = inject(FirebaseAuthService);

  return from(auth.getIdToken()).pipe(
    switchMap(token => {
      if (!token) {
        return next(req);
      }

      const authenticatedRequest = withBearerToken(req, token);
      return next(authenticatedRequest).pipe(
        catchError(error => {
          if (!(error instanceof HttpErrorResponse) || error.status !== 401 || req.context.get(AUTH_RETRY_TOKEN)) {
            return throwError(() => error);
          }

          return from(auth.getIdToken(true)).pipe(
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
  if (url.startsWith('/')) {
    return environment.apiBaseUrl.startsWith('/') && url.startsWith(environment.apiBaseUrl);
  }
  return url.startsWith(environment.apiBaseUrl);
}
