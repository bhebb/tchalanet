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

interface TrendPoint {
  readonly id: string;
  readonly label: string;
  readonly value: number;
}

type LabelFormat = 'raw' | 'date-short';
type ChartType = 'line' | 'bar';

@Component({
  selector: 'tch-trend-chart-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, LabelPipe],
  templateUrl: './trend-chart.widget.html',
  styleUrl: './trend-chart.widget.scss',
})
export class TrendChartWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'common.state.empty');
  readonly singlePointKey = computed(
    () => stringProp(this.config(), 'singlePointKey') ?? 'common.state.single_point',
  );
  readonly valueFormat = computed(() => stringProp(this.config(), 'valueFormat') ?? 'number');
  readonly currencyCode = computed(() => stringProp(this.config(), 'currencyCode') ?? 'HTG');
  readonly chartType = computed<ChartType>(() =>
    stringProp(this.config(), 'chartType') === 'bar' ? 'bar' : 'line',
  );
  readonly pointsSource = computed(() => this.config()?.props?.['points'] ?? { source: 'dynamic', path: 'points' });
  readonly labelPath = computed(() => stringProp(this.config(), 'labelPath') ?? 'label');
  readonly valuePath = computed(() => stringProp(this.config(), 'valuePath') ?? 'value');
  readonly labelFormat = computed<LabelFormat>(() => {
    const format = stringProp(this.config(), 'labelFormat');
    return format === 'date-short' ? 'date-short' : 'raw';
  });

  readonly points = computed<readonly TrendPoint[]>(() => {
    const raw = resolveBinding(this.pointsSource(), this.dynamic());
    if (!Array.isArray(raw)) return [];

    return raw.filter(isRecord).map((point, index) => {
      const value = this.numberAt(point, this.valuePath()) ?? 0;
      return {
        id: this.stringAt(point, 'id') || `${index}-${value}`,
        label: this.displayLabel(this.stringAt(point, this.labelPath()), index),
        value,
      };
    });
  });

  readonly hasTrend = computed(() => this.points().length >= 2);
  readonly hasChart = computed(() => this.chartType() === 'bar' ? this.points().length > 0 : this.hasTrend());
  readonly chartMax = computed(() => Math.max(...this.points().map(point => point.value), 1));
  readonly yAxisTicks = computed(() => {
    const max = this.chartMax();
    return [max, max * 0.75, max * 0.5, max * 0.25, 0];
  });

  readonly linePoints = computed(() => {
    const points = this.points();
    if (points.length === 0) return '';

    const max = Math.max(...points.map(point => point.value), 1);
    const min = Math.min(...points.map(point => point.value), 0);
    const spread = Math.max(max - min, 1);
    const lastIndex = Math.max(points.length - 1, 1);

    return points.map((point, index) => {
      const x = (index / lastIndex) * 100;
      const y = 88 - ((point.value - min) / spread) * 76;
      return `${x.toFixed(2)},${y.toFixed(2)}`;
    }).join(' ');
  });

  readonly areaPoints = computed(() => {
    if (!this.hasTrend()) return '';
    const line = this.linePoints();
    return line ? `0,96 ${line} 100,96` : '';
  });

  barHeight(point: TrendPoint): number {
    const max = this.chartMax();
    if (point.value <= 0) return 2;
    return Math.max((point.value / max) * 100, 4);
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

  private displayLabel(raw: string, index: number): string {
    if (!raw) {
      return String(index + 1);
    }
    if (this.labelFormat() !== 'date-short') {
      return raw;
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(raw);
    return match ? `${match[2]}/${match[3]}` : raw;
  }
}
