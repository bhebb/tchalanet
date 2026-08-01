import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, stringProp, stringValue } from '@tch/page-model';

type DashboardPeriod = 'TODAY' | 'YESTERDAY' | 'THIS_WEEK' | 'LAST_WEEK';

@Component({
  selector: 'tch-period-selector-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, RouterLink],
  templateUrl: './period-selector.widget.html',
  styleUrl: './period-selector.widget.scss',
})
export class PeriodSelectorWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');
  readonly selectedPeriod = computed(() => {
    const dynamic = this.dynamic();
    return isRecord(dynamic) ? normalizePeriod(stringValue(dynamic['period'])) : 'TODAY';
  });

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'dashboard.period.title');
  readonly options = computed<readonly DashboardPeriod[]>(() => {
    const values = this.config()?.props?.['options'];
    if (!Array.isArray(values)) return ['TODAY', 'YESTERDAY', 'THIS_WEEK', 'LAST_WEEK'];
    return values
      .map(value => stringValue(value))
      .filter((value): value is DashboardPeriod =>
        value === 'TODAY' || value === 'YESTERDAY' || value === 'THIS_WEEK' || value === 'LAST_WEEK',
      );
  });

  readonly dateRange = computed(() => {
    const dynamic = this.dynamic();
    if (!isRecord(dynamic)) return '';
    const fromDate = stringValue(dynamic['fromDate']);
    const toDate = stringValue(dynamic['toDate']);
    if (!fromDate || !toDate) return '';
    const from = formatDate(fromDate);
    const to = formatDate(toDate);
    return from === to ? from : `${from} – ${to}`;
  });

  periodLabelKey(period: DashboardPeriod): string {
    return `dashboard.period.${period.toLowerCase()}`;
  }
}

function normalizePeriod(value: string | null | undefined): DashboardPeriod {
  return value === 'YESTERDAY' || value === 'THIS_WEEK' || value === 'LAST_WEEK'
    ? value
    : 'TODAY';
}

function formatDate(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return value;
  const date = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]), 12));
  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(date);
}
