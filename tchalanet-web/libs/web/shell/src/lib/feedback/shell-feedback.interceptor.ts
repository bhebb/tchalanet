import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  mapHttpErrorToProblemDetail,
  TCH_FEEDBACK_CONTEXT,
  webAppErrorFromProblemDetail,
} from '@tch/api';
import { presentWebAppError } from '@tch/web/errors';
import { catchError, throwError } from 'rxjs';

import { buildCopyText } from './copy-error-details';
import { ShellFeedbackItem } from './shell-feedback.model';
import { ShellFeedbackStore } from './shell-feedback.store';

/**
 * Routes an unhandled request failure to the shell only when the caller declares shell ownership.
 * Page, section, form, field, and silent requests keep their feedback local to their owner.
 */
export const shellFeedbackInterceptor: HttpInterceptorFn = (request, next) => {
  const context = request.context.get(TCH_FEEDBACK_CONTEXT);
  if (context.owner !== 'shell' || context.mode === 'local' || context.mode === 'silent') {
    return next(request);
  }

  const store = inject(ShellFeedbackStore);
  const translate = inject(TranslateService);

  return next(request).pipe(
    catchError(error => {
      const normalized = webAppErrorFromProblemDetail(
        mapHttpErrorToProblemDetail(error),
        request.urlWithParams,
        'shell',
      );
      const presented = presentWebAppError(normalized, key => translate.instant(key));
      const item: Omit<ShellFeedbackItem, 'id' | 'repeatCount' | 'copyText' | 'dismissible'> = {
        dedupeKey: normalized.dedupeKey,
        severity: normalized.severity,
        title: presented.viewModel.title,
        message: presented.viewModel.message,
        source: normalized.source,
        requestId: normalized.requestId,
        traceId: normalized.traceId,
        spanId: normalized.spanId,
        errorId: normalized.errorId,
        status: normalized.status,
      };
      store.add({ ...item, copyText: buildCopyText(item) });

      return throwError(() => error);
    }),
  );
};
