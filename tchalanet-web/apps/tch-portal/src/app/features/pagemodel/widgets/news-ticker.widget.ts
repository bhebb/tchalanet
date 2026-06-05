import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { stringProp } from '../widget.contract';

interface NewsItem {
  readonly id?: string;
  readonly title?: string;
  readonly title_key?: string;
  readonly date?: string;
}

interface NewsDynamic {
  readonly items?: readonly NewsItem[];
}

/** `NewsTickerWidget`: list of latest news from the `public_home` dynamic source (`{ items }`). */
@Component({
  selector: 'tch-news-ticker-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="news">
      <h2 class="news__title">{{ titleKey() | tchLabel }}</h2>
      @if (items().length) {
        <ul class="news__list">
          @for (item of items(); track item.id ?? $index) {
            <li class="news__item">
              <span class="news__label">{{ item.title ?? (item.title_key | tchLabel) }}</span>
              @if (item.date) {
                <time class="news__date">{{ item.date }}</time>
              }
            </li>
          }
        </ul>
      } @else {
        <p class="news__empty">{{ 'home.news.empty' | tchLabel }}</p>
      }
    </section>
  `,
  styles: [
    `
      .news {
        display: grid;
        gap: 0.75rem;
        padding: 1.5rem;
        border-radius: var(--tch-radius-control, 12px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
      }
      .news__title {
        margin: 0;
        font-size: 1.25rem;
      }
      .news__list {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 0.5rem;
      }
      .news__item {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        padding-bottom: 0.5rem;
        border-bottom: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
      }
      .news__date {
        opacity: 0.7;
        font-variant-numeric: tabular-nums;
      }
    `,
  ],
})
export class NewsTickerWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(
    () => stringProp(this.config(), 'title_key') ?? 'home.news.title',
  );
  readonly items = computed<readonly NewsItem[]>(
    () => (this.dynamic() as NewsDynamic)?.items ?? [],
  );
}
