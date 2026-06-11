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

/** `FeatureGridWidget`: compact preview grid rendered from its resolved dynamic payload. */
@Component({
  selector: 'tch-feature-grid-widget',
  imports: [LabelPipe, TchPubCard, TchPubCardGrid],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="features">
      <h2 class="features__title">{{ titleKey() | tchLabel }}</h2>
      <tch-public-card-grid style="--pub-card-grid-min: 220px">
        @for (item of items(); track item.id ?? $index) {
          <tch-public-card density="compact">
            <h3 class="features__name">{{ item.title ?? (item.title_key | tchLabel) }}</h3>
            @if (item.description || item.description_key) {
              <p class="features__desc">
                {{ item.description ?? (item.description_key | tchLabel) }}
              </p>
            }
          </tch-public-card>
        }
      </tch-public-card-grid>
    </section>
  `,
  styles: [
    `
      .features {
        display: grid;
        gap: 1rem;
        padding: 1.5rem;
      }
      .features__title {
        margin: 0;
        font-size: 1.25rem;
      }
      .features__name {
        margin: 0;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: 1rem;
      }
      .features__desc {
        margin: 0;
        opacity: 0.85;
      }
    `,
  ],
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
