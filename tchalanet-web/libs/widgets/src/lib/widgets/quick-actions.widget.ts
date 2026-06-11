import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  LabelPipe,
  WidgetAction,
  WidgetConfig,
  actionsFrom,
  destinationHref,
  stringProp,
} from '@tch/page-model';

@Component({
  selector: 'tch-quick-actions-widget',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LabelPipe, RouterLink],
  template: `
    <div class="quick-actions">
      <h2 class="quick-actions__title">{{ titleKey() | tchLabel }}</h2>
      <div class="quick-actions__grid">
        @for (action of actions(); track action.id ?? action.labelKey) {
          @if (routerPath(action); as path) {
            <a class="qa-card" [routerLink]="path">
              @if (action.icon) {
                <span class="qa-card__icon material-symbols-outlined" aria-hidden="true">
                  {{ action.icon }}
                </span>
              }
              <span class="qa-card__label">{{ action.labelKey | tchLabel }}</span>
            </a>
          } @else {
            <a class="qa-card" [attr.href]="destinationHref(action.destination)">
              @if (action.icon) {
                <span class="qa-card__icon material-symbols-outlined" aria-hidden="true">
                  {{ action.icon }}
                </span>
              }
              <span class="qa-card__label">{{ action.labelKey | tchLabel }}</span>
            </a>
          }
        }
      </div>
    </div>
  `,
  styles: [
    `
      .quick-actions {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .quick-actions__title {
        margin: 0;
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .quick-actions__grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(8rem, 1fr));
        gap: 0.75rem;
      }

      .qa-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem 0.75rem;
        border-radius: var(--tch-radius-md, 12px);
        background: var(--tch-color-surface-container-low, #f3f3f7);
        border: 1px solid var(--tch-color-outline-variant, #e0e0e8);
        text-decoration: none;
        color: inherit;
        cursor: pointer;
        transition: background 0.15s ease;

        &:hover {
          background: var(--tch-color-surface-container, #ecedf1);
        }

        &:focus-visible {
          outline: 2px solid var(--tch-color-primary, #1a6ef7);
          outline-offset: 2px;
        }
      }

      .qa-card__icon {
        font-size: 1.5rem;
        color: var(--tch-color-primary, #1a6ef7);
      }

      .qa-card__label {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 600;
        text-align: center;
        color: var(--tch-color-on-surface, #1a1c1e);
      }
    `,
  ],
})
export class QuickActionsWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly actions = computed<readonly WidgetAction[]>(() =>
    actionsFrom(this.config()?.props?.['actions']),
  );

  routerPath(action: WidgetAction): string | null {
    return action.destination?.kind === 'route' ? action.destination.value : null;
  }

  destinationHref = destinationHref;
}
