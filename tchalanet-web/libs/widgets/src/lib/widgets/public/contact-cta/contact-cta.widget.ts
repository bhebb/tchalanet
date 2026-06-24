import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-contact-cta-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './contact-cta.widget.html',
  styleUrl: './contact-cta.widget.scss',
})
export class ContactCtaWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.contact_cta.title');
  readonly bodyKey = computed(() => stringProp(this.config(), 'bodyKey') ?? 'public.contact_cta.body');
  readonly primaryAction = computed(() => actionFrom(this.config()?.props?.['primaryAction']));
  readonly helpAction = computed(() => actionFrom(this.config()?.props?.['helpAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
