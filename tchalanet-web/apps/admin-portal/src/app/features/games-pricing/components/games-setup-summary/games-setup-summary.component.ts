import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminMetricCardComponent, AdminMetricCardTone } from '@tch/ui/console';

export interface GamesSetupSummaryItem {
  readonly labelKey: string;
  readonly value: number;
  readonly helpKey: string;
  readonly tone?: 'neutral' | 'success' | 'warning';
}

/** Local tone vocabulary → the shared card's. */
const TONES: Readonly<Record<string, AdminMetricCardTone>> = {
  neutral: 'default',
  success: 'positive',
  warning: 'warning',
};

@Component({
  selector: 'tch-games-setup-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminMetricCardComponent, TranslatePipe],
  styles: [':host { display: block; margin-block: 1rem 1.25rem; }'],
  templateUrl: './games-setup-summary.component.html',
})
export class GamesSetupSummaryComponent {
  readonly items = input.required<readonly GamesSetupSummaryItem[]>();

  cardTone(item: GamesSetupSummaryItem): AdminMetricCardTone {
    return TONES[item.tone ?? 'neutral'] ?? 'default';
  }
}
