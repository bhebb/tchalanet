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
    return action.destination?.kind === 'route' ? action.destination.value : null;
  }

  destinationHref = destinationHref;
}
