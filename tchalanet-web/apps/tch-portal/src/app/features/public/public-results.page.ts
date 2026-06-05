import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { PublicShellComponent } from './shell/public-shell.component';
import { ResultStatus } from '../pagemodel/widget.contract';

type ResultFilter = 'all' | 'new-york' | 'florida' | 'georgia';

export interface PublicResultListItem {
  readonly id: string;
  readonly gameName: string;
  readonly sourceKey: ResultFilter;
  readonly drawDateKey: string;
  readonly drawTime: string;
  readonly status: ResultStatus;
  readonly numbers: readonly string[];
  readonly accent?: string;
}

const RESULT_ITEMS: readonly PublicResultListItem[] = [
  {
    id: 'ny-afternoon',
    gameName: 'New York Afternoon',
    sourceKey: 'new-york',
    drawDateKey: 'public.results.fallback.draw_date',
    drawTime: '14:30',
    status: 'CONFIRMED',
    numbers: ['84', '12', '99', '24'],
    accent: '24',
  },
  {
    id: 'florida-evening',
    gameName: 'Florida Evening',
    sourceKey: 'florida',
    drawDateKey: 'public.results.list.today',
    drawTime: '18:45',
    status: 'PENDING',
    numbers: [],
  },
  {
    id: 'georgia-midday',
    gameName: 'Georgia Midday',
    sourceKey: 'georgia',
    drawDateKey: 'public.results.list.today',
    drawTime: '12:20',
    status: 'CONFIRMED',
    numbers: ['05', '47', '31', '08'],
    accent: '08',
  },
  {
    id: 'ny-evening',
    gameName: 'New York Evening',
    sourceKey: 'new-york',
    drawDateKey: 'public.results.list.yesterday',
    drawTime: '20:30',
    status: 'UNAVAILABLE',
    numbers: [],
  },
];

@Component({
  selector: 'tch-public-results-page',
  imports: [PublicShellComponent, RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tch-page-shell>
      <main class="results-page">
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

        <section class="results-page__filters" [attr.aria-label]="'public.results.filters.aria' | translate">
          @for (filter of filters; track filter.id) {
            <button
              class="results-page__filter"
              type="button"
              [class.is-selected]="activeFilter() === filter.id"
              (click)="activeFilter.set(filter.id)"
            >
              {{ filter.labelKey | translate }}
            </button>
          }
          <button class="results-page__filter-icon" type="button" [attr.aria-label]="'public.results.filters.more' | translate">
            <span class="material-symbols-outlined">tune</span>
          </button>
        </section>

        <section class="results-page__feed" aria-labelledby="results-feed-title">
          <div class="results-page__heading">
            <div>
              <h2 id="results-feed-title">{{ 'public.results.list.title' | translate }}</h2>
              <p>{{ 'public.results.list.updated' | translate }}</p>
            </div>
            <a class="results-page__history" routerLink="/public/results/ny-afternoon">
              <span class="material-symbols-outlined">history</span>
              <span>{{ 'public.results.list.history' | translate }}</span>
            </a>
          </div>

          @if (visibleResults().length) {
            <div class="results-page__grid">
              @for (result of visibleResults(); track result.id) {
                <article class="results-page__card">
                  <div class="results-page__card-top">
                    <div>
                      <div class="results-page__title-row">
                        <span class="results-page__dot" [attr.data-status]="result.status"></span>
                        <h3>{{ result.gameName }}</h3>
                      </div>
                      <p>{{ result.drawDateKey | translate }} · {{ result.drawTime }}</p>
                    </div>
                    <span class="results-page__status" [attr.data-status]="result.status">
                      {{ statusLabel(result.status) | translate }}
                    </span>
                  </div>

                  @if (result.status === 'CONFIRMED') {
                    <div class="results-page__numbers">
                      <div class="results-page__number-group">
                        <span class="results-page__number-label">{{ 'public.results.list.numbers_label' | translate }}</span>
                        <div class="results-page__balls">
                          @for (number of result.numbers.slice(0, 3); track $index) {
                            <span class="results-page__ball">{{ number }}</span>
                          }
                        </div>
                      </div>
                      @if (result.accent) {
                        <div class="results-page__accent">
                          <span>{{ 'public.results.list.accent_label' | translate }}</span>
                          <strong>{{ result.accent }}</strong>
                        </div>
                      }
                    </div>
                  } @else if (result.status === 'PENDING') {
                    <div class="results-page__pending">
                      <span class="material-symbols-outlined">hourglass_empty</span>
                      <span>{{ 'public.results.list.pending_body' | translate }}</span>
                    </div>
                  } @else {
                    <div class="results-page__pending">
                      <span class="material-symbols-outlined">cloud_off</span>
                      <span>{{ 'public.results.list.unavailable_body' | translate }}</span>
                    </div>
                  }

                  <a class="results-page__detail" [routerLink]="['/public/results', result.id]">
                    <span>{{ 'public.results.detail' | translate }}</span>
                    <span class="material-symbols-outlined">arrow_forward</span>
                  </a>
                </article>
              }
            </div>
          } @else {
            <div class="results-page__empty">
              <span class="material-symbols-outlined">search_off</span>
              <h3>{{ 'public.results.list.empty_title' | translate }}</h3>
              <p>{{ 'public.results.list.empty_body' | translate }}</p>
              <button type="button" (click)="activeFilter.set('all')">
                {{ 'public.results.list.clear_filters' | translate }}
              </button>
            </div>
          }
        </section>
      </main>
    </tch-page-shell>
  `,
  styles: [
    `
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

      .results-page__hero-image {
        object-fit: cover;
      }

      .results-page__hero-shade {
        background:
          linear-gradient(
            180deg,
            transparent,
            color-mix(in oklab, var(--tch-color-background, var(--mat-sys-background)) 92%, transparent)
          ),
          linear-gradient(
            90deg,
            color-mix(in oklab, var(--tch-color-background, var(--mat-sys-background)) 86%, transparent),
            transparent
          );
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
      .results-page__hero-copy p,
      .results-page__heading h2,
      .results-page__heading p,
      .results-page__card h3,
      .results-page__card p,
      .results-page__empty h3,
      .results-page__empty p {
        margin: 0;
      }

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

      .results-page__hero-copy p,
      .results-page__heading p,
      .results-page__card-top p,
      .results-page__number-label,
      .results-page__accent span,
      .results-page__empty p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .results-page__filters {
        display: flex;
        gap: 0.5rem;
        overflow-x: auto;
        padding: 0.25rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        box-shadow: var(--mat-sys-level1, none);
        scrollbar-width: none;
      }

      .results-page__filters::-webkit-scrollbar {
        display: none;
      }

      .results-page__filter,
      .results-page__filter-icon {
        flex: 0 0 auto;
        min-height: var(--tch-touch-target, 48px);
        border: 0;
        border-radius: var(--tch-radius-control, 8px);
        font-weight: 800;
      }

      .results-page__filter {
        padding: 0 1rem;
        background: var(--tch-color-surface-container-high, var(--mat-sys-surface-container-high));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .results-page__filter.is-selected {
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .results-page__filter-icon {
        display: inline-grid;
        place-items: center;
        width: var(--tch-touch-target, 48px);
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container-low));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .results-page__feed {
        display: grid;
        gap: 1rem;
      }

      .results-page__heading {
        display: flex;
        align-items: end;
        justify-content: space-between;
        gap: 1rem;
      }

      .results-page__heading h2 {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-title-md, 1.125rem);
        line-height: var(--tch-line-height-title-md, 1.5rem);
      }

      .results-page__history,
      .results-page__detail {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-weight: 800;
        text-decoration: none;
      }

      .results-page__history {
        display: inline-flex;
        align-items: center;
        gap: 0.375rem;
        min-height: var(--tch-touch-target, 48px);
      }

      .results-page__grid {
        display: grid;
        gap: 0.875rem;
      }

      .results-page__card {
        display: grid;
        gap: 1rem;
        padding: 1rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }

      .results-page__card-top {
        display: flex;
        align-items: start;
        justify-content: space-between;
        gap: 0.75rem;
      }

      .results-page__title-row {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .results-page__title-row h3 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-title-md, 1.125rem);
        line-height: var(--tch-line-height-title-md, 1.5rem);
      }

      .results-page__dot {
        width: 0.5rem;
        height: 0.5rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-status-missing, var(--mat-sys-outline));
      }

      .results-page__dot[data-status='CONFIRMED'] {
        background: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .results-page__dot[data-status='PENDING'] {
        background: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .results-page__status {
        flex: 0 0 auto;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.625rem;
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-status-missing, var(--mat-sys-outline));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .results-page__status[data-status='CONFIRMED'] {
        color: var(--tch-color-status-ready, var(--mat-sys-tertiary));
      }

      .results-page__status[data-status='PENDING'] {
        color: var(--tch-color-status-warning, var(--mat-sys-secondary));
      }

      .results-page__numbers {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        padding: 1rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container-low));
      }

      .results-page__number-group {
        display: grid;
        gap: 0.5rem;
      }

      .results-page__number-label,
      .results-page__accent span {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }

      .results-page__balls {
        display: flex;
        gap: 0.5rem;
      }

      .results-page__ball {
        display: grid;
        place-items: center;
        width: 2.5rem;
        height: 2.5rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-family: var(--tch-font-family-mono, monospace);
        font-weight: 800;
      }

      .results-page__accent {
        display: grid;
        gap: 0.25rem;
        text-align: right;
      }

      .results-page__accent strong {
        color: var(--tch-color-secondary, var(--mat-sys-secondary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 1.75rem;
      }

      .results-page__pending {
        display: grid;
        justify-items: center;
        gap: 0.5rem;
        padding: 1.5rem;
        border: 2px dashed var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-align: center;
      }

      .results-page__pending .material-symbols-outlined {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: 2rem;
      }

      .results-page__detail {
        display: inline-flex;
        align-items: center;
        justify-content: space-between;
        min-height: var(--tch-touch-target, 48px);
        border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

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

      .results-page__empty .material-symbols-outlined {
        color: var(--tch-color-status-missing, var(--mat-sys-outline));
        font-size: 3rem;
      }

      .results-page__empty h3 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .results-page__empty button {
        min-height: var(--tch-touch-target, 48px);
        border: 0;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-weight: 800;
        padding: 0 1rem;
      }

      @media (min-width: 760px) {
        .results-page {
          gap: 1.5rem;
          padding: 2rem 0;
        }

        .results-page__hero {
          min-height: 17rem;
        }

        .results-page__hero-copy {
          max-width: 36rem;
          padding: 2rem;
        }

        .results-page__hero-copy h1 {
          font-size: var(--tch-font-size-headline-lg, 2rem);
          line-height: var(--tch-line-height-headline-lg, 2.5rem);
        }

        .results-page__filters {
          justify-self: start;
        }

        .results-page__grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (min-width: 1120px) {
        .results-page__grid {
          grid-template-columns: repeat(3, minmax(0, 1fr));
        }
      }
    `,
  ],
})
export class PublicResultsPage {
  readonly filters: readonly { readonly id: ResultFilter; readonly labelKey: string }[] = [
    { id: 'all', labelKey: 'public.results.filters.all' },
    { id: 'new-york', labelKey: 'public.results.filters.new_york' },
    { id: 'florida', labelKey: 'public.results.filters.florida' },
    { id: 'georgia', labelKey: 'public.results.filters.georgia' },
  ];

  readonly activeFilter = signal<ResultFilter>('all');
  readonly visibleResults = computed(() => filterResults(RESULT_ITEMS, this.activeFilter()));

  statusLabel(status: ResultStatus): string {
    return `public.results.status.${status}`;
  }
}

export function filterResults(
  results: readonly PublicResultListItem[],
  filter: ResultFilter,
): readonly PublicResultListItem[] {
  return filter === 'all' ? results : results.filter(result => result.sourceKey === filter);
}
