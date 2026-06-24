import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-rules-simulation-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './rules-simulation.widget.html',
  styleUrl: './rules-simulation.widget.scss',
})
export class RulesSimulationWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly badgeKey = computed(() => stringProp(this.config(), 'badgeKey') ?? 'public.rules.badge');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.rules.title');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? 'public.rules.subtitle');
  readonly ctaNoteKey = computed(() => stringProp(this.config(), 'ctaNoteKey') ?? 'public.rules.cta_note');
  readonly primaryAction = computed(() => actionFrom(this.config()?.props?.['primaryAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
