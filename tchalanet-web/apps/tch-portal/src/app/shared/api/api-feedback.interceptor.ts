import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';

import {
  ApiNotice,
  ApiResponse,
  ProblemDetail,
  SUPPRESS_SHELL_FEEDBACK,
  ServiceHealth,
  mapHttpErrorToProblemDetail,
} from '@tch/api';
import { APPLICATION_API_URL_PATTERN } from '@tch/shared-config';

import { buildCopyText } from '../feedback/copy-error-details';
import { AddShellFeedbackInput, ShellFeedbackSeverity } from '../feedback/shell-feedback.model';
import { ShellFeedbackStore } from '../feedback/shell-feedback.store';

export const apiFeedbackInterceptor: HttpInterceptorFn = (req, next) => {
  if (!APPLICATION_API_URL_PATTERN.test(req.url)) return next(req);
  if (req.context.get(SUPPRESS_SHELL_FEEDBACK)) return next(req);

  const store = inject(ShellFeedbackStore);

  return next(req).pipe(
    tap(event => {
      if (!(event instanceof HttpResponse)) return;
      const body = event.body;
      if (!isApiResponse(body)) return;

      body.notices.forEach(notice => {
        if (notice.severity === 'info' || notice.severity === 'success') return;
        store.add(fromNotice(notice));
      });

      body.serviceHealth?.forEach(sh => {
        if (!sh.status.healthy) {
          store.add(fromUnhealthyService(sh));
        }
      });
    }),
    catchError(error => {
      const problem = isProblemDetail(error) ? error : mapHttpErrorToProblemDetail(error);
      store.add(fromProblemDetail(problem, req.url));
      return throwError(() => error);
    }),
  );
};

function fromNotice(notice: ApiNotice): AddShellFeedbackInput {
  const severity: ShellFeedbackSeverity = notice.severity === 'warning' ? 'warn' : 'info';
  return {
    severity,
    title: notice.code,
    message: notice.message,
    source: notice.target,
  };
}

function fromUnhealthyService(sh: ServiceHealth): AddShellFeedbackInput {
  return {
    severity: 'warn',
    title: sh.service,
    message: sh.status.label,
    source: sh.service,
  };
}

function fromProblemDetail(problem: ProblemDetail, url: string): AddShellFeedbackInput {
  const severity: ShellFeedbackSeverity =
    problem.status >= 500 || problem.status === 0 ? 'error' : 'warn';
  const requestId = problem.requestId ?? problem.correlationId;
  const traceId = problem.traceId;
  const spanId = problem.spanId;
  const source = problem.instance ?? url;

  const item: AddShellFeedbackInput = {
    severity,
    title: problem.title,
    message: problem.detail ?? problem.title,
    status: problem.status,
    requestId,
    traceId,
    spanId,
    source,
  };

  return {
    ...item,
    copyText: buildCopyText(item),
  };
}

function isApiResponse(body: unknown): body is ApiResponse<unknown> {
  return (
    typeof body === 'object' &&
    body !== null &&
    'data' in body &&
    'status' in body &&
    'notices' in body
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
