import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EMPTY, Observable } from 'rxjs';

import {
  PublicEmptyStateComponent,
  PublicFilterBarComponent,
  PublicListShellComponent,
  PublicPaginationBarComponent,
} from '../shared';
import {
  PublicDrawResultHistoryPage,
  PublicDrawResultRow,
  PublicDrawResultsService,
} from './public-draw-results.service';
import { PAGE_SIZE_OPTIONS } from './public-results.model';
import type { PageSizeOption, ProviderKey, SlotTypeKey } from './public-results.model';
import { resolveSlotKeys } from './public-results.utils';

@Component({
  selector: 'tch-public-results-page',
  imports: [
    RouterLink,
    TranslatePipe,
    PublicListShellComponent,
    PublicFilterBarComponent,
    PublicPaginationBarComponent,
    PublicEmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-results.page.html',
  styleUrls: ['./public-results.page.scss'],
})
export class PublicResultsPage {
  private readonly svc = inject(PublicDrawResultsService);

  // ── Filter state ────────────────────────────────────────────────────────────

  readonly providerFilter = signal<ProviderKey>('all');
  readonly slotTypeFilter = signal<SlotTypeKey>('all');
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly currentPage = signal(0);
  readonly pageSize = signal<PageSizeOption>(20);

  readonly slotKeys = computed(() => resolveSlotKeys(this.providerFilter(), this.slotTypeFilter()));

  readonly hasActiveFilters = computed(
    () =>
      this.providerFilter() !== 'all' ||
      this.slotTypeFilter() !== 'all' ||
      !!this.fromDate() ||
      !!this.toDate(),
  );

  constructor() {
    // Reset page to 0 whenever filters or page size change
    effect(() => {
      this.providerFilter();
      this.slotTypeFilter();
      this.fromDate();
      this.toDate();
      this.pageSize();
      // Untracked write: prevent the page signal from re-triggering this effect
      untracked(() => this.currentPage.set(0));
    });
  }

  // ── Server resource ─────────────────────────────────────────────────────────

  readonly result = rxResource({
    params: () => ({
      slotKeys: this.slotKeys(),
      from: this.fromDate() || undefined,
      to: this.toDate() || undefined,
      page: this.currentPage(),
      size: this.pageSize(),
    }),
    stream: ({ params }): Observable<PublicDrawResultHistoryPage> => {
      // Guard: empty slotKeys means the filter combo has no matching slots → short-circuit
      if (Array.isArray(params.slotKeys) && params.slotKeys.length === 0) {
        return EMPTY;
      }
      return this.svc.history(params);
    },
  });

  readonly rows = computed<readonly PublicDrawResultRow[]>(() => this.result.value()?.items ?? []);

  readonly totalPages = computed(() => this.result.value()?.totalPages ?? 0);

  readonly totalItems = computed(() => this.result.value()?.totalItems ?? 0);

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;

  // ── Filter actions ──────────────────────────────────────────────────────────

  setProvider(p: ProviderKey): void {
    this.providerFilter.set(p);
  }

  setSlotType(s: SlotTypeKey): void {
    this.slotTypeFilter.set(s);
  }

  setFrom(v: string): void {
    this.fromDate.set(v);
  }

  setTo(v: string): void {
    this.toDate.set(v);
  }

  clearFilters(): void {
    this.providerFilter.set('all');
    this.slotTypeFilter.set('all');
    this.fromDate.set('');
    this.toDate.set('');
  }

  // ── Pagination ──────────────────────────────────────────────────────────────

  setPageSize(n: number): void {
    const valid = PAGE_SIZE_OPTIONS.find(o => o === n);
    if (valid) this.pageSize.set(valid);
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  /**
   * Status label rules aligned with DrawResultStatus enum:
   * - No numbers → "En attente des résultats"
   * - CONFIRMED  → "Résultats confirmés"
   * - OVERRIDDEN → "Résultats corrigés"
   * - ERROR      → "Résultat en erreur"
   * - PROVISIONAL (or unknown) → "En attente de confirmation"
   */
  statusLabel(row: PublicDrawResultRow): string {
    if (!row.numbers.length) return 'public.results.awaiting';
    switch (row.status) {
      case 'CONFIRMED':
        return 'public.results.status.CONFIRMED';
      case 'OVERRIDDEN':
        return 'public.results.status.OVERRIDDEN';
      case 'ERROR':
        return 'public.results.status.ERROR';
      default:
        return 'public.results.status.PROVISIONAL';
    }
  }

  /** Strips seconds from a time string: "14:30:00" → "14:30". */
  hhmm(time: string | undefined): string {
    if (!time) return '';
    return time.length > 5 ? time.substring(0, 5) : time;
  }

  // ── Static filter definitions ───────────────────────────────────────────────

  readonly providerFilters: readonly { readonly id: ProviderKey; readonly labelKey: string }[] = [
    { id: 'all', labelKey: 'public.results.filters.all' },
    { id: 'ny', labelKey: 'public.results.filters.new_york' },
    { id: 'fl', labelKey: 'public.results.filters.florida' },
    { id: 'ga', labelKey: 'public.results.filters.georgia' },
    { id: 'tx', labelKey: 'public.results.filters.texas' },
  ];

  readonly slotTypeFilters: readonly { readonly id: SlotTypeKey; readonly labelKey: string }[] = [
    { id: 'all', labelKey: 'public.results.filters.slot_all' },
    { id: 'mid', labelKey: 'public.results.filters.slot_mid' },
    { id: 'eve', labelKey: 'public.results.filters.slot_eve' },
    { id: 'late', labelKey: 'public.results.filters.slot_late' },
  ];
}
