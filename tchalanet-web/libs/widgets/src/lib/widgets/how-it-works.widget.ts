import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { stringProp } from '@tch/page-model';

interface HowStep {
  readonly titleKey: string;
  readonly textKey: string;
}

@Component({
  selector: 'tch-how-it-works-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="how">
      <h2>{{ titleKey() | tchLabel }}</h2>
      <p>{{ descriptionKey() | tchLabel }}</p>
      <ol class="how__steps">
        @for (step of steps(); track step.titleKey) {
          <li>
            <span>{{ $index + 1 }}</span>
            <div>
              <h3>{{ step.titleKey | tchLabel }}</h3>
              <p>{{ step.textKey | tchLabel }}</p>
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
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.how.title');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey') ?? 'public.how.description');
  readonly steps = computed(() => readSteps(this.config().props?.['steps']));
}

function readSteps(value: unknown): readonly HowStep[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.flatMap((step) => {
    if (!step || typeof step !== 'object') {
      return [];
    }
    const record = step as Record<string, unknown>;
    return typeof record['titleKey'] === 'string' && typeof record['textKey'] === 'string'
      ? [{ titleKey: record['titleKey'], textKey: record['textKey'] }]
      : [];
  });
}
