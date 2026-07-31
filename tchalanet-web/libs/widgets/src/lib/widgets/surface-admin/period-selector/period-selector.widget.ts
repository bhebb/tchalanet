import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, stringProp, stringValue } from '@tch/page-model';

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

  periodLabelKey(period: DashboardPeriod): string {
    return `dashboard.period.${period.toLowerCase()}`;
  }
}
