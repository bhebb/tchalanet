import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';

@Component({
  selector: 'tch-how-it-works-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="how">
      <h2>{{ 'public.how.title' | tchLabel }}</h2>
      <p>{{ 'public.how.description' | tchLabel }}</p>
      <ol class="how__steps">
        @for (step of [1, 2, 3, 4]; track step) {
          <li>
            <span>{{ step }}</span>
            <div>
              <h3>{{ 'public.how.step' + step + '.title' | tchLabel }}</h3>
              <p>{{ 'public.how.step' + step + '.text' | tchLabel }}</p>
            </div>
          </li>
        }
      </ol>
    </section>
  `,
  styles: [
    `
      .how {
        display: grid;
        gap: 1rem;
        padding: 2rem var(--tch-page-margin-mobile, 16px);
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container));
      }
      .how h2,
      .how h3,
      .how p {
        margin: 0;
      }
      .how h2 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }
      .how__steps {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 1rem;
      }
      .how__steps li {
        display: grid;
        gap: 0.75rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }
      .how__steps span {
        width: 2.5rem;
        height: 2.5rem;
        display: grid;
        place-items: center;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-weight: 800;
      }
    `,
  ],
})
export class HowItWorksWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');
}
