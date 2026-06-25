import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { actionFrom, destinationHref, isRecord, stringProp, WidgetAction, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface HeroStat {
  id: string;
  icon: string;
  labelKey: string;
  value: string;
  highlight?: boolean;
}

@Component({
  selector: 'tch-public-business-hero-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './business-hero.widget.html',
  styleUrl: './business-hero.widget.scss',
})
export class PublicBusinessHeroWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly eyebrowKey = computed(() => stringProp(this.config(), 'eyebrowKey'));
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? '');
  readonly primaryAction = computed(() => actionFrom(this.config().props?.['primaryAction']));
  readonly secondaryAction = computed(() => actionFrom(this.config().props?.['secondaryAction']));

  readonly stats = computed<HeroStat[]>(() => {
    const raw = this.config().props?.['stats'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((s) => ({
      id: String(s['id'] ?? ''),
      icon: String(s['icon'] ?? 'star'),
      labelKey: String(s['labelKey'] ?? ''),
      value: String(s['value'] ?? ''),
      highlight: s['highlight'] === true,
    }));
  });

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }
}
