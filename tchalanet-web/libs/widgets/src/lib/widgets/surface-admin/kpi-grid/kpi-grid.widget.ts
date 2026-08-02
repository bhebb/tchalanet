import { CurrencyPipe, DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

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
  readonly tone?: string;
  readonly route?: string;
  readonly currencyCode?: string;
  /** Raw config value: a literal, or a `{ source:'dynamic', path }` binding into the payload. */
  readonly value?: unknown;
  readonly delta?: unknown;
  readonly deltaPercent?: unknown;
}

@Component({
  selector: 'tch-kpi-grid-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, LabelPipe, NgTemplateOutlet, RouterLink],
  templateUrl: './kpi-grid.widget.html',
  styleUrl: './kpi-grid.widget.scss',
})
export class KpiGridWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly variant = computed(() => stringProp(this.config(), 'variant') ?? '');

  readonly items = computed<readonly KpiItem[]>(() => {
    const raw = this.config()?.props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(item => ({
      id: stringValue(item['id']) ?? '',
      labelKey: stringValue(item['labelKey']) ?? '',
      icon: stringValue(item['icon']),
      tone: stringValue(item['tone']),
      route: stringValue(item['route']),
      currencyCode: stringValue(item['currencyCode']),
      value: item['value'],
      delta: item['delta'],
      deltaPercent: item['deltaPercent'],
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

  resolvedDelta(item: KpiItem): number | undefined {
    return this.numberValue(resolveBinding(item.delta, this.dynamic()));
  }

  resolvedDeltaPercent(item: KpiItem): number | undefined {
    return this.numberValue(resolveBinding(item.deltaPercent, this.dynamic()));
  }

  isNumericValue(value: string | number | undefined): value is number {
    return typeof value === 'number' && Number.isFinite(value);
  }

  visualTone(item: KpiItem, index: number): string {
    const tone = item.tone ?? (index === 0 ? 'primary' : '');
    const value = this.resolvedValue(item);
    if ((tone === 'danger' || tone === 'warning') && isZeroValue(value)) {
      return 'neutral';
    }
    return tone;
  }

  private dynamicValue(id: string): string | number | undefined {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return undefined;
    const val = dyn[id];
    return typeof val === 'string' || typeof val === 'number' ? val : undefined;
  }

  private numberValue(value: unknown): number | undefined {
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'string') {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : undefined;
    }
    return undefined;
  }
}

function isZeroValue(value: string | number | undefined): boolean {
  if (typeof value === 'number') return value === 0;
  return typeof value === 'string' && value.trim() !== '' && Number(value) === 0;
}
