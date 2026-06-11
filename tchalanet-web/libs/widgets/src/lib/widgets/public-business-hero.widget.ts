import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { actionFrom, destinationHref, isRecord, stringProp, WidgetAction, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface HeroStat {
  id: string;
  icon: string;
  labelKey: string;
  value: string;
  highlight?: boolean;
}

@Component({
  selector: 'tch-public-business-hero-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pbhero__content">
      @if (eyebrowKey(); as ek) {
        <p class="pbhero__eyebrow">{{ ek | tchLabel }}</p>
      }
      <h1 class="pbhero__title">{{ titleKey() | tchLabel }}</h1>
      <p class="pbhero__description">{{ descriptionKey() | tchLabel }}</p>

      <div class="pbhero__actions">
        @if (primaryAction(); as action) {
          <a class="pbhero__btn pbhero__btn--primary" [attr.href]="href(action)">
            {{ action.labelKey | tchLabel }}
          </a>
        }
        @if (secondaryAction(); as action) {
          <a class="pbhero__btn pbhero__btn--secondary" [attr.href]="href(action)">
            {{ action.labelKey | tchLabel }}
          </a>
        }
      </div>
    </div>

    <div class="pbhero__visual" aria-hidden="true">
      <div class="pbhero__stats-card">
        @for (stat of stats(); track stat.id) {
          <div class="pbhero__stat" [class.pbhero__stat--highlight]="stat.highlight">
            <span class="pbhero__stat-icon material-symbols-outlined">{{ stat.icon }}</span>
            <div class="pbhero__stat-text">
              <span class="pbhero__stat-value">{{ stat.value }}</span>
              <span class="pbhero__stat-label">{{ stat.labelKey | tchLabel }}</span>
            </div>
          </div>
        }
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

      .pbhero__content {
        display: grid;
        gap: 1.25rem;
        align-content: start;

        @include bp.up(expanded) {
          gap: 1.5rem;
        }
      }

      .pbhero__eyebrow {
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

      .pbhero__title {
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

      .pbhero__description {
        margin: 0;
        max-width: 36rem;
        color: color-mix(in oklab, var(--tch-color-on-primary, var(--mat-sys-on-primary)) 80%, transparent);
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.65;
      }

      /* ── Actions ── */

      .pbhero__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;

        @include bp.down(medium) {
          flex-direction: column;

          .pbhero__btn {
            width: 100%;
            box-sizing: border-box;
            text-align: center;
          }
        }
      }

      .pbhero__btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.625rem 1.5rem;
        font-size: 0.9375rem;
        font-weight: 600;
        text-decoration: none;
        transition: opacity 0.15s;

        &:hover { opacity: 0.85; }
      }

      .pbhero__btn--primary {
        background: var(--tch-color-accent, #fecb00);
        color: #1a1a1a;
      }

      .pbhero__btn--secondary {
        background: transparent;
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        border: 1.5px solid color-mix(in oklab, var(--tch-color-on-primary, var(--mat-sys-on-primary)) 40%, transparent);
      }

      /* ── Visual / stats card ── */

      .pbhero__visual {
        display: flex;
        justify-content: center;
        align-items: center;
        max-width: 100%;

        @include bp.up(expanded) {
          justify-self: center;
        }
      }

      .pbhero__stats-card {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1px;
        width: min(100%, 26rem);
        border-radius: var(--tch-radius-xl, 20px);
        overflow: hidden;
        background: var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        box-shadow:
          0 4px 6px -1px color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 20%, transparent),
          0 20px 48px -8px color-mix(in oklab, var(--tch-color-primary, var(--mat-sys-primary)) 32%, transparent);

        @include bp.up(expanded) {
          transform: rotate(-1.5deg);
        }
      }

      .pbhero__stat {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1.25rem;
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbhero__stat--highlight {
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .pbhero__stat-icon {
        font-size: 1.5rem;
        flex-shrink: 0;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .pbhero__stat--highlight .pbhero__stat-icon {
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        opacity: 0.8;
      }

      .pbhero__stat-text {
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
      }

      .pbhero__stat-value {
        font-size: 1.375rem;
        font-weight: 800;
        line-height: 1;
        color: inherit;
      }

      .pbhero__stat-label {
        font-size: 0.725rem;
        line-height: 1.3;
        color: inherit;
        opacity: 0.75;
      }
    `,
  ],
})
export class PublicBusinessHeroWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly eyebrowKey = computed(() => stringProp(this.config(), 'eyebrowKey'));
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? '');
  readonly primaryAction = computed(() => actionFrom(this.config().props?.['primaryAction']));
  readonly secondaryAction = computed(() => actionFrom(this.config().props?.['secondaryAction']));

  readonly stats = computed<HeroStat[]>(() => {
    const raw = this.config().props?.['stats'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((s) => ({
      id: String(s['id'] ?? ''),
      icon: String(s['icon'] ?? 'star'),
      labelKey: String(s['labelKey'] ?? ''),
      value: String(s['value'] ?? ''),
      highlight: s['highlight'] === true,
    }));
  });

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }
}
