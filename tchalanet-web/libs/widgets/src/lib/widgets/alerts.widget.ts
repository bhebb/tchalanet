import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LabelPipe, WidgetConfig, isRecord, stringProp, stringValue } from '@tch/page-model';

interface AlertItem {
  readonly id: string;
  readonly title: string;
  readonly message?: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR' | string;
}

@Component({
  selector: 'tch-alerts-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, LowerCasePipe],
  template: `
    <div class="alerts">
      <h2 class="alerts__title">{{ titleKey() | tchLabel }}</h2>

      @if (alerts().length) {
        <ul class="alerts__list">
          @for (alert of alerts(); track alert.id) {
            <li class="alerts__item alerts__item--{{ alert.severity | lowercase }}">
              <span class="alerts__severity-bar" aria-hidden="true"></span>
              <div class="alerts__body">
                <span class="alerts__alert-title">{{ alert.title }}</span>
                @if (alert.message) {
                  <span class="alerts__message">{{ alert.message }}</span>
                }
              </div>
            </li>
          }
        </ul>
      } @else {
        <p class="alerts__empty">{{ emptyKey() | tchLabel }}</p>
      }
    </div>
  `,
  styles: [
    `
      .alerts {
        display: flex;
        flex-direction: column;
        gap: 0.875rem;
      }

      .alerts__title {
        margin: 0;
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .alerts__list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .alerts__item {
        display: flex;
        gap: 0.625rem;
        padding: 0.625rem 0.875rem;
        border-radius: var(--tch-radius-sm, 8px);
        background: var(--tch-color-surface-container-low, #f3f3f7);
        border-left: 3px solid var(--tch-color-outline, #757780);
      }

      .alerts__item--info {
        border-left-color: var(--tch-color-info, #1a6ef7);
      }

      .alerts__item--warn {
        border-left-color: var(--tch-color-warning, #e6820e);
        background: color-mix(in oklab, var(--tch-color-warning, #e6820e) 6%, transparent);
      }

      .alerts__item--error {
        border-left-color: var(--tch-color-error, #ba1a1a);
        background: color-mix(in oklab, var(--tch-color-error, #ba1a1a) 6%, transparent);
      }

      .alerts__severity-bar {
        display: none;
      }

      .alerts__body {
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
        min-width: 0;
      }

      .alerts__alert-title {
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .alerts__message {
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        color: var(--tch-color-on-surface-variant, #44464f);
      }

      .alerts__empty {
        margin: 0;
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        color: var(--tch-color-on-surface-variant, #44464f);
      }
    `,
  ],
})
export class AlertsWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly emptyKey = computed(() => stringProp(this.config(), 'emptyKey') ?? 'alerts.empty');

  readonly alerts = computed<readonly AlertItem[]>(() => {
    const dyn = this.dynamic();
    if (!isRecord(dyn)) return [];
    const raw = dyn['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map(item => ({
      id: stringValue(item['id']) ?? '',
      title: stringValue(item['title']) ?? '',
      message: stringValue(item['message']),
      severity: stringValue(item['severity']) ?? 'INFO',
    }));
  });
}
