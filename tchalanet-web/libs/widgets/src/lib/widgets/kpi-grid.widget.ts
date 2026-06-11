import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import {
  LabelPipe,
  WidgetConfig,
  isRecord,
  resolveBinding,
  stringProp,
  stringValue,
} from '@tch/page-model';

interface KpiItem {
  readonly id: string;
  readonly labelKey: string;
  readonly icon?: string;
  /** Raw config value: a literal, or a `{ source:'dynamic', path }` binding into the payload. */
  readonly value?: unknown;
}

@Component({
  selector: 'tch-kpi-grid-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe],
  template: `
    <div class="kpi-grid">
      <h2 class="kpi-grid__title">{{ titleKey() | tchLabel }}</h2>
      <div class="kpi-grid__items">
        @for (item of items(); track item.id) {
          <div class="kpi-card">
            @if (item.icon) {
              <span class="kpi-card__icon material-symbols-outlined" aria-hidden="true">
                {{ item.icon }}
              </span>
            }
            <span class="kpi-card__label">{{ item.labelKey | tchLabel }}</span>
            @if (resolvedValue(item); as val) {
              <span class="kpi-card__value">{{ val }}</span>
            } @else {
              <span class="kpi-card__value kpi-card__value--placeholder">—</span>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .kpi-grid {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .kpi-grid__title {
        margin: 0;
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .kpi-grid__items {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
        gap: 0.75rem;
      }

      .kpi-card {
        display: flex;
        flex-direction: column;
        gap: 0.375rem;
        padding: 1rem;
        border-radius: var(--tch-radius-md, 12px);
        background: var(--tch-color-surface-container-low, #f3f3f7);
        border: 1px solid var(--tch-color-outline-variant, #e0e0e8);
      }

      .kpi-card__icon {
        font-size: 1.25rem;
        color: var(--tch-color-primary, #1a6ef7);
      }

      .kpi-card__label {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, #44464f);
        font-weight: 500;
      }

      .kpi-card__value {
        font-size: var(--tch-font-size-title-lg, 1.375rem);
        font-weight: 700;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .kpi-card__value--placeholder {
        color: var(--tch-color-outline, #757780);
      }
    `,
  ],
})
export class KpiGridWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly items = computed<readonly KpiItem[]>(() => {
    const raw = this.config()?.props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(item => ({
      id: stringValue(item['id']) ?? '',
      labelKey: stringValue(item['labelKey']) ?? '',
      icon: stringValue(item['icon']),
      value: item['value'],
    }));
  });

  /**
   * Resolve a KPI value: a declared `{ source:'dynamic', path }` binding wins; otherwise fall back
   * to the legacy flat lookup `dynamic.widgets[id][itemId]` for backward compatibility.
   */
  resolvedValue(item: KpiItem): string | number | undefined {
    const bound = resolveBinding(item.value, this.dynamic());
    if (typeof bound === 'string' || typeof bound === 'number') {
      return bound;
    }
    return this.dynamicValue(item.id);
  }

  private dynamicValue(id: string): string | number | undefined {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return undefined;
    const val = dyn[id];
    return typeof val === 'string' || typeof val === 'number' ? val : undefined;
  }
}
