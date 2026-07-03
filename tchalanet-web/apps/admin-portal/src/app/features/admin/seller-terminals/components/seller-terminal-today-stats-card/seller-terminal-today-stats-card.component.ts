import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { TchErrorPanel } from '@tch/ui/components';
import { ErrorViewModel } from '@tch/web/errors';

import { SellerTerminalDailyFinancialRow } from '../../../financials/data-access/admin-financials-api.service';

@Component({
  selector: 'tch-seller-terminal-today-stats-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, TchErrorPanel, TranslatePipe],
  templateUrl: './seller-terminal-today-stats-card.component.html',
  styleUrls: ['./seller-terminal-today-stats-card.component.scss'],
})
export class SellerTerminalTodayStatsCardComponent {
  readonly stats = input.required<SellerTerminalDailyFinancialRow>();
  readonly error = input<ErrorViewModel | null>(null);
  readonly retry = output<void>();
}
