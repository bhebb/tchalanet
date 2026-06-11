import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-operator-cta-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="operator">
      <div class="operator__text">
        @if (eyebrowKey(); as ek) {
          <span class="operator__eyebrow">{{ ek | tchLabel }}</span>
        }
        <h2 class="operator__title">{{ titleKey() | tchLabel }}</h2>
        <p class="operator__body">{{ descriptionKey() | tchLabel }}</p>
      </div>
      <div class="operator__side">
        <div class="operator__actions">
          @if (primaryAction(); as action) {
            <a tch-action class="operator__btn-primary" [attr.href]="href(action)">
              {{ labelKey(action) | tchLabel }}
            </a>
          }
          @if (secondaryAction(); as action) {
            <a tch-action class="operator__btn-secondary" [attr.href]="href(action)">
              {{ labelKey(action) | tchLabel }}
            </a>
          }
        </div>
        @if (tertiaryAction(); as action) {
          <a class="operator__access-link" [attr.href]="href(action)">
            {{ labelKey(action) | tchLabel }}
          </a>
        }
      </div>
    </section>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      .operator {
        display: grid;
        gap: 1.5rem;
        padding: 1.5rem 1.25rem;
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));

        @include bp.up(medium) {
          padding: 2rem;
        }

        @include bp.up(expanded) {
          grid-template-columns: 1fr auto;
          align-items: center;
          gap: 2rem;
          padding: clamp(2rem, 4vw, 3rem);
        }
      }

      /* ── Text ── */

      .operator__text {
        display: grid;
        gap: 0.5rem;
      }

      .operator__eyebrow {
        justify-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.75rem;
        background: color-mix(in oklab, currentColor 15%, transparent);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      .operator__title {
        margin: 0;
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;

        @include bp.up(medium) {
          font-size: var(--tch-font-size-headline-sm, 1.75rem);
        }

        @include bp.up(expanded) {
          font-size: var(--tch-font-size-headline-lg, 2rem);
          line-height: var(--tch-line-height-headline-lg, 2.5rem);
        }
      }

      .operator__body {
        margin: 0;
        max-width: 56ch;
        opacity: 0.85;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.6;
      }

      /* ── Side (actions) ── */

      .operator__side {
        display: grid;
        gap: 0.75rem;
        justify-items: start;

        @include bp.up(expanded) {
          justify-items: end;
          flex-shrink: 0;
        }
      }

      .operator__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }

      /* Override TchActionButton palette for on-primary context */
      .operator__btn-primary {
        --comp-action-bg: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        --comp-action-fg: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
      }

      .operator__btn-secondary {
        --comp-action-bg: transparent;
        --comp-action-fg: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        border: 1px solid color-mix(in oklab, currentColor 45%, transparent);
      }

      /* ── Access text-link (existing users only) ── */

      .operator__access-link {
        font-size: var(--tch-font-size-label-sm, 0.8rem);
        font-weight: 500;
        color: inherit;
        opacity: 0.7;
        text-decoration: underline;
        text-underline-offset: 3px;
        @include tch.focus-visible;
      }
    `,
  ],
})
export class OperatorCtaWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly eyebrowKey = computed(() => stringProp(this.config(), 'eyebrowKey'));
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.operator.title');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? 'public.operator.description');
  readonly primaryAction = computed(() => actionFrom(this.config().props?.['primaryAction']));
  readonly secondaryAction = computed(() => actionFrom(this.config().props?.['secondaryAction']));
  readonly tertiaryAction = computed(() => actionFrom(this.config().props?.['tertiaryAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
