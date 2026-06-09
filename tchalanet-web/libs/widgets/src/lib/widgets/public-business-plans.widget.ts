import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { actionFrom, destinationHref, isRecord, stringProp, WidgetAction, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface PlanItem {
  id: string;
  highlighted: boolean;
  badgeKey: string | null;
  nameKey: string;
  priceKey: string;
  descriptionKey: string;
  features: string[];
  action: WidgetAction | undefined;
}

@Component({
  selector: 'tch-public-business-plans-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pbplans">
      <div class="pbplans__header">
        <h2 class="pbplans__title">{{ titleKey() | tchLabel }}</h2>
        @if (subtitleKey(); as sk) {
          <p class="pbplans__subtitle">{{ sk | tchLabel }}</p>
        }
      </div>

      <ul class="pbplans__grid" role="list">
        @for (plan of plans(); track plan.id) {
          <li class="pbplans__plan" [class.pbplans__plan--highlighted]="plan.highlighted">
            @if (plan.badgeKey) {
              <span class="pbplans__badge">{{ plan.badgeKey | tchLabel }}</span>
            }
            <div class="pbplans__plan-header">
              <h3 class="pbplans__plan-name">{{ plan.nameKey | tchLabel }}</h3>
              <p class="pbplans__plan-price">{{ plan.priceKey | tchLabel }}</p>
              <p class="pbplans__plan-desc">{{ plan.descriptionKey | tchLabel }}</p>
            </div>
            <ul class="pbplans__features" role="list">
              @for (feat of plan.features; track feat) {
                <li class="pbplans__feature">
                  <span class="pbplans__feature-icon material-symbols-outlined" aria-hidden="true">check</span>
                  <span>{{ feat | tchLabel }}</span>
                </li>
              }
            </ul>
            @if (plan.action; as action) {
              <a
                class="pbplans__cta"
                [class.pbplans__cta--primary]="action.style === 'primary'"
                [attr.href]="href(action)"
              >
                {{ action.labelKey | tchLabel }}
              </a>
            }
          </li>
        }
      </ul>
    </section>
  `,
  styles: [
    `
      @use 'breakpoints' as bp;

      .pbplans {
        padding: 3rem 1.25rem;

        @include bp.up(medium) {
          padding: 4rem 2rem;
        }

        @include bp.up(expanded) {
          padding: 5rem clamp(2rem, 6vw, 5rem);
        }
      }

      .pbplans__header {
        text-align: center;
        display: grid;
        gap: 0.75rem;
        margin-bottom: 2.5rem;
      }

      .pbplans__title {
        margin: 0;
        font-size: clamp(1.5rem, 3vw, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbplans__subtitle {
        margin: 0 auto;
        max-width: 52ch;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.65;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .pbplans__grid {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 1.5rem;

        @include bp.up(medium) {
          grid-template-columns: repeat(2, 1fr);
        }

        @include bp.up(expanded) {
          grid-template-columns: repeat(4, 1fr);
          align-items: start;
        }
      }

      .pbplans__plan {
        position: relative;
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
        padding: 1.5rem;
        border-radius: var(--tch-radius-xl, 24px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface, var(--mat-sys-surface));
      }

      .pbplans__plan--highlighted {
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .pbplans__badge {
        position: absolute;
        top: -0.75rem;
        left: 50%;
        transform: translateX(-50%);
        white-space: nowrap;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.2rem 0.875rem;
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-size: 0.75rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .pbplans__plan-header {
        display: grid;
        gap: 0.25rem;
      }

      .pbplans__plan-name {
        margin: 0;
        font-size: 1.125rem;
        font-weight: 700;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbplans__plan--highlighted .pbplans__plan-name {
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .pbplans__plan-price {
        margin: 0;
        font-size: 1.375rem;
        font-weight: 800;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .pbplans__plan--highlighted .pbplans__plan-price {
        color: var(--tch-color-accent, #fecb00);
      }

      .pbplans__plan-desc {
        margin: 0;
        font-size: 0.875rem;
        line-height: 1.5;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .pbplans__plan--highlighted .pbplans__plan-desc {
        color: color-mix(in oklab, var(--tch-color-on-primary, var(--mat-sys-on-primary)) 85%, transparent);
      }

      .pbplans__features {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 0.5rem;
        flex: 1;
      }

      .pbplans__feature {
        display: flex;
        gap: 0.5rem;
        align-items: flex-start;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbplans__plan--highlighted .pbplans__feature {
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .pbplans__feature-icon {
        font-size: 1.125rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        flex-shrink: 0;
        margin-top: 0.0625rem;
      }

      .pbplans__plan--highlighted .pbplans__feature-icon {
        color: var(--tch-color-accent, #fecb00);
      }

      .pbplans__cta {
        display: block;
        text-align: center;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.75rem 1rem;
        font-size: 0.9375rem;
        font-weight: 600;
        text-decoration: none;
        transition: opacity 0.15s;
        background: transparent;
        border: 1.5px solid var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-primary, var(--mat-sys-primary));

        &:hover { opacity: 0.85; }
      }

      .pbplans__plan--highlighted .pbplans__cta {
        border-color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }

      .pbplans__cta--primary {
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        border-color: transparent;
      }

      .pbplans__plan--highlighted .pbplans__cta--primary {
        background: var(--tch-color-accent, #fecb00);
        color: #1a1a1a;
        border-color: transparent;
      }
    `,
  ],
})
export class PublicBusinessPlansWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey'));

  readonly plans = computed<PlanItem[]>(() => {
    const raw = this.config().props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((p) => ({
      id: String(p['id'] ?? ''),
      highlighted: p['highlighted'] === true,
      badgeKey: typeof p['badgeKey'] === 'string' ? p['badgeKey'] : null,
      nameKey: String(p['nameKey'] ?? ''),
      priceKey: String(p['priceKey'] ?? ''),
      descriptionKey: String(p['descriptionKey'] ?? ''),
      features: Array.isArray(p['features']) ? p['features'].map(String) : [],
      action: actionFrom(p['action']),
    }));
  });

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }
}
