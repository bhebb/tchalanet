import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-rules-simulation-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="rules">
      <div class="rules__intro">
        <span class="rules__badge">{{ badgeKey() | tchLabel }}</span>
        <h2 class="rules__title">{{ titleKey() | tchLabel }}</h2>
        <p class="rules__body">{{ descriptionKey() | tchLabel }}</p>
      </div>
      <div class="rules__actions">
        @if (primaryAction(); as action) {
          <a tch-action variant="primary" [attr.href]="href(action)">
            {{ labelKey(action) | tchLabel }}
          </a>
        } @else {
          <a tch-action variant="primary" href="/public/rules">
            {{ 'public.rules.cta_rules' | tchLabel }}
          </a>
        }
        <p class="rules__cta-note">{{ ctaNoteKey() | tchLabel }}</p>
      </div>
    </section>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      .rules {
        display: grid;
        gap: 1.5rem;
        padding: 1.5rem 1.25rem;
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));

        @include bp.up(medium) {
          padding: 2rem;
          gap: 1.75rem;
        }

        @include bp.up(expanded) {
          padding: clamp(2rem, 4vw, 3rem);
        }
      }

      /* ── Intro ── */

      .rules__intro {
        display: grid;
        gap: 0.625rem;
        align-content: start;
      }

      .rules__badge {
        justify-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.75rem;
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      .rules__title {
        margin: 0;
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));

        @include bp.up(medium) {
          font-size: var(--tch-font-size-headline-sm, 1.75rem);
        }
      }

      .rules__body {
        margin: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.6;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        max-width: 56ch;
      }

      /* ── Actions ── */

      .rules__actions {
        display: grid;
        gap: 0.5rem;
        align-content: start;
        justify-items: start;
      }

      .rules__cta-note {
        margin: 0;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
        opacity: 0.7;
      }
    `,
  ],
})
export class RulesSimulationWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly badgeKey = computed(() => stringProp(this.config(), 'badgeKey') ?? 'public.rules.badge');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.rules.title');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? 'public.rules.subtitle');
  readonly ctaNoteKey = computed(() => stringProp(this.config(), 'ctaNoteKey') ?? 'public.rules.cta_note');
  readonly primaryAction = computed(() => actionFrom(this.config()?.props?.['primaryAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
