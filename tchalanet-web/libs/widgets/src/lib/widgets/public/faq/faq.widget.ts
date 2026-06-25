import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface FaqItem {
  id: string;
  qKey: string;
  aKey: string;
}

@Component({
  selector: 'tch-faq-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './faq.widget.html',
  styleUrl: './faq.widget.scss',
})
export class FaqWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  private readonly openId = signal<string | null>(null);

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly items = computed<FaqItem[]>(() => {
    const raw = this.config().props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((i) => ({
      id: String(i['id'] ?? ''),
      qKey: String(i['qKey'] ?? ''),
      aKey: String(i['aKey'] ?? ''),
    }));
  });

  isOpen(id: string): boolean {
    return this.openId() === id;
  }

  toggle(id: string): void {
    this.openId.update((current) => (current === id ? null : id));
  }
}
