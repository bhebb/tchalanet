import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  LabelPipe,
  WidgetAction,
  WidgetConfig,
  actionsFrom,
  destinationHref,
  isRecord,
  stringProp,
} from '@tch/page-model';

@Component({
  selector: 'tch-quick-actions-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, RouterLink],
  templateUrl: './quick-actions.widget.html',
  styleUrl: './quick-actions.widget.scss',
})
export class QuickActionsWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly actions = computed<readonly WidgetAction[]>(() => {
    const dyn = this.dynamic();
    if (isRecord(dyn) && Array.isArray(dyn['actions'])) {
      const fromDynamic = actionsFrom(dyn['actions']);
      if (fromDynamic.length > 0) return fromDynamic;
    }
    return actionsFrom(this.config()?.props?.['actions']);
  });

  routerPath(action: WidgetAction): string | null {
    if (action.destination?.kind !== 'route') return null;
    const queryIndex = action.destination.value.indexOf('?');
    return queryIndex >= 0 ? action.destination.value.slice(0, queryIndex) : action.destination.value;
  }

  routerQueryParams(action: WidgetAction): Record<string, string> | null {
    if (action.destination?.kind !== 'route') return null;
    const queryIndex = action.destination.value.indexOf('?');
    if (queryIndex < 0) return null;

    const params: Record<string, string> = {};
    new URLSearchParams(action.destination.value.slice(queryIndex + 1)).forEach((value, key) => {
      params[key] = value;
    });
    return Object.keys(params).length > 0 ? params : null;
  }

  destinationHref = destinationHref;
}
