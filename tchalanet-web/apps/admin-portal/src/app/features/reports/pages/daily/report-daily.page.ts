import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';

import {
  DailySalesChartComponent,
  DailySalesChartPoint,
} from '../../components/daily-sales-chart/daily-sales-chart.component';
import { ReportMetricCardComponent } from '../../components/report-metric-card/report-metric-card.component';
import { AdminReportDailyRow, AdminReportOverview, AdminReportsApi } from '../../data-access/admin-reports-api.service';
import { exportReportCsv, exportReportPdf } from '../../utils/report-export.util';

const DEFAULT_CURRENCY = 'HTG';

@Component({
  selector: 'tch-admin-report-daily-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    TranslatePipe,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    DailySalesChartComponent,
    ReportMetricCardComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
  ],
  templateUrl: './report-daily.page.html',
  styleUrls: ['./report-daily.page.scss'],
})
export class AdminReportDailyPage {
  private readonly api = inject(AdminReportsApi);

  readonly today = new Date().toISOString().slice(0, 10);
  readonly fromFilter = signal(this.today);
  readonly toFilter = signal(this.today);
  readonly refreshTick = signal(0);
  readonly query = computed(() => ({
    from: this.fromFilter(),
    to: this.toFilter(),
    drawLimit: 500,
    sellerTerminalLimit: 500,
    refresh: this.refreshTick(),
  }));
  readonly daily = this.api.overviewResource(this.query, { suppressShellFeedback: true });
  readonly dailyError = resourceErrorVm(this.daily, 'admin.reports.daily');
  readonly currency = DEFAULT_CURRENCY;
  readonly displayedColumns = ['date', 'tickets', 'sales', 'payouts', 'net'];

  onFromFilter(value: string): void {
    this.fromFilter.set(value || this.today);
  }

  onToFilter(value: string): void {
    this.toFilter.set(value || this.fromFilter());
  }

  reload(): void {
    this.refreshTick.update(value => value + 1);
  }

  amountDisplay(amount: number): string {
    return amountFormatter.format(amount);
  }

  chartPoints(vm: AdminReportOverview): readonly DailySalesChartPoint[] {
    return vm.dailyRows.map(row => ({
      label: row.refDate,
      ticketsSold: row.ticketsSold,
      grossSales: row.grossSales,
    }));
  }

  exportCsv(vm: AdminReportOverview): void {
    const header = ['refDate', 'ticketsSold', 'grossSales', 'payoutsPaid', 'sellerCommission', 'tenantCharges', 'netRevenueEstimated'];
    exportReportCsv(`rapport-journee-${vm.from}-${vm.to}.csv`, [
      header,
      ...vm.dailyRows.map(row => [
        row.refDate,
        row.ticketsSold,
        row.grossSales,
        row.payoutsPaid,
        row.sellerCommission,
        row.tenantCharges,
        row.netRevenueEstimated,
      ]),
    ]);
  }

  exportPdf(): void {
    exportReportPdf();
  }

  trackDailyRow(_index: number, row: AdminReportDailyRow): string {
    return row.refDate;
  }
}

const amountFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
