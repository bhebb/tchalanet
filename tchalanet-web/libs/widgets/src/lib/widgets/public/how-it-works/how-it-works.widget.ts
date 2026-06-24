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
  templateUrl: './how-it-works.widget.html',
  styleUrl: './how-it-works.widget.scss',
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
