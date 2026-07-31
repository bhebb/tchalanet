import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, resolvePath, stringProp, stringValue } from '@tch/page-model';

interface PerformanceRow {
  readonly id: string;
  readonly label: string;
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly sellerCommission: number;
  readonly netRevenueEstimated: number;
}

@Component({
  selector: 'tch-terminal-performance-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, LabelPipe, RouterLink],
  templateUrl: './terminal-performance.widget.html',
  styleUrl: './terminal-performance.widget.scss',
})
export class TerminalPerformanceWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'dashboard.tenant_admin.terminal_performance.title');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'dashboard.tenant_admin.terminal_performance.empty');
  readonly page = computed(() => this.numberAt(this.dynamic(), 'page') ?? 0);
  readonly pageSize = computed(() => this.numberAt(this.dynamic(), 'pageSize') ?? 15);
  readonly totalElements = computed(() => this.numberAt(this.dynamic(), 'totalElements') ?? 0);
  readonly rows = computed<readonly PerformanceRow[]>(() => {
    const raw = resolvePath(this.dynamic(), 'rows');
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((row, index) => ({
      id: stringValue(row['id']) ?? `${index}`,
      label: stringValue(row['label']) ?? `#${index + 1}`,
      ticketsSold: this.numberAt(row, 'ticketsSold') ?? 0,
      grossSales: this.numberAt(row, 'grossSales') ?? 0,
      sellerCommission: this.numberAt(row, 'sellerCommission') ?? 0,
      netRevenueEstimated: this.numberAt(row, 'netRevenueEstimated') ?? 0,
    }));
  });

  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.totalElements() / this.pageSize())));

  private numberAt(value: unknown, path: string): number | undefined {
    const raw = resolvePath(value, path);
    if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
    if (typeof raw === 'string') {
      const parsed = Number(raw);
      return Number.isFinite(parsed) ? parsed : undefined;
    }
    return undefined;
  }
}
