import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { mapHttpErrorToProblemDetail } from './http-error.mapper';

export const problemDetailInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError(error => {
      // HttpErrorResponse is already an Error. Keeping it intact lets rxResource enter its error
      // state while the mapper still extracts the structured ProblemDetail from `error.error`.
      if (error instanceof HttpErrorResponse) {
        return throwError(() => error);
      }

      const problem = mapHttpErrorToProblemDetail(error);
      const normalized = new Error(problem.title || 'HTTP request failed');

      // rxResource requires an Error instance. Keep the structured ProblemDetail as the cause so
      // resourceErrorVm and direct feature subscriptions can still resolve the same diagnostics.
      (normalized as Error & { cause?: unknown }).cause = problem;
      return throwError(() => normalized);
    }),
  );
