import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '../widget.contract';

@Component({
  selector: 'tch-operator-cta-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="operator">
      <div>
        <h2>{{ titleKey() | tchLabel }}</h2>
        <p>{{ descriptionKey() | tchLabel }}</p>
      </div>
      <div class="operator__actions">
        @if (primaryAction(); as action) {
          <a class="operator__primary" [attr.href]="href(action)">{{ labelKey(action) | tchLabel }}</a>
        }
        @if (secondaryAction(); as action) {
          <a class="operator__secondary" [attr.href]="href(action)">{{ labelKey(action) | tchLabel }}</a>
        }
      </div>
    </section>
  `,
  styles: [
    `
      .operator {
        display: flex;
        justify-content: space-between;
        gap: 1.5rem;
        align-items: center;
        padding: clamp(1.5rem, 5vw, 3rem);
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }
      .operator h2,
      .operator p {
        margin: 0;
      }
      .operator p {
        max-width: 62ch;
        color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
      }
      .operator__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }
      .operator a {
        min-height: var(--tch-touch-target, 48px);
        display: inline-flex;
        align-items: center;
        padding: 0 1rem;
        border-radius: var(--tch-radius-control, 8px);
        text-decoration: none;
        font-weight: 800;
      }
      .operator__primary {
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
      }
      .operator__secondary {
        color: inherit;
        border: 1px solid color-mix(in oklab, currentColor 40%, transparent);
      }
      @media (max-width: 720px) {
        .operator {
          display: grid;
        }
      }
    `,
  ],
})
export class OperatorCtaWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.operator.title');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? 'public.operator.description');
  readonly primaryAction = computed(() => actionFrom(this.config().props?.['primaryAction']));
  readonly secondaryAction = computed(() => actionFrom(this.config().props?.['secondaryAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
