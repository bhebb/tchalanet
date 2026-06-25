import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, WidgetConfig, isRecord, stringProp, stringValue } from '@tch/page-model';

interface ResourceItem {
  readonly id: string;
  readonly displayName: string;
  readonly status: string;
  readonly severity: 'OK' | 'WARNING' | 'CRITICAL' | string;
  readonly memoryUsedMb?: number;
  readonly memoryLimitMb?: number;
  readonly memoryPercent?: number;
  readonly cpuPercent?: number;
  readonly restartCount?: number;
  readonly oomKilled?: boolean;
  readonly sizeMb?: number;
  readonly indexSizeMb?: number;
  readonly tableCount?: number;
  readonly message?: string;
  readonly detailsPath?: string;
}

@Component({
  selector: 'tch-resource-status-list-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, LabelPipe, RouterLink],
  templateUrl: './resource-status-list.widget.html',
  styleUrl: './resource-status-list.widget.scss',
})
export class ResourceStatusListWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'common.state.empty');
  readonly detailsPath = computed(() => stringProp(this.config(), 'detailsPath') ?? '');
  readonly detailsLabelKey = computed(() => stringProp(this.config(), 'detailsLabelKey') ?? 'common.action.view');
  readonly serviceKeyPrefix = computed(() => stringProp(this.config(), 'serviceKeyPrefix') ?? '');
  readonly maxItems = computed(() => {
    const value = numberProp(this.config(), 'maxItems');
    return value && value > 0 ? value : undefined;
  });
  readonly services = computed<readonly ResourceItem[]>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn) || !Array.isArray(dyn['services'])) return [];

    const prefix = this.serviceKeyPrefix();
    return dyn['services'].filter(isRecord).map((service, index) => ({
      id: stringValue(service['serviceKey']) ?? `${index}`,
      displayName: stringValue(service['displayName']) ?? `#${index + 1}`,
      status: stringValue(service['status']) ?? 'UNKNOWN',
      severity: stringValue(service['severity']) ?? 'WARNING',
      memoryUsedMb: numberValue(service['memoryUsedMb']),
      memoryLimitMb: numberValue(service['memoryLimitMb']),
      memoryPercent: numberValue(service['memoryPercent']),
      cpuPercent: numberValue(service['cpuPercent']),
      restartCount: numberValue(service['restartCount']),
      oomKilled: booleanValue(service['oomKilled']),
      sizeMb: numberValue(service['sizeMb']),
      indexSizeMb: numberValue(service['indexSizeMb']),
      tableCount: numberValue(service['tableCount']),
      message: stringValue(service['message']),
      detailsPath: stringValue(service['detailsPath']),
    })).filter((service) => !prefix || service.id.startsWith(prefix));
  });

  readonly visibleServices = computed<readonly ResourceItem[]>(() => {
    const sorted = [...this.services()].sort((a, b) => severityRank(b.severity) - severityRank(a.severity));
    const max = this.maxItems();
    return max ? sorted.slice(0, max) : sorted;
  });

  readonly summary = computed(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return { criticalCount: 0, warningCount: 0 };
    return {
      criticalCount: numberValue(dyn['criticalCount']) ?? 0,
      warningCount: numberValue(dyn['warningCount']) ?? 0,
    };
  });

}

function severityRank(severity: string): number {
  switch (severity) {
    case 'CRITICAL':
      return 3;
    case 'WARNING':
      return 2;
    case 'OK':
      return 1;
    default:
      return 0;
  }
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
