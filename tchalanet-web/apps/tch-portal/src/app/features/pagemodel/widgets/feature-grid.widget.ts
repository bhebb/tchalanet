import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { stringProp } from '../widget.contract';

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
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="features">
      <h2 class="features__title">{{ titleKey() | tchLabel }}</h2>
      <ul class="features__grid">
        @for (item of items(); track item.id ?? $index) {
          <li class="features__card">
            <h3 class="features__name">{{ item.title ?? (item.title_key | tchLabel) }}</h3>
            @if (item.description || item.description_key) {
              <p class="features__desc">
                {{ item.description ?? (item.description_key | tchLabel) }}
              </p>
            }
          </li>
        }
      </ul>
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
      .features__grid {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 1rem;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      }
      .features__card {
        display: grid;
        gap: 0.5rem;
        padding: 1.25rem;
        border-radius: var(--tch-radius-control, 12px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
        border: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
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
