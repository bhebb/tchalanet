import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import {
  ConsoleActorIdentity,
  ConsoleActorRowComponent,
  consoleSellerTerminalActorIdentity,
} from '@tch/web/console';

import {
  SellerTerminalStatus,
  SellerTerminalSummaryRow,
} from '../../data-access/seller-terminal-api.service';

@Component({
  selector: 'tch-seller-terminal-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    ConsoleActorRowComponent,
    TranslatePipe,
    MatButtonModule,
    MatMenuModule,
    MatTableModule,
  ],
  templateUrl: './seller-terminal-table.component.html',
  styleUrls: ['./seller-terminal-table.component.scss'],
})
export class SellerTerminalTableComponent {
  readonly rows = input.required<readonly SellerTerminalSummaryRow[]>();

  readonly openPos = output<SellerTerminalSummaryRow>();
  readonly resetPin = output<SellerTerminalSummaryRow>();
  readonly blockSeller = output<SellerTerminalSummaryRow>();
  readonly unblock = output<SellerTerminalSummaryRow>();
  readonly disable = output<SellerTerminalSummaryRow>();
  readonly limits = output<SellerTerminalSummaryRow>();

  readonly displayedColumns = [
    'seller',
    'commissionRate',
    'lastSeenAt',
    'actions',
  ];

  statusLabelKey(row: SellerTerminalSummaryRow): string {
    if (row.pinResetRequired) return 'admin.sellerTerminals.list.statusLabel.pinResetRequired';
    if (row.status === 'ACTIVE' && !row.lastSeenAt) return 'admin.sellerTerminals.list.statusLabel.activeNeverSeen';
    return `admin.sellerTerminals.status.${row.status}`;
  }

  canOpenPos(status: SellerTerminalStatus): boolean {
    return status === 'ACTIVE';
  }

  actorIdentity(row: SellerTerminalSummaryRow): ConsoleActorIdentity {
    return consoleSellerTerminalActorIdentity(row);
  }
}
