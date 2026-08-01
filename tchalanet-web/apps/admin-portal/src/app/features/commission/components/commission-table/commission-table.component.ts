import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminStatusPillComponent } from '@tch/ui/console';

import {
  SellerTerminalCommissionRow,
  sellerTerminalCommissionId,
} from '../../data-access/admin-commission-api.service';

@Component({
  selector: 'tch-commission-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminStatusPillComponent, MatButtonModule, MatMenuModule, MatTableModule, RouterLink, TranslatePipe],
  templateUrl: './commission-table.component.html',
  styleUrls: ['./commission-table.component.scss'],
})
export class CommissionTableComponent {
  readonly rows = input.required<readonly SellerTerminalCommissionRow[]>();
  readonly resettingId = input<string | null>(null);

  readonly editRate = output<SellerTerminalCommissionRow>();
  readonly resetRate = output<SellerTerminalCommissionRow>();

  readonly displayedColumns = ['seller', 'status', 'rate', 'source', 'actions'];

  rowId(row: SellerTerminalCommissionRow): string {
    return sellerTerminalCommissionId(row);
  }

  statusTone(status: string): 'success' | 'warning' | 'danger' | 'neutral' {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'PENDING':
        return 'warning';
      case 'BLOCKED':
      case 'DISABLED':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  statusLabel(status: string): string {
    return `admin.sellerTerminals.status.${status}`;
  }

  sourceLabel(source: SellerTerminalCommissionRow['rateSource']): string {
    return source === 'CUSTOM'
      ? 'admin.commission.source.custom'
      : 'admin.commission.source.default';
  }

  isResetting(row: SellerTerminalCommissionRow): boolean {
    return this.resettingId() === this.rowId(row);
  }

  rateDisplay(rate: number | null): string {
    return rate == null ? '—' : `${rate}%`;
  }
}
