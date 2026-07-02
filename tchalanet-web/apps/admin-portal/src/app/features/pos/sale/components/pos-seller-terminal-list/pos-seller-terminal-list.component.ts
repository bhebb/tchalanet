import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSortModule, Sort, SortDirection } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';

import { PosSellerTerminalPickerView } from '../../data-access/pos-sale.models';

@Component({
  selector: 'tch-pos-seller-terminal-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatSortModule, MatTableModule, TchStatusBadge],
  templateUrl: './pos-seller-terminal-list.component.html',
  styleUrls: ['./pos-seller-terminal-list.component.scss'],
})
export class PosSellerTerminalListComponent {
  readonly sellerTerminals = input<readonly PosSellerTerminalPickerView[]>([]);
  readonly sortActive = input('displayName');
  readonly sortDirection = input<SortDirection>('asc');

  readonly sellAs = output<PosSellerTerminalPickerView>();
  readonly sortChange = output<Sort>();

  readonly displayedColumns = [
    'seller',
    'status',
    'todayTicketCount',
    'todaySalesAmount',
    'lastSeenAt',
    'actions',
  ];

  canSellAs(sellerTerminal: PosSellerTerminalPickerView): boolean {
    return sellerTerminal.status === 'ACTIVE';
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'Actif';
      case 'BLOCKED':
        return 'Bloqué';
      case 'DISABLED':
        return 'Désactivé';
      case 'INACTIVE':
        return 'Inactif';
      case 'PENDING':
        return 'En attente';
      default:
        return status || 'Statut inconnu';
    }
  }

  statusBadge(status: string): BadgeStatus {
    switch (status) {
      case 'ACTIVE':
        return 'ready';
      case 'BLOCKED':
      case 'DISABLED':
        return 'blocked';
      case 'PENDING':
        return 'pending';
      default:
        return 'missing';
    }
  }
}
