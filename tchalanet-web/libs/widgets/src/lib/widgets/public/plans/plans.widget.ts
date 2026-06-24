import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TchPubCard, TchPubCardGrid } from '@tch/ui/components';

import { LabelPipe, WidgetConfig, stringProp } from '@tch/page-model';

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

@Component({
  selector: 'tch-plans-widget',
  imports: [LabelPipe, TchPubCard, TchPubCardGrid],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './plans.widget.html',
  styleUrl: './plans.widget.scss',
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
