import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, stringProp } from '@tch/page-model';

function numVal(v: unknown): number | null {
  if (typeof v === 'number') return v;
  if (typeof v === 'string') {
    const n = parseFloat(v);
    return isNaN(n) ? null : n;
  }
  return null;
}

@Component({
  selector: 'tch-commission-summary-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, RouterLink],
  templateUrl: './commission-summary.widget.html',
  styleUrl: './commission-summary.widget.scss',
})
export class CommissionSummaryWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly manageLink = computed(() => stringProp(this.config(), 'manageLink') ?? null);

  readonly tenantDefaultRate = computed<number | null>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return null;
    const val = numVal(dyn['tenantDefaultRate']);
    return val ?? null;
  });

  readonly totalSellerTerminals = computed<number>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return 0;
    return numVal(dyn['totalSellerTerminals']) ?? 0;
  });

  readonly countAtDefaultRate = computed<number>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return 0;
    return numVal(dyn['countAtDefaultRate']) ?? 0;
  });

  readonly countWithCustomRate = computed<number>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return 0;
    return numVal(dyn['countWithCustomRate']) ?? 0;
  });

  readonly rateRange = computed<string | null>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return null;
    const min = numVal(dyn['minRate']);
    const max = numVal(dyn['maxRate']);
    if (min === null || max === null) return null;
    return `${min}% – ${max}%`;
  });
}
