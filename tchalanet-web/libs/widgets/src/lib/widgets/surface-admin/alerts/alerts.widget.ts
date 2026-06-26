import { LowerCasePipe, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, stringProp, stringValue } from '@tch/page-model';

interface AlertItem {
  readonly id: string;
  readonly title: string;
  readonly message?: string;
  readonly path?: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR' | string;
}

@Component({
  selector: 'tch-alerts-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, LowerCasePipe, NgTemplateOutlet, RouterLink],
  templateUrl: './alerts.widget.html',
  styleUrl: './alerts.widget.scss',
})
export class AlertsWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'alerts.empty');

  readonly alerts = computed<readonly AlertItem[]>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return [];
    const raw = dyn['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(item => ({
      id: stringValue(item['id']) ?? '',
      title: stringValue(item['title']) ?? stringValue(item['messageKey']) ?? '',
      message: stringValue(item['message']),
      path: stringValue(item['path']),
      severity: stringValue(item['severity']) ?? 'INFO',
    }));
  });
}
