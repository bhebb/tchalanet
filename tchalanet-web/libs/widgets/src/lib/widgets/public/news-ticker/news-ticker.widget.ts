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
  readonly sourceType?: string;
  readonly sourceUrl?: string;
  readonly publishedAt?: string;
}

interface NewsDynamic {
  readonly items?: readonly NewsItem[];
}

/** `NewsTickerWidget`: list of latest news from the `public_home` dynamic source (`{ items }`). */
@Component({
  selector: 'tch-news-ticker-widget',
  imports: [LabelPipe, TchCard, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './news-ticker.widget.html',
  styleUrl: './news-ticker.widget.scss',
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
  readonly items = computed<readonly NewsItem[]>(() => (this.dynamic() as NewsDynamic)?.items ?? []);

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
