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
  PublicDrawResultSlot,
  PublicDrawResultsService,
} from './public-draw-results.service';
import { PAGE_SIZE_OPTIONS } from './public-results.model';
import type { PageSizeOption, ProviderKey, SlotTypeKey } from './public-results.model';
import { providerKey, providerLabel, resolveSlotKeys, slotTypeKey } from './public-results.utils';

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

  readonly slotsResult = rxResource({
    stream: (): Observable<{ items: readonly PublicDrawResultSlot[] }> => this.svc.slots(),
  });

  readonly activeSlots = computed(() => this.slotsResult.value()?.items ?? []);

  readonly slotKeys = computed(() => resolveSlotKeys(
    this.activeSlots(),
    this.providerFilter(),
    this.slotTypeFilter(),
  ));

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
   * - CONFIRMED/OVERRIDDEN/ERROR/PROVISIONAL → shared result domain statuses
   */
  statusLabel(row: PublicDrawResultRow): string {
    if (!row.numbers.length) return 'public.results.awaiting';
    switch (row.status) {
      case 'CONFIRMED':
        return 'domain.result.status.CONFIRMED';
      case 'OVERRIDDEN':
        return 'domain.result.status.OVERRIDDEN';
      case 'ERROR':
        return 'domain.result.status.ERROR';
      default:
        return 'domain.result.status.PROVISIONAL';
    }
  }

  /** Strips seconds from a time string: "14:30:00" → "14:30". */
  hhmm(time: string | undefined): string {
    if (!time) return '';
    return time.length > 5 ? time.substring(0, 5) : time;
  }

  // ── Static filter definitions ───────────────────────────────────────────────

  readonly providerFilters = computed<readonly { readonly id: ProviderKey; readonly label: string }[]>(() => {
    const providers = new Map<string, string>();
    for (const slot of this.activeSlots()) {
      const id = providerKey(slot.provider);
      if (id) providers.set(id, providerLabel(slot.provider));
    }
    return [
      { id: 'all', label: 'Tous' },
      ...Array.from(providers.entries())
        .sort((a, b) => a[1].localeCompare(b[1]))
        .map(([id, label]) => ({ id, label })),
    ];
  });

  readonly slotTypeFilters = computed<readonly { readonly id: SlotTypeKey; readonly labelKey: string }[]>(() => {
    const types = new Set<SlotTypeKey>(['all']);
    for (const slot of this.activeSlots()) {
      types.add(slotTypeKey(slot));
    }
    const ordered: readonly SlotTypeKey[] = ['all', 'mid', 'eve', 'late'];
    const labels: Record<SlotTypeKey, string> = {
      all: 'common.all',
      mid: 'domain.draw.slotType.mid',
      eve: 'domain.draw.slotType.eve',
      late: 'domain.draw.slotType.late',
    };
    return ordered.filter(id => types.has(id)).map(id => ({ id, labelKey: labels[id] }));
  });
}
