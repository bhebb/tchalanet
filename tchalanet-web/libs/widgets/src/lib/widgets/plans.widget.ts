import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TchPubCard, TchPubCardGrid } from '@tch/ui/components';

import { LabelPipe, WidgetConfig, stringProp } from '@tch/page-model';

interface PlanItem {
  readonly id?: string;
  readonly code?: string;
  readonly name?: string;
  readonly nameKey?: string;
  readonly price?: string | number;
  readonly highlighted?: boolean;
}

interface PlansDynamic {
  readonly plans?: readonly PlanItem[];
}

/** `PlansWidget`: grid of active subscription plans from the `public_home` source (`{ plans }`). */
@Component({
  selector: 'tch-plans-widget',
  imports: [LabelPipe, TchPubCard, TchPubCardGrid],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="plans">
      <h2 class="plans__title">{{ titleKey() | tchLabel }}</h2>
      @if (plans().length) {
        <tch-public-card-grid style="--pub-card-grid-min: 200px">
          @for (plan of plans(); track plan.id ?? plan.code ?? $index) {
            <tch-public-card [tone]="plan.highlighted ? 'primary' : 'default'">
              <h3 class="plans__name">{{ plan.name || (plan.nameKey | tchLabel) || plan.code }}</h3>
              @if (plan.price !== null && plan.price !== undefined) {
                <span class="plans__price">{{ plan.price }}</span>
              }
            </tch-public-card>
          }
        </tch-public-card-grid>
      } @else {
        <p class="plans__empty">{{ 'home.plans.empty' | tchLabel }}</p>
      }
    </section>
  `,
  styles: [
    `
      .plans {
        display: grid;
        gap: 1rem;
        padding: 1.5rem;
      }
      .plans__title {
        margin: 0;
        font-size: 1.25rem;
      }
      .plans__name {
        margin: 0;
        font-size: 1rem;
      }
      .plans__price {
        font-weight: 700;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }
    `,
  ],
})
export class PlansWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'home.plans.title');
  readonly plans = computed<readonly PlanItem[]>(
    () => (this.dynamic() as PlansDynamic)?.plans ?? [],
  );
}
