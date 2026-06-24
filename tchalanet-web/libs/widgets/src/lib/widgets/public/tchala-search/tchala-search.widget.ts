import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-tchala-search-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tchala-search.widget.html',
  styleUrl: './tchala-search.widget.scss',
})
export class TchalaSearchWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly badgeKey = computed(() => stringProp(this.config(), 'badgeKey') ?? 'home.tchala.badge');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'home.tchala.title');
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey') ?? 'home.tchala.subtitle');
  readonly ctaAction = computed(() => actionFrom(this.config()?.props?.['ctaAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
