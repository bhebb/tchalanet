import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface FeatureItem {
  id: string;
  icon: string;
  titleKey: string;
  descriptionKey: string;
}

@Component({
  selector: 'tch-public-business-features-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './business-features.widget.html',
  styleUrl: './business-features.widget.scss',
})
export class PublicBusinessFeaturesWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey'));
  readonly sectionId = computed(() => stringProp(this.config(), 'id') ?? 'managers-features');

  readonly items = computed<FeatureItem[]>(() => {
    const raw = this.config().props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((i) => ({
      id: String(i['id'] ?? ''),
      icon: String(i['icon'] ?? 'star'),
      titleKey: String(i['titleKey'] ?? ''),
      descriptionKey: String(i['descriptionKey'] ?? ''),
    }));
  });
}
