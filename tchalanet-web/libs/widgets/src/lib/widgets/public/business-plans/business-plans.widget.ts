import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { actionFrom, destinationHref, isRecord, stringProp, WidgetAction, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface PlanItem {
  id: string;
  highlighted: boolean;
  badgeKey: string | null;
  nameKey: string;
  priceKey: string;
  descriptionKey: string;
  features: string[];
  action: WidgetAction | undefined;
}

@Component({
  selector: 'tch-public-business-plans-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './business-plans.widget.html',
  styleUrl: './business-plans.widget.scss',
})
export class PublicBusinessPlansWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey'));

  readonly plans = computed<PlanItem[]>(() => {
    const raw = this.config().props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((p) => ({
      id: String(p['id'] ?? ''),
      highlighted: p['highlighted'] === true,
      badgeKey: typeof p['badgeKey'] === 'string' ? p['badgeKey'] : null,
      nameKey: String(p['nameKey'] ?? ''),
      priceKey: String(p['priceKey'] ?? ''),
      descriptionKey: String(p['descriptionKey'] ?? ''),
      features: Array.isArray(p['features']) ? p['features'].map(String) : [],
      action: actionFrom(p['action']),
    }));
  });

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }
}
