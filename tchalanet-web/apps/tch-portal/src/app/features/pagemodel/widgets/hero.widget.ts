import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import {
  actionsFrom,
  destinationHref,
  stringProp,
  WidgetAction,
} from '../widget.contract';

interface HeroDynamic {
  readonly title_key?: string;
  readonly tagline_key?: string;
  readonly subtitle_key?: string;
  readonly cta_key?: string;
  readonly cta_path?: string;
  readonly actions?: readonly unknown[];
}

/**
 * `HeroWidget`: strong hero with a primary action. Title/subtitle/CTA come from the widget props
 * (`*_key`) with an optional dynamic (json_file) override. Styled only via theme tokens.
 */
@Component({
  selector: 'tch-hero-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hero">
      <div class="hero__copy">
        @if (taglineKey()) {
          <p class="hero__eyebrow">{{ taglineKey() | tchLabel }}</p>
        }
        <h1 class="hero__title">{{ titleKey() | tchLabel }}</h1>
        @if (subtitleKey()) {
          <p class="hero__subtitle">{{ subtitleKey() | tchLabel }}</p>
        }
        @if (actions().length) {
          <div class="hero__actions">
            @for (action of actions(); track action.id ?? action.label_key) {
              <a
                class="hero__cta"
                [class.hero__cta--secondary]="action.style !== 'primary'"
                [attr.href]="href(action)"
              >
                {{ action.label ?? (action.label_key | tchLabel) }}
              </a>
            }
          </div>
        } @else if (ctaKey()) {
          <a class="hero__cta" [attr.href]="ctaPath()">{{ ctaKey() | tchLabel }}</a>
        }
      </div>

      <div class="hero__visual" aria-hidden="true">
        <img
          class="hero__ticket"
          src="/assets/public/ticket-verification-preview.svg"
          alt=""
          loading="eager"
        />
        <div class="hero__quick-check">
          <div class="hero__quick-head">
            <span>{{ 'public.ticket.code_label' | tchLabel }}</span>
            <span>QR</span>
          </div>
          <div class="hero__quick-code">{{ 'public.ticket.placeholder' | tchLabel }}</div>
          <div class="hero__quick-note">{{ 'public.ticket.description' | tchLabel }}</div>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .hero {
        position: relative;
        overflow: hidden;
        display: grid;
        grid-template-columns: minmax(0, 1fr) minmax(16rem, 24rem);
        align-items: center;
        gap: 1rem;
        min-height: clamp(24rem, 52vw, 34rem);
        padding: clamp(2rem, 8vw, 5rem) clamp(1.25rem, 5vw, 4rem);
        border-radius: var(--tch-radius-xl, 24px);
        background:
          radial-gradient(
            circle at 88% 8%,
            color-mix(
                in oklab,
                var(--tch-color-primary-container, var(--mat-sys-primary-container)) 58%,
                var(--tch-color-on-primary, var(--mat-sys-on-primary)) 0%
              )
              0,
            transparent 18rem
          ),
          linear-gradient(
            145deg,
            var(--tch-color-primary, var(--mat-sys-primary)) 0%,
            var(--tch-color-primary-container, var(--mat-sys-primary-container)) 100%
          );
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }
      .hero__copy {
        position: relative;
        z-index: 1;
        display: grid;
        gap: 1rem;
      }
      .hero__eyebrow {
        margin: 0;
        color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: var(--tch-line-height-label-sm, 1rem);
        font-weight: 700;
        text-transform: uppercase;
      }
      .hero__title {
        margin: 0;
        color: inherit;
        font-size: clamp(2rem, 6vw, var(--tch-font-size-display-lg, 2.5rem));
        line-height: var(--tch-line-height-display-lg, 3rem);
        max-width: 46rem;
      }
      .hero__subtitle {
        margin: 0;
        max-width: 42rem;
        color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
      }
      .hero__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }
      .hero__cta {
        justify-self: start;
        min-height: var(--tch-touch-target, 48px);
        display: inline-flex;
        align-items: center;
        padding: 0 1.25rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        text-decoration: none;
        font-weight: 600;
      }
      .hero__cta--secondary {
        background: transparent;
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        border: 1px solid color-mix(in oklab, currentColor 40%, transparent);
      }
      .hero__visual {
        position: relative;
        z-index: 1;
        display: grid;
        justify-items: center;
        gap: 1rem;
      }
      .hero__ticket {
        width: min(100%, 18rem);
        filter: drop-shadow(0 22px 34px color-mix(in oklab, var(--tch-color-on-surface, var(--mat-sys-on-surface)) 32%, transparent));
        transform: rotate(-2deg);
      }
      .hero__quick-check {
        width: min(100%, 20rem);
        display: none;
        gap: 0.75rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        box-shadow: var(--mat-sys-level3, 0 14px 32px color-mix(in oklab, var(--tch-color-on-surface, var(--mat-sys-on-surface)) 22%, transparent));
      }
      .hero__quick-head {
        display: flex;
        justify-content: space-between;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: var(--tch-line-height-label-sm, 1rem);
        font-weight: 800;
        text-transform: uppercase;
      }
      .hero__quick-code {
        padding: 0.875rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 1.125rem;
        font-weight: 800;
        text-align: center;
        letter-spacing: 0.08em;
      }
      .hero__quick-note {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: 0.75rem;
        text-align: center;
      }
      @media (max-width: 720px) {
        .hero {
          grid-template-columns: 1fr;
          min-height: auto;
          justify-items: center;
          text-align: center;
          padding: 2rem var(--tch-page-margin-mobile, 16px) 2.25rem;
          border-radius: 0 0 var(--tch-radius-xl, 24px) var(--tch-radius-xl, 24px);
        }
        .hero__copy {
          justify-items: center;
        }
        .hero__title {
          font-size: var(--tch-font-size-headline-mobile, 1.5rem);
          line-height: var(--tch-line-height-headline-mobile, 2rem);
          max-width: 20rem;
        }
        .hero__subtitle {
          max-width: 21rem;
        }
        .hero__ticket {
          width: min(62vw, 15rem);
          order: -1;
        }
        .hero__actions {
          width: min(100%, 20rem);
        }
        .hero__cta {
          width: 100%;
          justify-content: center;
        }
        .hero__cta--secondary {
          display: none;
        }
      }
      @media (min-width: 520px) and (max-width: 720px) {
        .hero__quick-check {
          display: grid;
        }
      }
    `,
  ],
})
export class HeroWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  private readonly data = computed<HeroDynamic>(() => (this.dynamic() as HeroDynamic) ?? {});

  readonly titleKey = computed(
    () => this.data().title_key ?? stringProp(this.config(), 'title_key') ?? 'home.hero.title',
  );
  readonly taglineKey = computed(() => this.data().tagline_key ?? stringProp(this.config(), 'tagline_key'));
  readonly subtitleKey = computed(
    () => this.data().subtitle_key ?? stringProp(this.config(), 'subtitle_key'),
  );
  readonly ctaKey = computed(() => this.data().cta_key ?? stringProp(this.config(), 'cta_key'));
  readonly ctaPath = computed(
    () => this.data().cta_path ?? stringProp(this.config(), 'cta_path') ?? '#',
  );
  readonly actions = computed(() => publicHeroActions(this.widgetId(), actionsFrom(this.data().actions)));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }
}

const HOME_HERO_ACTIONS: readonly WidgetAction[] = [
  {
    id: 'public-check-ticket',
    label_key: 'public.hero.primary_action',
    style: 'primary',
    destination: { type: 'path', path: '/public/check-ticket' },
  },
  {
    id: 'public-results',
    label_key: 'public.hero.secondary_action',
    style: 'secondary',
    destination: { type: 'path', path: '/public/results' },
  },
];

function publicHeroActions(
  widgetId: string,
  actions: readonly WidgetAction[],
): readonly WidgetAction[] {
  if (widgetId === 'home.hero') {
    return HOME_HERO_ACTIONS;
  }
  return actions.filter(action => action.id !== 'LOGIN');
}
