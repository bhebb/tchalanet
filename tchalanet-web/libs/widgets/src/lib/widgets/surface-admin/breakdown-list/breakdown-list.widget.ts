import { CurrencyPipe, DecimalPipe } from '@angular/common';
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

interface BreakdownItem {
  readonly id: string;
  readonly label: string;
  readonly value: number;
}

type ValueFormat = 'number' | 'currency';

@Component({
  selector: 'tch-breakdown-list-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, LabelPipe],
  templateUrl: './breakdown-list.widget.html',
  styleUrl: './breakdown-list.widget.scss',
})
export class BreakdownListWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'common.state.empty');
  readonly itemsSource = computed(() => this.config()?.props?.['items'] ?? { source: 'dynamic', path: 'items' });
  readonly labelPath = computed(() => stringProp(this.config(), 'labelPath') ?? 'label');
  readonly valuePath = computed(() => stringProp(this.config(), 'valuePath') ?? 'value');
  readonly valueFormat = computed<ValueFormat>(() => {
    const format = stringProp(this.config(), 'valueFormat');
    return format === 'currency' ? 'currency' : 'number';
  });
  readonly currencyCode = computed(() => stringProp(this.config(), 'currencyCode') ?? 'HTG');

  readonly items = computed<readonly BreakdownItem[]>(() => {
    const raw = resolveBinding(this.itemsSource(), this.dynamic());
    if (!Array.isArray(raw)) return [];

    return raw.filter(isRecord).map((item, index) => {
      const value = this.numberAt(item, this.valuePath()) ?? 0;
      const label = this.stringAt(item, this.labelPath()) || `#${index + 1}`;
      return {
        id: this.stringAt(item, 'id') || label,
        label,
        value,
      };
    }).filter(item => item.value > 0);
  });

  readonly total = computed(() => this.items().reduce((sum, item) => sum + item.value, 0));

  percentage(item: BreakdownItem): number {
    const total = this.total();
    return total > 0 ? Math.round((item.value / total) * 100) : 0;
  }

  barWidth(item: BreakdownItem): string {
    return `${this.percentage(item)}%`;
  }

  private stringAt(item: Record<string, unknown>, path: string): string {
    const value = resolvePath(item, path);
    return stringValue(value) ?? '';
  }

  private numberAt(item: Record<string, unknown>, path: string): number | null {
    const value = resolvePath(item, path);
    if (typeof value === 'number') return value;
    if (typeof value === 'string') {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    }
    return null;
  }
}
