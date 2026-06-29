import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { catchError, tap, throwError } from 'rxjs';

import {
  ApiNotice,
  ApiResponse,
  ProblemDetail,
  SUPPRESS_SHELL_FEEDBACK,
  WebAppError,
  mapHttpErrorToProblemDetail,
  webAppErrorFromNotice,
  webAppErrorFromProblemDetail,
  webAppErrorFromServiceStatus,
} from '@tch/api';
import { APPLICATION_API_URL_PATTERN } from '@tch/shared-config';
import { AddShellFeedbackInput, ShellFeedbackStore, buildCopyText } from '@tch/web/shell';

import { resolveErrorFeedbackCopy } from '@tch/web/errors';

export const apiFeedbackInterceptor: HttpInterceptorFn = (req, next) => {
  if (!APPLICATION_API_URL_PATTERN.test(req.url)) return next(req);
  if (req.context.get(SUPPRESS_SHELL_FEEDBACK)) return next(req);

  const store = inject(ShellFeedbackStore);
  const translate = inject(TranslateService);

  return next(req).pipe(
    tap(event => {
      if (!(event instanceof HttpResponse)) return;
      const body = event.body;
      if (!isApiResponse(body)) return;

      body.notices.forEach(notice => {
        if (isQuietNotice(notice)) return;
        if (isLocallyOwnedNotice(notice)) return;
        store.add(fromWebAppError(webAppErrorFromNotice(notice, body.trace, req.url), key => translate.instant(key) as string));
      });

      const services = body.services ?? body.serviceHealth ?? [];
      services.forEach(service => {
        if (service.status !== 'UP') {
          store.add(fromWebAppError(webAppErrorFromServiceStatus(service, body.trace, req.url), key => translate.instant(key) as string));
        }
      });
    }),
    catchError(error => {
      const problem = isProblemDetail(error) ? error : mapHttpErrorToProblemDetail(error);
      store.add(fromWebAppError(webAppErrorFromProblemDetail(problem, req.url), key => translate.instant(key) as string));
      return throwError(() => error);
    }),
  );
};

function fromWebAppError(error: WebAppError, translate: (key: string) => string): AddShellFeedbackInput {
  const copy = resolveErrorFeedbackCopy(error, translate);
  const item: AddShellFeedbackInput = {
    severity: error.severity,
    title: copy.title,
    message: copy.message,
    status: error.status,
    requestId: error.requestId,
    traceId: error.traceId,
    spanId: error.spanId,
    errorId: error.errorId,
    source: error.source,
    dedupeKey: error.dedupeKey,
  };

  return {
    ...item,
    copyText: buildCopyText(item),
  };
}

function isQuietNotice(notice: ApiNotice): boolean {
  return notice.severity === 'INFO' || notice.severity === 'info' || notice.severity === 'success';
}

function isLocallyOwnedNotice(notice: ApiNotice): boolean {
  const surface = notice.meta?.['surface'];
  return surface === 'page' || surface === 'section' || surface === 'field';
}

function isApiResponse(body: unknown): body is ApiResponse<unknown> {
  return (
    typeof body === 'object' &&
    body !== null &&
    'data' in body &&
    'status' in body &&
    Array.isArray((body as { notices?: unknown }).notices)
  );
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    'title' in value &&
    typeof (value as Record<string, unknown>)['title'] === 'string' &&
    'status' in value &&
    typeof (value as Record<string, unknown>)['status'] === 'number'
  );
}
