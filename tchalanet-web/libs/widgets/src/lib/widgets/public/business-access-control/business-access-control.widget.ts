import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface AccessItem {
  id: string;
  icon: string;
  titleKey: string;
  descriptionKey: string;
}

@Component({
  selector: 'tch-public-business-access-control-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './business-access-control.widget.html',
  styleUrl: './business-access-control.widget.scss',
})
export class PublicBusinessAccessControlWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly descriptionKey = computed(() => stringProp(this.config(), 'descriptionKey'));

  readonly items = computed<AccessItem[]>(() => {
    const raw = this.config().props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((i) => ({
      id: String(i['id'] ?? ''),
      icon: String(i['icon'] ?? 'lock'),
      titleKey: String(i['titleKey'] ?? ''),
      descriptionKey: String(i['descriptionKey'] ?? ''),
    }));
  });
}
