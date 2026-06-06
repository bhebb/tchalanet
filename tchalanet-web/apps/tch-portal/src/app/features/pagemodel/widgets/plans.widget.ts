import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { stringProp } from '../widget.contract';

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
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="plans">
      <h2 class="plans__title">{{ titleKey() | tchLabel }}</h2>
      @if (plans().length) {
        <ul class="plans__grid">
          @for (plan of plans(); track plan.id ?? plan.code ?? $index) {
            <li class="plans__card" [class.plans__card--highlight]="plan.highlighted">
              <h3 class="plans__name">{{ plan.name || (plan.nameKey | tchLabel) || plan.code }}</h3>
              @if (plan.price !== null && plan.price !== undefined) {
                <span class="plans__price">{{ plan.price }}</span>
              }
            </li>
          }
        </ul>
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
      .plans__grid {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 1rem;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      }
      .plans__card {
        display: grid;
        gap: 0.5rem;
        padding: 1.25rem;
        border-radius: var(--tch-radius-control, 12px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
        border: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
      }
      .plans__card--highlight {
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
        box-shadow: 0 0 0 1px var(--tch-color-primary, var(--mat-sys-primary));
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
