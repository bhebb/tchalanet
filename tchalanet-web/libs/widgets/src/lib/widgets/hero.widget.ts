import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import {
  actionsFrom,
  destinationHref,
  isRecord,
  stringProp,
  stringValue,
  WidgetAction,
} from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

/**
 * `HeroWidget` — section d'entrée de la page publique.
 * Mobile-first : copy (eyebrow → titre → description → actions) puis visuel ticket.
 * Desktop : deux colonnes [copy | visuel].
 * Props lues depuis config.props (camelCase), actions depuis config.props.actions.
 */
@Component({
  selector: 'tch-hero-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="hero-widget__content">
      @if (eyebrowKey()) {
        <p class="hero-widget__eyebrow">{{ eyebrowKey() | tchLabel }}</p>
      }
      <h1 class="hero-widget__title">{{ titleKey() | tchLabel }}</h1>
      @if (descriptionKey()) {
        <p class="hero-widget__description">{{ descriptionKey() | tchLabel }}</p>
      }
      @if (primaryAction() || secondaryActions().length) {
        <div class="hero-widget__actions">
          @if (primaryAction(); as action) {
            <a
              tch-action
              variant="primary"
              [attr.href]="href(action)"
              style="--comp-action-bg: var(--tch-color-accent, #fecb00); --comp-action-fg: #1a1a1a;"
            >
              {{ action.label ?? (action.labelKey | tchLabel) }}
            </a>
          }
          @for (action of secondaryActions(); track action.id ?? action.labelKey) {
            <a
              tch-action
              variant="tertiary"
              [attr.href]="href(action)"
              style="color: var(--tch-color-on-primary, #fff); --comp-action-outline: color-mix(in oklab, var(--tch-color-on-primary, #fff) 40%, transparent);"
            >
              {{ action.label ?? (action.labelKey | tchLabel) }}
            </a>
          }
        </div>
      }
    </div>

    <div class="hero-widget__visual" aria-hidden="true">
      <div class="hero-widget__ticket-card">
        <div class="hero-widget__ticket-header">
          <span class="hero-widget__ticket-label">{{ 'domain.ticket.field.publicCode' | tchLabel }}</span>
          <span class="hero-widget__ticket-qr-icon" aria-hidden="true">▣</span>
        </div>
        <div class="hero-widget__ticket-code">{{ ticketCodeLiteral() ?? ('public.check.placeholder' | tchLabel) }}</div>
        <div class="hero-widget__ticket-status-row">
          <span class="hero-widget__ticket-badge">{{ statusLabelKey() | tchLabel }}</span>
        </div>
        <p class="hero-widget__ticket-note">{{ helperTextKey() | tchLabel }}</p>
      </div>
    </div>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      :host {
        display: grid;
        gap: 1.5rem;
        padding: 2rem 1rem 2.5rem;
        overflow: hidden;
        border-radius: 0 0 var(--tch-radius-xl, 24px) var(--tch-radius-xl, 24px);
        background:
          radial-gradient(
            ellipse at 80% 0%,
            color-mix(in oklab, var(--tch-color-primary-container, var(--mat-sys-primary-container)) 60%, transparent) 0%,
            transparent 50%
          ),
          linear-gradient(
            150deg,
            var(--tch-color-primary, var(--mat-sys-primary)) 0%,
            color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 80%, var(--tch-color-primary-container, var(--mat-sys-primary-container))) 100%
          );
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));

        @include bp.up(expanded) {
          grid-template-columns: minmax(0, 0.9fr) minmax(22rem, 1.1fr);
          align-items: center;
          min-height: clamp(26rem, 36vw, 34rem);
          padding: clamp(3rem, 5vw, 4.5rem) clamp(2rem, 4.5vw, 4rem);
          border-radius: var(--tch-radius-xl, 24px);
          gap: clamp(2rem, 4vw, 3.5rem);
        }
      }

      /* ── Content ── */

      .hero-widget__content {
        display: grid;
        gap: 1.25rem;
        align-content: start;

        @include bp.up(expanded) {
          gap: 1.5rem;
        }
      }

      .hero-widget__eyebrow {
        margin: 0;
        width: fit-content;
        padding: 0.3rem 0.875rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: color-mix(in oklab, var(--tch-color-on-primary, var(--mat-sys-on-primary)) 14%, transparent);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        letter-spacing: 0.05em;
        text-transform: uppercase;
        color: inherit;
      }

      .hero-widget__title {
        margin: 0;
        font-size: clamp(1.875rem, 5.5vw, var(--tch-font-size-display-md, 2.5rem));
        line-height: 1.15;
        font-weight: 800;
        color: inherit;
        max-width: 22rem;

        @include bp.up(expanded) {
          max-width: 38rem;
        }
      }

      .hero-widget__description {
        margin: 0;
        max-width: 36rem;
        color: color-mix(in oklab, var(--tch-color-on-primary, var(--mat-sys-on-primary)) 80%, transparent);
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.65;
      }

      /* ── Actions ── */

      .hero-widget__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;

        @include bp.down(medium) {
          flex-direction: column;

          a[tch-action] {
            width: 100%;
            max-width: 100%;
            box-sizing: border-box;
            min-width: 0;
          }
        }
      }

      /* Ghost secondary — visible on dark hero background */
      .hero-widget__secondary-cta {
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        --comp-action-outline: color-mix(in oklab, var(--tch-color-on-primary, var(--mat-sys-on-primary)) 40%, transparent);
      }

      /* ── Visual ── */

      .hero-widget__visual {
        display: flex;
        justify-content: center;
        align-items: center;
        max-width: 100%;

        @include bp.up(expanded) {
          justify-self: center;
        }
      }

      .hero-widget__ticket-card {
        display: grid;
        gap: 0.875rem;
        width: min(100%, 20rem);
        padding: 1.25rem;
        border-radius: var(--tch-radius-xl, 20px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        box-shadow:
          0 4px 6px -1px color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 20%, transparent),
          0 20px 48px -8px color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 32%, transparent);

        @include bp.up(expanded) {
          width: min(100%, 26rem);
          padding: 2rem;
          transform: rotate(-1.5deg);
        }
      }

      .hero-widget__ticket-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .hero-widget__ticket-label {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .hero-widget__ticket-qr-icon {
        font-size: 1.5rem;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        opacity: 0.5;
      }

      .hero-widget__ticket-code {
        padding: 0.875rem 1rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 1.25rem;
        font-weight: 800;
        letter-spacing: 0.1em;
        text-align: center;
      }

      .hero-widget__ticket-status-row {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .hero-widget__ticket-badge {
        display: inline-flex;
        align-items: center;
        padding: 0.25rem 0.75rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: color-mix(in oklab, var(--tch-color-accent, #fecb00) 18%, transparent);
        color: color-mix(in oklab, var(--tch-color-accent, #fecb00) 80%, var(--tch-color-on-surface, var(--mat-sys-on-surface)));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
      }

      .hero-widget__ticket-note {
        margin: 0;
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-align: center;
      }
    `,
  ],
})
export class HeroWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly eyebrowKey = computed(() => stringProp(this.config(), 'taglineKey'));
  readonly titleKey = computed(
    () => stringProp(this.config(), 'titleKey') ?? 'home.hero.title',
  );
  readonly descriptionKey = computed(
    () => stringProp(this.config(), 'subtitleKey') ?? 'home.hero.subtitle',
  );

  private readonly visual = computed(() => {
    const v = this.config()?.props?.['visual'];
    return isRecord(v) ? v : undefined;
  });

  readonly ticketCodeLiteral = computed(
    () => stringValue(this.visual()?.['ticketCode']),
  );
  readonly statusLabelKey = computed(
    () => stringValue(this.visual()?.['statusLabelKey']) ?? 'ticket.status.pending',
  );
  readonly helperTextKey = computed(
    () => stringValue(this.visual()?.['helperTextKey']) ?? 'public.check.description',
  );

  private readonly allActions = computed(() =>
    actionsFrom(this.config()?.props?.['actions']).filter(
      a => a.id !== 'LOGIN' && a.id !== 'login',
    ),
  );

  readonly primaryAction = computed(() => this.allActions()[0]);
  readonly secondaryActions = computed(() => this.allActions().slice(1));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }
}
