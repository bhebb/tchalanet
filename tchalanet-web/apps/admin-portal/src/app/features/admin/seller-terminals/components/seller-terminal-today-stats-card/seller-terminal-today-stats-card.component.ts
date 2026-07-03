import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { SellerTerminalDailyFinancialRow } from '../../../financials/data-access/admin-financials-api.service';

@Component({
  selector: 'tch-seller-terminal-today-stats-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, TranslatePipe],
  templateUrl: './seller-terminal-today-stats-card.component.html',
  styleUrls: ['./seller-terminal-today-stats-card.component.scss'],
})
export class SellerTerminalTodayStatsCardComponent {
  readonly stats = input.required<SellerTerminalDailyFinancialRow>();
}
