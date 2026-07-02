import { Signal, computed, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { WebErrorSurface } from '@tch/api';
import { TchErrorViewModel, presentApiError } from '@tch/web/errors';

/** Surface minimale d'un `ResourceRef` consommée par les helpers async. */
export interface TchAsyncErrorSource {
  readonly status: () => string;
  readonly error: () => unknown;
}

/**
 * Erreur d'un resource → `ErrorViewModel` affichable, une seule fois pour tout le web.
 * À créer dans un contexte d'injection (initialiseur de champ de page).
 */
export function resourceErrorVm(
  res: TchAsyncErrorSource,
  source: string,
  surface: WebErrorSurface = 'page',
): Signal<TchErrorViewModel | null> {
  const translate = inject(TranslateService);
  const lookup = (key: string): string => translate.instant(key);

  return computed(() => {
    if (res.status() !== 'error') return null;
    const err = res.error();
    if (err == null) return null;
    return presentApiError(unwrapResourceError(err), lookup, { source, surface }).viewModel;
  });
}

/** Les resources Angular enveloppent l'erreur du stream dans une `Error` (`cause` = origine). */
export function unwrapResourceError(err: unknown): unknown {
  if (err instanceof Error) {
    // cast : les apps compilent encore avec lib es2020 (Error.cause absent du type)
    const cause = (err as Error & { cause?: unknown }).cause;
    if (cause !== undefined && cause !== null) return cause;
  }
  return err;
}
