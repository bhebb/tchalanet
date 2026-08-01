import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { AdminRefreshButtonComponent } from '@tch/ui/console';
import { ConsoleGameNamePipe } from '@tch/web/console';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';

import { AdminMetricCardComponent } from '@tch/ui/console';
import {
  AdminReportDrawRow,
  AdminReportOverview,
  AdminReportSellerTerminalRow,
  AdminReportTopSelectionRow,
  AdminReportsApi,
} from '../../data-access/admin-reports-api.service';

const DEFAULT_CURRENCY = 'HTG';

@Component({
  selector: 'tch-admin-report-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    TranslatePipe,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminSectionCardComponent,
    ConsoleGameNamePipe,
    AdminMetricCardComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './report.page.html',
  styleUrls: ['./report.page.scss'],
})
export class AdminReportPage {
  private readonly api = inject(AdminReportsApi);

  readonly refreshTick = signal(0);
  readonly query = computed(() => ({
    drawLimit: 100,
    sellerTerminalLimit: 8,
    refresh: this.refreshTick(),
  }));
  readonly overview = this.api.overviewResource(this.query, { suppressShellFeedback: true });
  readonly overviewError = resourceErrorVm(this.overview, 'admin.reports.overview');

  readonly currency = DEFAULT_CURRENCY;

  reload(): void {
    this.refreshTick.update(value => value + 1);
  }

  amountDisplay(amount: number): string {
    return amount.toFixed(2);
  }

  netMarginPercent(vm: AdminReportOverview): number {
    if (vm.summary == null || vm.summary.grossSales === 0) return 0;
    return (vm.summary.netRevenueEstimated / vm.summary.grossSales) * 100;
  }

  drawLabel(row: AdminReportDrawRow): string {
    return row.drawChannelCode ? row.drawChannelCode : row.gameCode;
  }

  topDrawRows(rows: readonly AdminReportDrawRow[]): readonly AdminReportDrawRow[] {
    return [...rows]
      .sort((a, b) => b.ticketsSold - a.ticketsSold || b.grossSales - a.grossSales)
      .slice(0, 5);
  }

  shortSellerId(row: AdminReportSellerTerminalRow): string {
    const id = row.sellerTerminalId;
    return id.length <= 8 ? id : id.slice(0, 8);
  }

  topSelectionLabel(row: AdminReportTopSelectionRow): string {
    return row.betOption == null
      ? `${row.gameCode} · ${row.betType}`
      : `${row.gameCode} · ${row.betType} ${row.betOption}`;
  }
}
