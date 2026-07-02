import { Signal, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { WebErrorSurface } from '@tch/api';
import { TchErrorViewModel, presentApiError } from '@tch/web/errors';

import { unwrapResourceError } from './resource-error-vm';

const GLOBAL_KEY = '__tch_mutation__';

export interface TchMutationContext {
  /** Fournie quand `idempotency` est configuré — à passer à l'endpoint (header/champ). */
  readonly idempotencyKey?: string;
}

export interface TchMutationFeedback {
  readonly kind: 'success' | 'error';
  /** Renseigné en erreur ; le message de succès reste au choix de la page. */
  readonly vm: TchErrorViewModel | null;
}

export interface TchMutationOptions<TInput, TResult> {
  readonly run: (input: TInput, ctx: TchMutationContext) => Observable<TResult>;
  /** Source d'erreur stable, ex. `admin.setup.locale`. */
  readonly source: string;
  readonly surface?: WebErrorSurface;
  /** Typiquement : recharger le resource concerné. */
  readonly onSuccess?: (result: TResult, input: TInput) => void;
  /**
   * Traitement d'erreur spécifique (ex. mapping des erreurs serveur par champ
   * via `applyServerFieldErrors`). Renvoyer `true` = erreur entièrement gérée,
   * aucun feedback générique n'est émis ; sinon le feedback erreur standard s'applique.
   */
  readonly onError?: (err: unknown, input: TInput) => boolean | void;
  /** Pour les endpoints idempotents (vente, reset PIN, résultat, override). */
  readonly idempotency?: { readonly keyFactory?: () => string };
}

/**
 * État d'écriture d'une page : pending (global ou par clé de ligne), feedback
 * succès/erreur, double-submit bloqué localement. Les invariants (audit,
 * permissions, rejeu/conflit d'idempotence, transitions) restent serveur.
 *
 * À créer dans un contexte d'injection (initialiseur de champ de page).
 */
export class TchMutation<TInput, TResult> {
  private readonly pendingKeys = signal<ReadonlySet<string>>(new Set());
  private readonly feedbackState = signal<TchMutationFeedback | null>(null);

  readonly feedback: Signal<TchMutationFeedback | null> = this.feedbackState.asReadonly();

  constructor(
    private readonly options: TchMutationOptions<TInput, TResult>,
    private readonly translate: (key: string) => string,
  ) {}

  /** Sans argument : une exécution est-elle en cours ? Avec clé : cette ligne est-elle en cours ? */
  pending(key?: string): boolean {
    const keys = this.pendingKeys();
    return key === undefined ? keys.size > 0 : keys.has(key);
  }

  execute(input: TInput, opts?: { readonly key?: string }): void {
    const key = opts?.key ?? GLOBAL_KEY;
    if (this.pendingKeys().has(key)) return; // double-submit : no-op

    const ctx: TchMutationContext = this.options.idempotency
      ? { idempotencyKey: (this.options.idempotency.keyFactory ?? randomKey)() }
      : {};

    this.feedbackState.set(null);
    this.pendingKeys.update(keys => new Set(keys).add(key));

    this.options.run(input, ctx).subscribe({
      next: result => {
        this.release(key);
        this.feedbackState.set({ kind: 'success', vm: null });
        this.options.onSuccess?.(result, input);
      },
      error: (err: unknown) => {
        this.release(key);
        if (this.options.onError?.(err, input) === true) return;
        this.feedbackState.set({
          kind: 'error',
          vm: presentApiError(unwrapResourceError(err), this.translate, {
            source: this.options.source,
            surface: this.options.surface ?? 'section',
          }).viewModel,
        });
      },
    });
  }

  clearFeedback(): void {
    this.feedbackState.set(null);
  }

  private release(key: string): void {
    this.pendingKeys.update(keys => {
      const next = new Set(keys);
      next.delete(key);
      return next;
    });
  }
}

export function tchMutation<TInput, TResult>(
  options: TchMutationOptions<TInput, TResult>,
): TchMutation<TInput, TResult> {
  const translate = inject(TranslateService);
  return new TchMutation(options, key => translate.instant(key));
}

function randomKey(): string {
  return crypto.randomUUID();
}
