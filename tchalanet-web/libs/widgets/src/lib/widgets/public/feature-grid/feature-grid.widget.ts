import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TchPubCard, TchPubCardGrid } from '@tch/ui/components';

import { LabelPipe, WidgetConfig, stringProp } from '@tch/page-model';

interface FeatureItem {
  readonly id?: string;
  readonly title?: string;
  readonly title_key?: string;
  readonly description?: string;
  readonly description_key?: string;
  readonly icon?: string;
}

interface FeatureDynamic {
  readonly items?: readonly FeatureItem[];
  readonly features?: readonly FeatureItem[];
}

@Component({
  selector: 'tch-feature-grid-widget',
  imports: [LabelPipe, TchPubCard, TchPubCardGrid],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './feature-grid.widget.html',
  styleUrl: './feature-grid.widget.scss',
})
export class FeatureGridWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(
    () => stringProp(this.config(), 'title_key') ?? 'home.features.title',
  );
  readonly items = computed<readonly FeatureItem[]>(() => {
    const data = this.dynamic() as FeatureDynamic;
    return data?.items ?? data?.features ?? [];
  });
}
