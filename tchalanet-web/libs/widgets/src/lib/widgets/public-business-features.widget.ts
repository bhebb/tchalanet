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
  template: `
    <section class="pbfeatures" [id]="sectionId()">
      <div class="pbfeatures__header">
        <h2 class="pbfeatures__title">{{ titleKey() | tchLabel }}</h2>
        @if (subtitleKey(); as sk) {
          <p class="pbfeatures__subtitle">{{ sk | tchLabel }}</p>
        }
      </div>

      <ul class="pbfeatures__grid" role="list">
        @for (item of items(); track item.id) {
          <li class="pbfeatures__item">
            <span class="pbfeatures__icon material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            <h3 class="pbfeatures__item-title">{{ item.titleKey | tchLabel }}</h3>
            <p class="pbfeatures__item-desc">{{ item.descriptionKey | tchLabel }}</p>
          </li>
        }
      </ul>
    </section>
  `,
  styles: [
    `
      @use 'breakpoints' as bp;

      .pbfeatures {
        padding: 3rem 1.25rem;

        @include bp.up(medium) {
          padding: 4rem 2rem;
        }

        @include bp.up(expanded) {
          padding: 5rem clamp(2rem, 6vw, 5rem);
        }
      }

      .pbfeatures__header {
        display: grid;
        gap: 0.75rem;
        text-align: center;
        margin-bottom: 2.5rem;
      }

      .pbfeatures__title {
        margin: 0;
        font-size: clamp(1.5rem, 3vw, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbfeatures__subtitle {
        margin: 0 auto;
        max-width: 54ch;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.65;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .pbfeatures__grid {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 1.5rem;
        grid-template-columns: 1fr;

        @include bp.up(medium) {
          grid-template-columns: repeat(2, 1fr);
        }

        @include bp.up(expanded) {
          grid-template-columns: repeat(3, 1fr);
        }
      }

      .pbfeatures__item {
        display: grid;
        gap: 0.75rem;
        padding: 2rem;
        border-radius: var(--tch-radius-lg, 16px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
      }

      .pbfeatures__icon {
        font-size: 2.25rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .pbfeatures__item-title {
        margin: 0;
        font-size: 1.0625rem;
        font-weight: 700;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbfeatures__item-desc {
        margin: 0;
        font-size: 0.9375rem;
        line-height: 1.65;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }
    `,
  ],
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
