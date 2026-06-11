import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-tchala-search-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="tchala">
      @if (badgeKey(); as bk) {
        <span class="tchala__badge">{{ bk | tchLabel }}</span>
      }
      <h2 class="tchala__title">{{ titleKey() | tchLabel }}</h2>
      <p class="tchala__body">{{ subtitleKey() | tchLabel }}</p>
      @if (ctaAction(); as action) {
        <a tch-action class="tchala__cta" [attr.href]="href(action)">
          {{ labelKey(action) | tchLabel }}
        </a>
      } @else {
        <a tch-action class="tchala__cta" href="/public/tchala">
          {{ 'home.tchala.discover' | tchLabel }}
        </a>
      }
    </section>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      .tchala {
        display: grid;
        align-content: start;
        gap: 0.625rem;
        padding: 1.25rem;
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));

        @include bp.up(medium) {
          padding: 1.75rem;
          gap: 0.75rem;
        }

        @include bp.up(expanded) {
          padding: 2rem;
        }
      }

      /* ── Badge ── */

      .tchala__badge {
        justify-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.75rem;
        background: var(--tch-color-tertiary-container, var(--mat-sys-tertiary-container));
        color: var(--tch-color-on-tertiary-container, var(--mat-sys-on-tertiary-container));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      /* ── Text ── */

      .tchala__title {
        margin: 0;
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));

        @include bp.up(medium) {
          font-size: var(--tch-font-size-headline-sm, 1.75rem);
        }
      }

      .tchala__body {
        margin: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.6;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        max-width: 52ch;
      }

      /* ── CTA ── */

      a.tchala__cta {
        --comp-action-bg: var(--tch-color-primary-container, var(--mat-sys-primary-container));
        --comp-action-fg: var(--tch-color-on-primary-container, var(--mat-sys-on-primary-container));
        margin-top: 0.25rem;
        justify-self: start;

        @include bp.up(medium) {
          margin-top: 0.5rem;
        }
      }
    `,
  ],
})
export class TchalaSearchWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly badgeKey = computed(() => stringProp(this.config(), 'badgeKey') ?? 'home.tchala.badge');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'home.tchala.title');
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey') ?? 'home.tchala.subtitle');
  readonly ctaAction = computed(() => actionFrom(this.config()?.props?.['ctaAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
