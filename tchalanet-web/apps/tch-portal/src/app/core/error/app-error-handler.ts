import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandler, Injectable, Injector, inject } from '@angular/core';

import { ShellFeedbackStore } from '../../shared/feedback/shell-feedback.store';

interface ProblemLike {
  readonly title?: string;
  readonly detail?: string;
}

/** Always turn an unknown thrown value into a human-readable sentence (never `[object Object]`). */
function errorToMessage(error: unknown): string {
  if (isProblemLike(error)) {
    return error.detail || error.title || 'Une erreur est survenue.';
  }
  if (error instanceof Error) {
    return error.message || 'Une erreur est survenue.';
  }
  if (typeof error === 'string') {
    return error;
  }
  return 'Une erreur inattendue est survenue.';
}

function isProblemLike(value: unknown): value is ProblemLike {
  if (typeof value !== 'object' || value === null) return false;
  const v = value as Record<string, unknown>;
  return typeof v['detail'] === 'string' || typeof v['title'] === 'string';
}

/** Angular wraps some thrown values; reach the original error when present. */
function unwrap(error: unknown): unknown {
  if (typeof error === 'object' && error !== null) {
    const v = error as Record<string, unknown>;
    if (v['ngOriginalError']) return v['ngOriginalError'];
    if (v['rejection']) return v['rejection'];
  }
  return error;
}

@Injectable()
export class AppErrorHandler implements ErrorHandler {
  private readonly injector = inject(Injector);

  handleError(error: unknown): void {
    const original = unwrap(error);
    console.error('[AppError]', original);

    // HTTP errors are already surfaced — with ProblemDetail, trace id and copy — by
    // apiFeedbackInterceptor. Re-reporting them here produced a duplicate, technical
    // "Erreur inattendue [object Object]" banner. Skip them.
    if (original instanceof HttpErrorResponse) return;

    const store = this.injector.get(ShellFeedbackStore, null);
    if (!store) return;

    const message = errorToMessage(original);

    let ctx = '';
    try {
      const stack = original instanceof Error ? original.stack?.slice(0, 800) : undefined;
      const payload = JSON.stringify({ message, stack });
      ctx = btoa(unescape(encodeURIComponent(payload)));
    } catch {
      ctx = btoa(unescape(encodeURIComponent(message)));
    }

    store.add({
      severity: 'error',
      title: 'Erreur inattendue',
      message,
      reportUrl: `/public/support?ctx=${encodeURIComponent(ctx)}`,
    });
  }
}
