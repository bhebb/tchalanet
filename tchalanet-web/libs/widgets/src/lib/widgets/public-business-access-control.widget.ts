import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface AccessItem {
  id: string;
  icon: string;
  titleKey: string;
  descriptionKey: string;
}

@Component({
  selector: 'tch-public-business-access-control-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pbaccess">
      <div class="pbaccess__header">
        <h2 class="pbaccess__title">{{ titleKey() | tchLabel }}</h2>
        @if (descriptionKey(); as dk) {
          <p class="pbaccess__desc">{{ dk | tchLabel }}</p>
        }
      </div>

      <ul class="pbaccess__grid" role="list">
        @for (item of items(); track item.id) {
          <li class="pbaccess__item">
            <span class="pbaccess__icon material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>
            <h3 class="pbaccess__item-title">{{ item.titleKey | tchLabel }}</h3>
            <p class="pbaccess__item-desc">{{ item.descriptionKey | tchLabel }}</p>
          </li>
        }
      </ul>
    </section>
  `,
  styles: [
    `
      @use 'breakpoints' as bp;

      .pbaccess {
        padding: 3rem 1.25rem;
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));

        @include bp.up(medium) {
          padding: 4rem 2rem;
        }

        @include bp.up(expanded) {
          padding: 5rem clamp(2rem, 6vw, 5rem);
        }
      }

      .pbaccess__header {
        text-align: center;
        display: grid;
        gap: 0.75rem;
        margin-bottom: 2.5rem;
      }

      .pbaccess__title {
        margin: 0;
        font-size: clamp(1.5rem, 3vw, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbaccess__desc {
        margin: 0 auto;
        max-width: 52ch;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.65;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .pbaccess__grid {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 1.5rem;

        @include bp.up(medium) {
          grid-template-columns: repeat(3, 1fr);
        }
      }

      .pbaccess__item {
        display: grid;
        gap: 0.625rem;
        padding: 1.75rem;
        border-radius: var(--tch-radius-lg, 16px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .pbaccess__icon {
        font-size: 2rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .pbaccess__item-title {
        margin: 0;
        font-size: 1rem;
        font-weight: 700;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbaccess__item-desc {
        margin: 0;
        font-size: 0.9rem;
        line-height: 1.6;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }
    `,
  ],
})
export class PublicBusinessAccessControlWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey'));

  readonly items = computed<AccessItem[]>(() => {
    const raw = this.config().props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((i) => ({
      id: String(i['id'] ?? ''),
      icon: String(i['icon'] ?? 'lock'),
      titleKey: String(i['titleKey'] ?? ''),
      descriptionKey: String(i['descriptionKey'] ?? ''),
    }));
  });
}
