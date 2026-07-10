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
      title: alertTitleKey(stringValue(item['title']) ?? stringValue(item['messageKey']) ?? ''),
      message: stringValue(item['message']),
      path: stringValue(item['path']),
      severity: stringValue(item['severity']) ?? 'INFO',
    }));
  });
}

function alertTitleKey(value: string): string {
  const clean = value.trim();
  if (!clean || clean.includes('.')) return clean;

  const slug = clean.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
  if (slug === 'closed_draws_pending_results') {
    return 'dashboard.tenant_admin.alerts.closed_draws_pending_results';
  }
  if (slug === 'blocked_seller_terminals') {
    return 'dashboard.tenant_admin.alerts.blocked_seller_terminals';
  }
  if (slug === 'unread_notifications') {
    return 'dashboard.tenant_admin.alerts.unread_notifications';
  }
  return clean;
}
