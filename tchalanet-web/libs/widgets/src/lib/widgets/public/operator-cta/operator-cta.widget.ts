import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-operator-cta-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './operator-cta.widget.html',
  styleUrl: './operator-cta.widget.scss',
})
export class OperatorCtaWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly eyebrowKey = computed(() => stringProp(this.config(), 'eyebrowKey'));
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.operator.title');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? 'public.operator.description');
  readonly primaryAction = computed(() => actionFrom(this.config().props?.['primaryAction']));
  readonly secondaryAction = computed(() => actionFrom(this.config().props?.['secondaryAction']));
  readonly tertiaryAction = computed(() => actionFrom(this.config().props?.['tertiaryAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
