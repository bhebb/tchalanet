import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import {
  LabelPipe,
  WidgetConfig,
  isRecord,
  resolveBinding,
  stringProp,
  stringValue,
} from '@tch/page-model';

interface KpiItem {
  readonly id: string;
  readonly labelKey: string;
  readonly icon?: string;
  /** Raw config value: a literal, or a `{ source:'dynamic', path }` binding into the payload. */
  readonly value?: unknown;
}

@Component({
  selector: 'tch-kpi-grid-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe],
  templateUrl: './kpi-grid.widget.html',
  styleUrl: './kpi-grid.widget.scss',
})
export class KpiGridWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly items = computed<readonly KpiItem[]>(() => {
    const raw = this.config()?.props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(item => ({
      id: stringValue(item['id']) ?? '',
      labelKey: stringValue(item['labelKey']) ?? '',
      icon: stringValue(item['icon']),
      value: item['value'],
    }));
  });

  /**
   * Resolve a KPI value: a declared `{ source:'dynamic', path }` binding wins; otherwise fall back
   * to the legacy flat lookup `dynamic.widgets[id][itemId]` for backward compatibility.
   */
  resolvedValue(item: KpiItem): string | number | undefined {
    const bound = resolveBinding(item.value, this.dynamic());
    if (typeof bound === 'string' || typeof bound === 'number') {
      return bound;
    }
    return this.dynamicValue(item.id);
  }

  private dynamicValue(id: string): string | number | undefined {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return undefined;
    const val = dyn[id];
    return typeof val === 'string' || typeof val === 'number' ? val : undefined;
  }
}
