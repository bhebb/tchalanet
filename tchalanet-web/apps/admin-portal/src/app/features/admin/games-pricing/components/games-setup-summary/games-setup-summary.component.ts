import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export interface GamesSetupSummaryItem {
  readonly labelKey: string;
  readonly value: number;
  readonly helpKey: string;
  readonly tone?: 'neutral' | 'success' | 'warning';
}

@Component({
  selector: 'tch-games-setup-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './games-setup-summary.component.html',
  styleUrls: ['./games-setup-summary.component.scss'],
})
export class GamesSetupSummaryComponent {
  readonly items = input.required<readonly GamesSetupSummaryItem[]>();
}
