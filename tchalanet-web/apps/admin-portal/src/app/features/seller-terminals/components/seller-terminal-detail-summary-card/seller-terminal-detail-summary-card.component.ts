import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';

import {
  SellerTerminalStatus,
  SellerTerminalView,
} from '../../data-access/seller-terminal-api.service';

@Component({
  selector: 'tch-seller-terminal-detail-summary-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchStatusBadge, TranslatePipe],
  templateUrl: './seller-terminal-detail-summary-card.component.html',
  styleUrls: ['./seller-terminal-detail-summary-card.component.scss'],
})
export class SellerTerminalDetailSummaryCardComponent {
  readonly terminal = input.required<SellerTerminalView>();

  statusBadge(status: SellerTerminalStatus): BadgeStatus {
    if (status === 'ACTIVE') return 'ready';
    if (status === 'BLOCKED') return 'blocked';
    if (status === 'PENDING') return 'pending';
    return 'missing';
  }
}
