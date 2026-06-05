import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { isRecord } from '../widget.contract';

@Component({
  selector: 'tch-tchala-search-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="tchala">
      <h2>{{ titleKey() | tchLabel }}</h2>
      <p>{{ subtitleKey() | tchLabel }}</p>
      <a href="/public/help">{{ 'public.help.title' | tchLabel }}</a>
    </section>
  `,
  styles: [
    `
      .tchala {
        display: grid;
        gap: 0.5rem;
        padding: 1.25rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }
      .tchala h2,
      .tchala p {
        margin: 0;
      }
      .tchala a {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-weight: 800;
      }
    `,
  ],
})
export class TchalaSearchWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');
  readonly titleKey = computed(() => keyFromDynamic(this.dynamic(), 'title_key') ?? 'public.help.title');
  readonly subtitleKey = computed(() => keyFromDynamic(this.dynamic(), 'subtitle_key') ?? 'public.help.subtitle');
}

function keyFromDynamic(value: unknown, key: string): string | undefined {
  return isRecord(value) && typeof value[key] === 'string' ? value[key] : undefined;
}
