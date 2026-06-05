import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';

@Component({
  selector: 'tch-rules-simulation-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="rules">
      <div class="rules__intro">
        <span>{{ 'public.rules.badge' | tchLabel }}</span>
        <h1>{{ 'public.rules.title' | tchLabel }}</h1>
        <p>{{ 'public.rules.subtitle' | tchLabel }}</p>
      </div>
      <div class="rules__panel" role="status">
        <h2>{{ 'public.rules.simulation_title' | tchLabel }}</h2>
        <p>{{ 'public.rules.simulation_unavailable' | tchLabel }}</p>
        <p class="rules__disclaimer">{{ 'public.rules.disclaimer' | tchLabel }}</p>
      </div>
    </section>
  `,
  styles: [
    `
      .rules {
        display: grid;
        gap: 1rem;
        padding: clamp(1.25rem, 4vw, 2rem);
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container));
      }
      .rules__intro,
      .rules__panel {
        display: grid;
        gap: 0.75rem;
      }
      .rules h1,
      .rules h2,
      .rules p {
        margin: 0;
      }
      .rules__intro span {
        justify-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.625rem;
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }
      .rules__panel {
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }
      .rules__disclaimer {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
      }
    `,
  ],
})
export class RulesSimulationWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');
}
