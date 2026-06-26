import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, stringProp, stringValue } from '@tch/page-model';

interface ReadinessCheck {
  readonly code: string;
  readonly labelKey: string;
  readonly status: 'READY' | 'MISSING' | 'BLOCKED' | 'WARNING' | string;
  readonly message?: string;
  readonly path?: string;
}

@Component({
  selector: 'tch-readiness-summary-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, LowerCasePipe, RouterLink],
  templateUrl: './readiness-summary.widget.html',
  styleUrl: './readiness-summary.widget.scss',
})
export class ReadinessSummaryWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly checks = computed<readonly ReadinessCheck[]>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return [];
    const raw = dyn['checks'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(item => ({
      code: stringValue(item['code']) ?? '',
      labelKey: stringValue(item['labelKey']) ?? '',
      status: stringValue(item['status']) ?? 'READY',
      message: stringValue(item['message']),
      path: stringValue(item['path']),
    }));
  });
}
