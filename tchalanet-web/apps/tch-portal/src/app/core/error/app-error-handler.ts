import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandler, Injectable, Injector, inject } from '@angular/core';

import { ShellFeedbackStore } from '@tch/web/shell';

/** Angular wraps some thrown values; reach the original error when present. */
function unwrap(error: unknown): unknown {
  if (typeof error === 'object' && error !== null) {
    const v = error as Record<string, unknown>;
    if (v['ngOriginalError']) return v['ngOriginalError'];
    if (v['rejection']) return v['rejection'];
  }
  return error;
}

function isIgnorableBrowserNoise(error: unknown): boolean {
  const message = error instanceof Error ? error.message : typeof error === 'string' ? error : '';
  return message.includes('ResizeObserver loop completed with undelivered notifications')
    || message.includes('ResizeObserver loop limit exceeded');
}

@Injectable()
export class AppErrorHandler implements ErrorHandler {
  private readonly injector = inject(Injector);

  handleError(error: unknown): void {
    const original = unwrap(error);

    // HTTP errors are already surfaced with ProblemDetail, trace id and copy by
    // apiFeedbackInterceptor. Re-reporting them here produced a duplicate, technical
    // "Erreur inattendue [object Object]" banner. Skip them.
    if (original instanceof HttpErrorResponse) return;
    if (isIgnorableBrowserNoise(original)) return;

    console.error('[AppError]', original);

    const store = this.injector.get(ShellFeedbackStore, null);
    if (!store) return;

    store.add({
      dedupeKey: 'frontend.unexpected|shell',
      severity: 'error',
      title: 'frontend.unexpected',
      message: 'Un probleme est survenu. Copiez la reference support si le probleme persiste.',
      copyText: buildRuntimeCopyText(original),
    });
  }
}

function buildRuntimeCopyText(error: unknown): string {
  const parts = [
    'category=unexpected',
    `time=${new Date().toISOString()}`,
  ];
  if (error instanceof Error && error.name) {
    parts.push(`errorName=${error.name}`);
  }
  return parts.join(' ');
}
