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

import { PublicDrawResultHistoryPage, PublicDrawResultRow, PublicDrawResultsService } from './public-draw-results.service';

// ── Filter types ──────────────────────────────────────────────────────────────

type ProviderKey = 'all' | 'ny' | 'fl' | 'ga' | 'tx';
type SlotTypeKey = 'all' | 'mid' | 'eve' | 'late';

const SLOTS_BY_PROVIDER: Readonly<Record<Exclude<ProviderKey, 'all'>, readonly string[]>> = {
  ny: ['NY_MID', 'NY_EVE'],
  fl: ['FL_MID', 'FL_EVE'],
  ga: ['GA_MID', 'GA_EVE', 'GA_LATE'],
  tx: ['TX_1000', 'TX_1227', 'TX_1800', 'TX_2212'],
};

const SLOT_TYPE_SETS: Readonly<Record<Exclude<SlotTypeKey, 'all'>, ReadonlySet<string>>> = {
  mid:  new Set(['NY_MID', 'FL_MID', 'GA_MID', 'TX_1000', 'TX_1227']),
  eve:  new Set(['NY_EVE', 'FL_EVE', 'GA_EVE', 'TX_1800', 'TX_2212']),
  late: new Set(['GA_LATE']),
};

/** Returns the resolved slotKeys to send as a query param, or undefined = no filter. */
export function resolveSlotKeys(
  provider: ProviderKey,
  slotType: SlotTypeKey,
): readonly string[] | undefined {
  // Collect the base set for the selected provider
  const base: readonly string[] =
    provider === 'all'
      ? Object.values(SLOTS_BY_PROVIDER).flat()
      : SLOTS_BY_PROVIDER[provider];

  // No slot-type filter → use the full provider set (or undefined = "all" if nothing selected)
  if (slotType === 'all') {
    return provider === 'all' ? undefined : base;
  }

  const typeSet = SLOT_TYPE_SETS[slotType];
  const filtered = base.filter(k => typeSet.has(k));
  // Return the filtered list; empty list means "no results exist for this combination"
  return filtered;
}

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;
type PageSizeOption = (typeof PAGE_SIZE_OPTIONS)[number];

@Component({
  selector: 'tch-public-results-page',
  imports: [RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="results-page">

      <!-- ── Hero ── -->
      <section class="results-page__hero" aria-labelledby="results-page-title">
        <img
          class="results-page__hero-image"
          src="/assets/public/results-hero-balls.png"
          alt=""
          aria-hidden="true"
        />
        <div class="results-page__hero-shade" aria-hidden="true"></div>
        <div class="results-page__hero-copy">
          <p class="results-page__eyebrow">{{ 'public.results.detail_eyebrow' | translate }}</p>
          <h1 id="results-page-title">{{ 'public.results.latest_title' | translate }}</h1>
          <p>{{ 'public.results.subtitle' | translate }}</p>
        </div>
      </section>

      <!-- ── Filter panel ── -->
      <section class="results-page__filters" [attr.aria-label]="'public.results.filters.aria' | translate">

        <!-- Provider chips -->
        <div class="results-page__filter-row" role="group" [attr.aria-label]="'public.results.filters.provider' | translate">
          @for (p of providerFilters; track p.id) {
            <button
              type="button"
              class="results-page__chip"
              [class.is-active]="providerFilter() === p.id"
              (click)="setProvider(p.id)"
            >
              {{ p.labelKey | translate }}
            </button>
          }
        </div>

        <!-- Slot type chips -->
        <div class="results-page__filter-row" role="group" [attr.aria-label]="'public.results.filters.slot_type' | translate">
          @for (s of slotTypeFilters; track s.id) {
            <button
              type="button"
              class="results-page__chip"
              [class.is-active]="slotTypeFilter() === s.id"
              (click)="setSlotType(s.id)"
            >
              {{ s.labelKey | translate }}
            </button>
          }
        </div>

        <!-- Date range -->
        <div class="results-page__filter-dates">
          <label class="results-page__date-label">
            <span>{{ 'public.results.filters.from' | translate }}</span>
            <input
              class="results-page__date-input"
              type="date"
              [value]="fromDate()"
              (change)="setFrom($any($event.target).value)"
            />
          </label>
          <label class="results-page__date-label">
            <span>{{ 'public.results.filters.to' | translate }}</span>
            <input
              class="results-page__date-input"
              type="date"
              [value]="toDate()"
              (change)="setTo($any($event.target).value)"
            />
          </label>
          @if (hasActiveFilters()) {
            <button type="button" class="results-page__clear" (click)="clearFilters()">
              {{ 'public.results.list.clear_filters' | translate }}
            </button>
          }
        </div>

      </section>

      <!-- ── Table section ── -->
      <section class="results-page__table-section">

        @if (result.isLoading()) {
          <div class="results-page__loading" aria-live="polite" aria-busy="true">
            <span class="results-page__spinner" aria-hidden="true"></span>
            <span>{{ 'public.results.loading' | translate }}</span>
          </div>
        } @else if (result.error()) {
          <div class="results-page__error" role="alert">
            <span class="material-symbols-outlined">cloud_off</span>
            <p>{{ 'public.results.load_error' | translate }}</p>
            <button type="button" (click)="result.reload()">
              {{ 'public.results.retry' | translate }}
            </button>
          </div>
        } @else if (rows().length) {

          <!-- Desktop table -->
          <div class="results-page__table-wrap">
            <table class="results-page__table">
              <thead>
                <tr>
                  <th scope="col">{{ 'public.results.table.date' | translate }}</th>
                  <th scope="col">{{ 'public.results.table.draw' | translate }}</th>
                  <th scope="col" class="results-page__th--right">{{ 'public.results.table.time' | translate }}</th>
                  <th scope="col">{{ 'public.results.table.status' | translate }}</th>
                  <th scope="col">{{ 'public.results.table.numbers' | translate }}</th>
                  <th scope="col"><span class="visually-hidden">{{ 'public.results.table.action' | translate }}</span></th>
                </tr>
              </thead>
              <tbody>
                @for (row of rows(); track row.drawResultId) {
                  <tr>
                    <td class="results-page__td--date">{{ row.resultDate }}</td>
                    <td class="results-page__td--draw">
                      <span class="results-page__draw-label">{{ row.drawChannelLabelKey | translate }}</span>
                      <span class="results-page__draw-code">{{ row.slotKey }}</span>
                    </td>
                    <td class="results-page__td--right results-page__td--mono">{{ hhmm(row.drawTime) }}</td>
                    <td>
                      <span class="results-page__status-chip" [attr.data-status]="row.status">
                        {{ statusLabel(row) | translate }}
                      </span>
                    </td>
                    <td class="results-page__td--numbers">
                      @if (row.numbers.length) {
                        <div class="results-page__numbers">
                          @for (n of row.numbers; track $index) {
                            <span class="results-page__ball">{{ n }}</span>
                          }
                        </div>
                      } @else {
                        <span class="results-page__numbers-pending">
                          {{ 'public.results.numbers_pending' | translate }}
                        </span>
                      }
                    </td>
                    <td class="results-page__td--action">
                      <a class="results-page__detail-link" [routerLink]="['/public/results', row.drawResultId]">
                        {{ 'public.results.detail' | translate }}
                        <span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
                      </a>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Mobile cards -->
          <div class="results-page__mobile-cards">
            @for (row of rows(); track row.drawResultId) {
              <div class="results-page__card">
                <div class="results-page__card-top">
                  <div>
                    <p class="results-page__card-label">{{ row.drawChannelLabelKey | translate }}</p>
                    <p class="results-page__card-meta">{{ row.resultDate }} · {{ hhmm(row.drawTime) }}</p>
                  </div>
                  <span class="results-page__status-chip" [attr.data-status]="row.status">
                    {{ statusLabel(row) | translate }}
                  </span>
                </div>
                @if (row.numbers.length) {
                  <div class="results-page__numbers">
                    @for (n of row.numbers; track $index) {
                      <span class="results-page__ball">{{ n }}</span>
                    }
                  </div>
                } @else {
                  <p class="results-page__numbers-pending">
                    {{ 'public.results.numbers_pending' | translate }}
                  </p>
                }
                <a class="results-page__detail-link" [routerLink]="['/public/results', row.drawResultId]">
                  {{ 'public.results.detail' | translate }}
                  <span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
                </a>
              </div>
            }
          </div>

          <!-- Pagination bar -->
          <div class="results-page__pagination-bar">
            <!-- Total + page size -->
            <div class="results-page__pagination-meta">
              <span class="results-page__total">
                {{ totalItems() }} {{ 'public.results.pagination.total' | translate }}
              </span>
              <label class="results-page__size-label">
                {{ 'public.results.pagination.per_page' | translate }}
                <select
                  class="results-page__size-select"
                  [value]="pageSize()"
                  (change)="setPageSize(+$any($event.target).value)"
                >
                  @for (opt of pageSizeOptions; track opt) {
                    <option [value]="opt">{{ opt }}</option>
                  }
                </select>
              </label>
            </div>

            <!-- Prev / page info / next -->
            @if (totalPages() > 1) {
              <nav class="results-page__pagination" aria-label="Pagination">
                <button
                  type="button"
                  class="results-page__page-btn"
                  [disabled]="currentPage() === 0"
                  (click)="prevPage()"
                >
                  <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
                  <span>{{ 'public.results.pagination.prev' | translate }}</span>
                </button>
                <span class="results-page__page-info">
                  {{ currentPage() + 1 }} / {{ totalPages() }}
                </span>
                <button
                  type="button"
                  class="results-page__page-btn"
                  [disabled]="currentPage() >= totalPages() - 1"
                  (click)="nextPage()"
                >
                  <span>{{ 'public.results.pagination.next' | translate }}</span>
                  <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
                </button>
              </nav>
            }
          </div>

        } @else {
          <div class="results-page__empty">
            <span class="material-symbols-outlined">search_off</span>
            <h3>{{ 'public.results.list.empty_title' | translate }}</h3>
            <p>{{ 'public.results.list.empty_body' | translate }}</p>
            @if (hasActiveFilters()) {
              <button type="button" (click)="clearFilters()">
                {{ 'public.results.list.clear_filters' | translate }}
              </button>
            }
          </div>
        }

      </section>
    </div>
  `,
  styles: [
    `
      .visually-hidden {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0 0 0 0);
        white-space: nowrap;
        border: 0;
      }

      .results-page {
        display: grid;
        gap: 1rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1160px);
        margin: 0 auto;
        padding: 1rem 0 calc(5rem + var(--tch-page-gutter, 16px));
      }

      .results-page .material-symbols-outlined {
        display: inline-block;
        overflow: hidden;
        max-width: 1em;
        line-height: 1;
        vertical-align: middle;
        white-space: nowrap;
      }

      /* ── Hero ── */

      .results-page__hero {
        position: relative;
        display: grid;
        align-items: end;
        min-height: 13rem;
        overflow: hidden;
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }

      .results-page__hero-image,
      .results-page__hero-shade {
        position: absolute;
        inset: 0;
        width: 100%;
        height: 100%;
      }

      .results-page__hero-image { object-fit: cover; }

      .results-page__hero-shade {
        background:
          linear-gradient(180deg, transparent,
            color-mix(in oklab, var(--tch-color-background, var(--mat-sys-background)) 92%, transparent)),
          linear-gradient(90deg,
            color-mix(in oklab, var(--tch-color-background, var(--mat-sys-background)) 86%, transparent),
            transparent);
      }

      .results-page__hero-copy {
        position: relative;
        z-index: 1;
        display: grid;
        gap: 0.375rem;
        padding: 1rem;
      }

      .results-page__eyebrow,
      .results-page__hero-copy h1,
      .results-page__hero-copy p { margin: 0; }

      .results-page__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }

      .results-page__hero-copy h1 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }

      .results-page__hero-copy p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      /* ── Filter panel ── */

      .results-page__filters {
        display: grid;
        gap: 0.625rem;
        padding: 1rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }

      .results-page__filter-row {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }

      .results-page__chip {
        flex: 0 0 auto;
        padding: 0.375rem 0.875rem;
        min-height: 2.25rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-pill, 9999px);
        background: transparent;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        cursor: pointer;
        transition: background 150ms, color 150ms, border-color 150ms;
      }

      .results-page__chip.is-active {
        background: var(--tch-color-primary, var(--mat-sys-primary));
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .results-page__filter-dates {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 0.75rem;
        padding-top: 0.375rem;
        border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .results-page__date-label {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .results-page__date-input {
        padding: 0.375rem 0.625rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-family: inherit;
      }

      .results-page__clear {
        padding: 0.375rem 0.75rem;
        border: 0;
        border-radius: var(--tch-radius-pill, 9999px);
        background: transparent;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        cursor: pointer;
        text-decoration: underline;
        text-underline-offset: 2px;
      }

      /* ── Table section ── */

      .results-page__table-section {
        display: grid;
        gap: 1rem;
      }

      /* Desktop table */

      .results-page__table-wrap {
        overflow-x: auto;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }

      .results-page__table {
        width: 100%;
        border-collapse: collapse;
        font-size: var(--tch-font-size-body-sm, 0.875rem);
      }

      .results-page__table thead th {
        padding: 0.75rem 1rem;
        border-bottom: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-align: left;
        text-transform: uppercase;
        white-space: nowrap;
      }

      .results-page__th--right { text-align: right; }

      .results-page__table tbody tr {
        border-bottom: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .results-page__table tbody tr:last-child { border-bottom: 0; }

      .results-page__table tbody tr:hover {
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }

      .results-page__table td {
        padding: 0.875rem 1rem;
        vertical-align: middle;
      }

      .results-page__td--date {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        white-space: nowrap;
      }

      .results-page__td--draw {
        display: grid;
        gap: 0.125rem;
        min-width: 11rem;
      }

      .results-page__draw-label {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-weight: 700;
      }

      .results-page__draw-code {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
      }

      .results-page__td--right { text-align: right; }

      .results-page__td--mono {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-variant-numeric: tabular-nums;
        white-space: nowrap;
      }

      /* ── Status chip ── */

      .results-page__status-chip {
        display: inline-block;
        padding: 0.25rem 0.625rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-status-missing, var(--mat-sys-outline));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        white-space: nowrap;
      }

      .results-page__status-chip[data-status='CONFIRMED'],
      .results-page__status-chip[data-status='OVERRIDDEN'] {
        color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .results-page__status-chip[data-status='PROVISIONAL'] {
        color: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .results-page__status-chip[data-status='ERROR'] {
        color: var(--tch-color-status-blocked, var(--mat-sys-error));
      }

      /* ── Numbers ── */

      .results-page__td--numbers { min-width: 8rem; }

      .results-page__numbers {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
        align-items: center;
      }

      .results-page__ball {
        display: grid;
        place-items: center;
        width: 2rem;
        height: 2rem;
        flex-shrink: 0;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 0.75rem;
        font-weight: 800;
      }

      .results-page__numbers-pending {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        margin: 0;
      }

      /* ── Detail link ── */

      .results-page__td--action { text-align: right; }

      .results-page__detail-link {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        min-height: var(--tch-touch-target, 48px);
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-decoration: none;
        white-space: nowrap;
      }

      /* ── Mobile cards (hidden on desktop) ── */

      .results-page__mobile-cards { display: none; }

      .results-page__card {
        display: grid;
        gap: 0.75rem;
        padding: 1rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }

      .results-page__card-top {
        display: flex;
        justify-content: space-between;
        align-items: start;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .results-page__card-top .results-page__status-chip {
        flex-shrink: 0;
        font-size: 0.6875rem; /* slightly smaller on mobile cards */
      }

      .results-page__card-label {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-weight: 700;
        margin: 0;
      }

      .results-page__card-meta {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-variant-numeric: tabular-nums;
        margin: 0;
      }

      /* ── Pagination bar ── */

      .results-page__pagination-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 0.75rem;
      }

      .results-page__pagination-meta {
        display: flex;
        align-items: center;
        gap: 1rem;
        flex-wrap: wrap;
      }

      .results-page__total {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-variant-numeric: tabular-nums;
      }

      .results-page__size-label {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .results-page__size-select {
        padding: 0.25rem 0.5rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-family: inherit;
        cursor: pointer;
      }

      .results-page__pagination {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .results-page__page-btn {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0 0.75rem;
        min-height: var(--tch-touch-target, 48px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        cursor: pointer;
      }

      .results-page__page-btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .results-page__page-info {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-variant-numeric: tabular-nums;
      }

      /* ── Loading / error / empty ── */

      .results-page__loading,
      .results-page__error,
      .results-page__empty {
        display: grid;
        justify-items: center;
        gap: 0.75rem;
        padding: 3rem 1rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        text-align: center;
      }

      .results-page__loading {
        flex-direction: row;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .results-page__spinner {
        width: 1.5rem;
        height: 1.5rem;
        border: 2px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-top-color: var(--tch-color-primary, var(--mat-sys-primary));
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }

      .results-page__error .material-symbols-outlined,
      .results-page__empty .material-symbols-outlined {
        font-size: 2.5rem;
        color: var(--tch-color-status-missing, var(--mat-sys-outline));
      }

      .results-page__error h3,
      .results-page__empty h3,
      .results-page__error p,
      .results-page__empty p { margin: 0; }

      .results-page__error h3,
      .results-page__empty h3 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .results-page__error p,
      .results-page__empty p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .results-page__error button,
      .results-page__empty button {
        padding: 0 1rem;
        min-height: var(--tch-touch-target, 48px);
        border: 0;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-weight: 800;
        cursor: pointer;
      }

      @keyframes spin {
        to { transform: rotate(360deg); }
      }

      /* ── Responsive ── */

      @media (max-width: 679px) {
        .results-page__table-wrap { display: none; }
        .results-page__mobile-cards { display: grid; gap: 0.75rem; }
      }

      @media (min-width: 760px) {
        .results-page {
          gap: 1.5rem;
          padding: 2rem 0;
        }

        .results-page__hero { min-height: 17rem; }

        .results-page__hero-copy {
          max-width: 36rem;
          padding: 2rem;
        }

        .results-page__hero-copy h1 {
          font-size: var(--tch-font-size-headline-lg, 2rem);
          line-height: var(--tch-line-height-headline-lg, 2.5rem);
        }

        .results-page__filter-dates {
          flex-wrap: nowrap;
        }
      }
    `,
  ],
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

  readonly slotKeys = computed(() =>
    resolveSlotKeys(this.providerFilter(), this.slotTypeFilter()),
  );

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

  readonly rows = computed<readonly PublicDrawResultRow[]>(
    () => this.result.value()?.items ?? [],
  );

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

  prevPage(): void {
    this.currentPage.update(p => Math.max(0, p - 1));
  }

  nextPage(): void {
    this.currentPage.update(p => Math.min(this.totalPages() - 1, p + 1));
  }

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
      case 'CONFIRMED':  return 'public.results.status.CONFIRMED';
      case 'OVERRIDDEN': return 'public.results.status.OVERRIDDEN';
      case 'ERROR':      return 'public.results.status.ERROR';
      default:           return 'public.results.status.PROVISIONAL';
    }
  }

  /** Strips seconds from a time string: "14:30:00" → "14:30". */
  hhmm(time: string | undefined): string {
    if (!time) return '';
    return time.length > 5 ? time.substring(0, 5) : time;
  }

  // ── Static filter definitions ───────────────────────────────────────────────

  readonly providerFilters: readonly { readonly id: ProviderKey; readonly labelKey: string }[] = [
    { id: 'all',  labelKey: 'public.results.filters.all' },
    { id: 'ny',   labelKey: 'public.results.filters.new_york' },
    { id: 'fl',   labelKey: 'public.results.filters.florida' },
    { id: 'ga',   labelKey: 'public.results.filters.georgia' },
    { id: 'tx',   labelKey: 'public.results.filters.texas' },
  ];

  readonly slotTypeFilters: readonly { readonly id: SlotTypeKey; readonly labelKey: string }[] = [
    { id: 'all',  labelKey: 'public.results.filters.slot_all' },
    { id: 'mid',  labelKey: 'public.results.filters.slot_mid' },
    { id: 'eve',  labelKey: 'public.results.filters.slot_eve' },
    { id: 'late', labelKey: 'public.results.filters.slot_late' },
  ];
}
