import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  contentChild,
  input,
  linkedSignal,
  output,
} from '@angular/core';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { TchErrorViewModel } from '@tch/web/errors';

import { delayedVisibility } from '../delayed-visibility';
import { TchAsyncReadyContext, TchAsyncReadyDirective } from './tch-async-ready.directive';

/** Surface minimale d'un `ResourceRef` consommée par le composant (lecture seule). */
export interface TchAsyncSource<T> {
  readonly status: () => string;
  readonly value: () => T | undefined;
  readonly error: () => unknown;
}

type ViewState = 'blank' | 'loading' | 'error' | 'error-stale' | 'empty' | 'ready';

/**
 * États de template d'un resource : loading (anti-flash), erreur (+retry, +ids
 * de corrélation), vide (slot `[empty]`), ready (`ng-template tchAsyncReady`).
 * Lecture seule — les mutations (pending bouton/ligne) restent hors du composant.
 * Jamais de blanchiment : en rechargement, la dernière valeur reste affichée
 * sous une barre de progression discrète.
 */
@Component({
  selector: 'tch-async-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgTemplateOutlet, TchLoading, TchErrorPanel, TchSectionError],
  templateUrl: './tch-async-view.component.html',
  styleUrls: ['./tch-async-view.component.scss'],
})
export class TchAsyncViewComponent<T = unknown> {
  readonly resource = input.required<TchAsyncSource<T>>();
  readonly error = input<TchErrorViewModel | null>(null);
  readonly isEmpty = input<(value: T) => boolean>(defaultIsEmpty);
  readonly loadingLabel = input<string>('');
  readonly retryLabel = input('Réessayer');
  readonly errorTitle = input('Une erreur est survenue');
  readonly retry = output<void>();

  private readonly readyTpl = contentChild(TchAsyncReadyDirective);

  private readonly status = computed(() => this.resource().status());

  private readonly currentValue = computed<T | undefined>(() => {
    const st = this.status();
    // value() lance l'erreur en état 'error' — ne lire que quand elle est valide.
    return st === 'resolved' || st === 'local' || st === 'reloading'
      ? this.resource().value()
      : undefined;
  });

  /** Dernière valeur résolue — permet l'affichage stale (reload, erreur au reload). */
  private readonly lastValue = linkedSignal<
    { st: string; v: T | undefined },
    T | undefined
  >({
    source: () => ({ st: this.status(), v: this.currentValue() }),
    computation: (src, prev) =>
      src.st === 'resolved' || src.st === 'local' ? src.v : prev?.value,
  });

  private readonly displayValue = computed(() => {
    // lire lastValue inconditionnellement : le linkedSignal doit être évalué à
    // chaque rendu pour mémoriser son `previous` (sinon perdu au 1er reload)
    const last = this.lastValue();
    const current = this.currentValue();
    return current !== undefined ? current : last;
  });
  private readonly hasStale = computed(() => this.lastValue() !== undefined);

  private readonly initialLoadingVisible = delayedVisibility(
    () => this.status() === 'loading' && !this.hasStale(),
  );
  protected readonly reloadingVisible = delayedVisibility(
    () => this.status() === 'reloading' || (this.status() === 'loading' && this.hasStale()),
  );

  protected readonly viewState = computed<ViewState>(() => {
    const st = this.status();
    if (st === 'error') return this.hasStale() ? 'error-stale' : 'error';
    if (st === 'idle') return 'blank';
    if (st === 'loading' && !this.hasStale()) {
      return this.initialLoadingVisible() ? 'loading' : 'blank';
    }
    const value = this.displayValue();
    if (value === undefined || this.isEmpty()(value)) return 'empty';
    return 'ready';
  });

  protected readonly readyTemplate = computed(() => this.readyTpl()?.templateRef ?? null);
  protected readonly readyContext = computed<TchAsyncReadyContext<T | undefined>>(() => ({
    $implicit: this.displayValue(),
  }));

  protected readonly correlationId = computed(() => {
    const vm = this.error();
    return vm?.traceId ?? vm?.requestId ?? vm?.errorId ?? null;
  });
}

function defaultIsEmpty(value: unknown): boolean {
  if (value === null || value === undefined) return true;
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === 'object' && 'items' in value) {
    const items = (value as { items: unknown }).items;
    return Array.isArray(items) && items.length === 0;
  }
  return false;
}
