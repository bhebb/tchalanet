import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface ProcessStep {
  id: string;
  number: number;
  titleKey: string;
  descriptionKey: string;
}

@Component({
  selector: 'tch-public-business-process-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pbprocess">
      <h2 class="pbprocess__title">{{ titleKey() | tchLabel }}</h2>

      <ol class="pbprocess__steps" role="list">
        @for (step of steps(); track step.id) {
          <li class="pbprocess__step">
            <div class="pbprocess__step-number" aria-hidden="true">{{ step.number }}</div>
            <div class="pbprocess__step-body">
              <h3 class="pbprocess__step-title">{{ step.titleKey | tchLabel }}</h3>
              <p class="pbprocess__step-desc">{{ step.descriptionKey | tchLabel }}</p>
            </div>
          </li>
        }
      </ol>
    </section>
  `,
  styles: [
    `
      @use 'breakpoints' as bp;

      .pbprocess {
        padding: 3rem 1.25rem;
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));

        @include bp.up(medium) {
          padding: 4rem 2rem;
        }

        @include bp.up(expanded) {
          padding: 5rem clamp(2rem, 6vw, 5rem);
        }
      }

      .pbprocess__title {
        margin: 0 0 2.5rem;
        text-align: center;
        font-size: clamp(1.5rem, 3vw, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbprocess__steps {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 1.5rem;

        @include bp.up(expanded) {
          grid-template-columns: repeat(4, 1fr);
          gap: 1.25rem;
        }
      }

      .pbprocess__step {
        display: flex;
        gap: 1.25rem;
        align-items: flex-start;
        padding: 2rem 1.75rem;
        border-radius: var(--tch-radius-lg, 16px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        min-height: 10rem;

        @include bp.up(expanded) {
          flex-direction: column;
          gap: 1rem;
        }
      }

      .pbprocess__step-number {
        flex-shrink: 0;
        width: 2.5rem;
        height: 2.5rem;
        border-radius: 50%;
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 1rem;
        font-weight: 800;
      }

      .pbprocess__step-body {
        display: grid;
        gap: 0.5rem;
      }

      .pbprocess__step-title {
        margin: 0;
        font-size: 1rem;
        font-weight: 700;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pbprocess__step-desc {
        margin: 0;
        font-size: 0.9375rem;
        line-height: 1.65;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }
    `,
  ],
})
export class PublicBusinessProcessWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly steps = computed<ProcessStep[]>(() => {
    const raw = this.config().props?.['steps'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((s) => ({
      id: String(s['id'] ?? ''),
      number: Number(s['number'] ?? 0),
      titleKey: String(s['titleKey'] ?? ''),
      descriptionKey: String(s['descriptionKey'] ?? ''),
    }));
  });
}
