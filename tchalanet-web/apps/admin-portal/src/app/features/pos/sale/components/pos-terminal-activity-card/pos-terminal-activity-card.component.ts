import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { PosTerminalActivityView } from '../../data-access/pos-sale.models';

@Component({
  selector: 'tch-pos-terminal-activity-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe],
  templateUrl: './pos-terminal-activity-card.component.html',
  styleUrls: ['./pos-terminal-activity-card.component.scss'],
})
export class PosTerminalActivityCardComponent {
  readonly activity = input<PosTerminalActivityView | null>(null);
  readonly loading = input(false);
}
