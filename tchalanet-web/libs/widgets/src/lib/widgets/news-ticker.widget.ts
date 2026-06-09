import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton, TchCard } from '@tch/ui/components';

interface NewsItem {
  readonly id?: string;
  readonly title?: string;
  readonly title_key?: string;
  readonly date?: string;
  readonly source?: string;
}

interface NewsDynamic {
  readonly items?: readonly NewsItem[];
}

/** `NewsTickerWidget`: list of latest news from the `public_home` dynamic source (`{ items }`). */
@Component({
  selector: 'tch-news-ticker-widget',
  imports: [LabelPipe, TchCard, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tch-card class="news">
      <div class="news__header">
        <h2 class="news__title">{{ titleKey() | tchLabel }}</h2>
        @if (subtitleKey(); as sk) {
          <p class="news__subtitle">{{ sk | tchLabel }}</p>
        }
      </div>

      @if (items().length) {
        <ul class="news__list" role="list">
          @for (item of items(); track item.id ?? $index) {
            <li class="news__item">
              <span class="news__label">{{ item.title ?? (item.title_key | tchLabel) }}</span>
              <span class="news__meta">
                @if (item.source) {
                  <span class="news__source">{{ item.source }}</span>
                }
                @if (item.date) {
                  <time class="news__date">{{ item.date }}</time>
                }
              </span>
            </li>
          }
        </ul>
        <div class="news__footer">
          @if (externalSourceNoteKey(); as nk) {
            <p class="news__source-note">{{ nk | tchLabel }}</p>
          }
          @if (seeAllAction(); as action) {
            <a tch-action variant="tertiary" class="news__see-all" [attr.href]="href(action)">
              {{ labelKey(action) | tchLabel }}
            </a>
          }
        </div>
      } @else {
        <div class="news__empty-state">
          <p class="news__empty-body">{{ 'home.news.empty_body' | tchLabel }}</p>
          @if (seeAllAction(); as action) {
            <a tch-action variant="tertiary" class="news__see-all" [attr.href]="href(action)">
              {{ labelKey(action) | tchLabel }}
            </a>
          }
          @if (externalSourceNoteKey(); as nk) {
            <p class="news__source-note">{{ nk | tchLabel }}</p>
          }
        </div>
      }
    </tch-card>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      /* Expand TchCard defaults */
      tch-card.news {
        display: grid;
        gap: 1rem;
        padding: 1.25rem;
        border-radius: var(--tch-radius-xl, 24px);

        @include bp.up(medium) {
          padding: 1.5rem;
          gap: 1.25rem;
        }

        @include bp.up(expanded) {
          grid-template-columns: 1fr 1fr;
          grid-template-rows: auto 1fr auto;
          column-gap: 2rem;
        }
      }

      /* ── Header ── */

      .news__header {
        display: grid;
        gap: 0.25rem;

        @include bp.up(expanded) {
          grid-column: 1 / -1;
        }
      }

      .news__title {
        margin: 0;
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .news__subtitle {
        margin: 0;
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      /* ── List ── */

      .news__list {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 0;

        @include bp.up(expanded) {
          grid-column: 1 / -1;
          grid-template-columns: 1fr 1fr;
          column-gap: 2rem;
        }
      }

      .news__item {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        gap: 0.75rem;
        padding: 0.625rem 0;
        border-bottom: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));

        &:last-child {
          border-bottom: none;
        }
      }

      .news__label {
        flex: 1;
        min-width: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        @include tch.text-ellipsis(2);
      }

      .news__meta {
        display: flex;
        gap: 0.35rem;
        flex-shrink: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        white-space: nowrap;
      }

      .news__source::after {
        content: ' ·';
      }

      /* ── Empty state ── */

      .news__empty-state {
        display: grid;
        gap: 0.75rem;
        justify-items: start;
      }

      .news__empty-body {
        margin: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
      }

      /* ── Footer (only shown with items) ── */

      .news__footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        flex-wrap: wrap;

        @include bp.up(expanded) {
          grid-column: 1 / -1;
        }
      }

      .news__source-note {
        margin: 0;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
        opacity: 0.7;
      }

      /* Compact see-all button */
      a.news__see-all {
        min-height: 36px;
        padding: 0 0.875rem;
        font-size: var(--tch-font-size-label-sm, 0.875rem);
        white-space: nowrap;
      }
    `,
  ],
})
export class NewsTickerWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(
    () => stringProp(this.config(), 'title_key') ?? stringProp(this.config(), 'titleKey') ?? 'home.news.title',
  );
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey'));
  readonly externalSourceNoteKey = computed(() => stringProp(this.config(), 'externalSourceNoteKey'));
  readonly seeAllAction = computed(() => actionFrom(this.config().props?.['seeAllAction']));
  readonly items = computed<readonly NewsItem[]>(() =>
    (this.dynamic() as NewsDynamic)?.items ?? [],
  );

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
