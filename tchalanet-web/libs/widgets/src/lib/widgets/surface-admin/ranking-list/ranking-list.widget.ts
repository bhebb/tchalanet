import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import {
  LabelPipe,
  WidgetConfig,
  isRecord,
  resolveBinding,
  resolvePath,
  stringProp,
  stringValue,
} from '@tch/page-model';

interface RankingMetric {
  readonly labelKey: string;
  readonly path: string;
}

interface RankingItem {
  readonly id: string;
  readonly primary: string;
  readonly secondary?: string;
  readonly metrics: readonly RankingMetric[];
  readonly source: Record<string, unknown>;
}

@Component({
  selector: 'tch-ranking-list-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe],
  templateUrl: './ranking-list.widget.html',
  styleUrl: './ranking-list.widget.scss',
})
export class RankingListWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'common.state.empty');
  readonly primaryPath = computed(() => stringProp(this.config(), 'primaryPath') ?? 'label');
  readonly secondaryPath = computed(() => stringProp(this.config(), 'secondaryPath'));
  readonly itemsSource = computed(() => this.config()?.props?.['items'] ?? { source: 'dynamic', path: 'items' });

  readonly metrics = computed<readonly RankingMetric[]>(() => {
    const raw = this.config()?.props?.['metrics'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(metric => ({
      labelKey: stringValue(metric['labelKey']) ?? '',
      path: stringValue(metric['path']) ?? '',
    })).filter(metric => metric.labelKey && metric.path);
  });

  readonly items = computed<readonly RankingItem[]>(() => {
    const rawItems = resolveBinding(this.itemsSource(), this.dynamic());
    if (!Array.isArray(rawItems)) return [];

    return rawItems.filter(isRecord).map((item, index) => {
      const primary = this.valueAt(item, this.primaryPath()) || `#${index + 1}`;
      const secondaryPath = this.secondaryPath();
      const secondary = secondaryPath ? this.valueAt(item, secondaryPath) : undefined;
      return {
        id: this.valueAt(item, 'id') || primary || String(index),
        primary,
        secondary,
        metrics: this.metrics(),
        source: item,
      };
    });
  });

  metricValue(item: RankingItem, metric: RankingMetric): string {
    return this.valueAt(item.source, metric.path) || '—';
  }

  private valueAt(item: Record<string, unknown>, path: string): string {
    const value = resolvePath(item, path);
    if (typeof value === 'number') {
      return new Intl.NumberFormat().format(value);
    }
    if (typeof value === 'string') {
      return value;
    }
    return '';
  }
}
