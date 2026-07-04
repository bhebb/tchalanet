import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { SellerTerminalsSummary } from '../../data-access/seller-terminal-api.service';

@Component({
  selector: 'tch-seller-terminal-kpi-strip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, TranslatePipe],
  templateUrl: './seller-terminal-kpi-strip.component.html',
  styleUrls: ['./seller-terminal-kpi-strip.component.scss'],
})
export class SellerTerminalKpiStripComponent {
  readonly summary = input.required<SellerTerminalsSummary>();
}
