import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LabelPipe, WidgetConfig, isRecord, stringProp, stringValue } from '@tch/page-model';

interface ReadinessCheck {
  readonly code: string;
  readonly labelKey: string;
  readonly status: 'READY' | 'MISSING' | 'BLOCKED' | 'WARNING' | string;
  readonly message?: string;
}

@Component({
  selector: 'tch-readiness-summary-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, LowerCasePipe],
  template: `
    <div class="readiness">
      <h2 class="readiness__title">{{ titleKey() | tchLabel }}</h2>

      @if (checks().length) {
        <ul class="readiness__list">
          @for (check of checks(); track check.code) {
            <li class="readiness__item readiness__item--{{ check.status | lowercase }}">
              <span class="readiness__status-dot" aria-hidden="true"></span>
              <span class="readiness__label">{{ check.labelKey | tchLabel }}</span>
            </li>
          }
        </ul>
      } @else {
        <p class="readiness__empty">{{ 'readiness.noIssues' | tchLabel }}</p>
      }
    </div>
  `,
  styles: [
    `
      .readiness {
        display: flex;
        flex-direction: column;
        gap: 0.875rem;
      }

      .readiness__title {
        margin: 0;
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .readiness__list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .readiness__item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.625rem 0.875rem;
        border-radius: var(--tch-radius-sm, 8px);
        background: var(--tch-color-surface-container-low, #f3f3f7);
      }

      .readiness__status-dot {
        width: 0.5rem;
        height: 0.5rem;
        border-radius: 50%;
        flex-shrink: 0;
        background: var(--tch-color-outline, #757780);
      }

      .readiness__item--ready .readiness__status-dot {
        background: var(--tch-color-success, #1a7f4b);
      }

      .readiness__item--warning .readiness__status-dot {
        background: var(--tch-color-warning, #e6820e);
      }

      .readiness__item--blocked .readiness__status-dot,
      .readiness__item--missing .readiness__status-dot {
        background: var(--tch-color-error, #ba1a1a);
      }

      .readiness__label {
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .readiness__empty {
        margin: 0;
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        color: var(--tch-color-on-surface-variant, #44464f);
      }
    `,
  ],
})
export class ReadinessSummaryWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly checks = computed<readonly ReadinessCheck[]>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return [];
    const raw = dyn['checks'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(item => ({
      code: stringValue(item['code']) ?? '',
      labelKey: stringValue(item['labelKey']) ?? '',
      status: stringValue(item['status']) ?? 'READY',
      message: stringValue(item['message']),
    }));
  });
}
