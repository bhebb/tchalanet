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
  templateUrl: './business-process.widget.html',
  styleUrl: './business-process.widget.scss',
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
