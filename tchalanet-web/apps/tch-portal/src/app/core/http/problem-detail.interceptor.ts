import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { mapHttpErrorToProblemDetail } from './http-error.mapper';

export const problemDetailInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(catchError(error => throwError(() => mapHttpErrorToProblemDetail(error))));
