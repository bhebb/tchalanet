import { ErrorHandler, Injectable, Injector, inject } from '@angular/core';

import { ShellFeedbackStore } from '../../shared/feedback/shell-feedback.store';

@Injectable()
export class AppErrorHandler implements ErrorHandler {
  private readonly injector = inject(Injector);

  handleError(error: unknown): void {
    console.error('[AppError]', error);

    const err = error instanceof Error ? error : new Error(String(error));
    const store = this.injector.get(ShellFeedbackStore, null);

    if (!store) return;

    let ctx = '';
    try {
      const payload = JSON.stringify({ message: err.message, stack: err.stack?.slice(0, 800) });
      ctx = btoa(unescape(encodeURIComponent(payload)));
    } catch {
      ctx = btoa(unescape(encodeURIComponent(err.message)));
    }

    store.add({
      severity: 'error',
      title: 'Erreur inattendue',
      message: err.message || 'Une erreur est survenue.',
      reportUrl: `/public/support?ctx=${encodeURIComponent(ctx)}`,
    });
  }
}
