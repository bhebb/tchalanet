import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, stringProp, stringValue } from '@tch/page-model';

interface OpsJobItem {
  readonly id: string;
  readonly displayName: string;
  readonly jobKey: string;
  readonly scope: string;
  readonly status: string;
  readonly severity: 'OK' | 'WARNING' | 'CRITICAL' | string;
  readonly context?: string;
}

@Component({
  selector: 'tch-ops-job-status-list-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, RouterLink],
  templateUrl: './ops-job-status-list.widget.html',
  styleUrl: './ops-job-status-list.widget.scss',
})
export class OpsJobStatusListWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'common.state.empty');
  readonly detailsPath = computed(() => stringProp(this.config(), 'detailsPath') ?? '');
  readonly detailsLabelKey = computed(() => stringProp(this.config(), 'detailsLabelKey') ?? 'common.action.view');
  readonly maxItems = computed(() => {
    const value = numberProp(this.config(), 'maxItems');
    return value && value > 0 ? value : undefined;
  });

  readonly summary = computed(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) {
      return { failedCount: 0, disabledGateCount: 0, staleCount: 0, neverRunCount: 0, historyAvailable: false };
    }
    return {
      failedCount: numberValue(dyn['failedCount']) ?? 0,
      disabledGateCount: numberValue(dyn['disabledGateCount']) ?? 0,
      staleCount: numberValue(dyn['staleCount']) ?? 0,
      neverRunCount: numberValue(dyn['neverRunCount']) ?? 0,
      historyAvailable: booleanValue(dyn['historyAvailable']) ?? false,
    };
  });

  readonly jobs = computed<readonly OpsJobItem[]>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn) || !Array.isArray(dyn['items'])) return [];

    return dyn['items'].filter(isRecord).map((job, index) => ({
      id: stringValue(job['jobKey']) ?? `${index}`,
      jobKey: stringValue(job['jobKey']) ?? '',
      displayName: stringValue(job['displayName']) ?? `#${index + 1}`,
      scope: stringValue(job['scope']) ?? 'GLOBAL',
      status: stringValue(job['status']) ?? 'UNKNOWN',
      severity: stringValue(job['severity']) ?? 'WARNING',
      context: stringValue(job['context']),
    }));
  });

  readonly visibleJobs = computed<readonly OpsJobItem[]>(() => {
    const max = this.maxItems();
    return max ? this.jobs().slice(0, max) : this.jobs();
  });
}

function numberValue(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
  return undefined;
}

function booleanValue(value: unknown): boolean | undefined {
  return typeof value === 'boolean' ? value : undefined;
}

function numberProp(config: WidgetConfig | undefined, key: string): number | undefined {
  const value = config?.props?.[key];
  return numberValue(value);
}
