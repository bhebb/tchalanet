import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import type { TchSearchOption } from '@tch/ui/components';
import { ConsoleGameNamePipe } from '@tch/web/console';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';
import { Observable } from 'rxjs';

import { DrawReportFilterBarComponent } from '../../components/draw-report-filter-bar/draw-report-filter-bar.component';
import { ReportMetricCardComponent } from '../../components/report-metric-card/report-metric-card.component';
import { AdminReportDrawRow, AdminReportDraws, AdminReportsApi } from '../../data-access/admin-reports-api.service';
import { exportReportCsv, exportReportPdf } from '../../utils/report-export.util';

const DEFAULT_CURRENCY = 'HTG';
const DRAW_SORT_VALUES = [
  'scheduledAt,DESC',
  'scheduledAt,ASC',
  'grossSales,DESC',
  'grossSales,ASC',
  'ticketsSold,DESC',
  'ticketsSold,ASC',
] as const;

type DrawReportSort = typeof DRAW_SORT_VALUES[number];

function isDrawReportSort(value: string): value is DrawReportSort {
  return DRAW_SORT_VALUES.includes(value as DrawReportSort);
}

@Component({
  selector: 'tch-admin-report-draws-page',
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
    ConsoleGameNamePipe,
    DrawReportFilterBarComponent,
    ReportMetricCardComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './report-draws.page.html',
  styleUrls: ['./report-draws.page.scss'],
})
export class AdminReportDrawsPage {
  private readonly api = inject(AdminReportsApi);

  readonly refreshTick = signal(0);
  readonly fromFilter = signal('');
  readonly toFilter = signal('');
  readonly selectedDraws = signal<readonly TchSearchOption[]>([]);
  readonly sortFilter = signal<DrawReportSort>('scheduledAt,DESC');
  readonly sortOptions: readonly { value: DrawReportSort; labelKey: string }[] = [
    { value: 'scheduledAt,DESC', labelKey: 'admin.reports.draws.sort.scheduledDesc' },
    { value: 'scheduledAt,ASC', labelKey: 'admin.reports.draws.sort.scheduledAsc' },
    { value: 'grossSales,DESC', labelKey: 'admin.reports.draws.sort.salesDesc' },
    { value: 'grossSales,ASC', labelKey: 'admin.reports.draws.sort.salesAsc' },
    { value: 'ticketsSold,DESC', labelKey: 'admin.reports.draws.sort.ticketsDesc' },
    { value: 'ticketsSold,ASC', labelKey: 'admin.reports.draws.sort.ticketsAsc' },
  ];
  readonly query = computed(() => ({
    drawLimit: 100,
    from: this.fromFilter() || undefined,
    to: this.toFilter() || undefined,
    drawIds: this.selectedDraws().map(draw => draw.id),
    refresh: this.refreshTick(),
  }));
  readonly draws = this.api.drawsResource(this.query, { suppressShellFeedback: true });
  readonly drawsError = resourceErrorVm(this.draws, 'admin.reports.draws');
  readonly currency = DEFAULT_CURRENCY;
  readonly displayedColumns = ['draw', 'tickets', 'sales', 'payouts', 'net'];

  reload(): void {
    this.refreshTick.update(value => value + 1);
  }

  onFromFilter(value: string): void {
    this.fromFilter.set(value);
  }

  onToFilter(value: string): void {
    this.toFilter.set(value);
  }

  onDrawsFilter(value: readonly TchSearchOption[]): void {
    this.selectedDraws.set(value);
  }

  onSortFilter(value: string): void {
    if (isDrawReportSort(value)) {
      this.sortFilter.set(value);
    }
  }

  readonly searchDraws = (query: string): Observable<readonly TchSearchOption[]> =>
    this.api.searchDraws(query, { suppressShellFeedback: true });

  amountDisplay(amount: number): string {
    return amountFormatter.format(amount);
  }

  payoutRatePercent(vm: AdminReportDraws): number {
    if (vm.summary.grossSales === 0) return 0;
    return (vm.summary.payoutsPaid / vm.summary.grossSales) * 100;
  }

  drawChannel(row: AdminReportDrawRow): string {
    return row.drawChannelCode ?? row.gameCode;
  }

  sortedRows(rows: readonly AdminReportDrawRow[]): readonly AdminReportDrawRow[] {
    const [field, direction] = this.sortFilter().split(',') as [string, 'ASC' | 'DESC'];
    const factor = direction === 'ASC' ? 1 : -1;
    return [...rows].sort((a, b) => compareDrawRows(a, b, field) * factor);
  }

  exportPdf(): void {
    exportReportPdf();
  }

  exportCsv(vm: AdminReportDraws): void {
    const rows = this.sortedRows(vm.rows);
    const header = ['drawId', 'scheduledAt', 'drawChannelCode', 'gameCode', 'ticketsSold', 'grossSales', 'payoutsPaid', 'netRevenueEstimated'];
    exportReportCsv(`rapport-tirages-${vm.from}-${vm.to}.csv`, [
      header,
      ...rows.map(row => [
        row.drawId,
        row.scheduledAt,
        row.drawChannelCode ?? '',
        row.gameCode,
        row.ticketsSold,
        row.grossSales,
        row.payoutsPaid,
        row.netRevenueEstimated,
      ]),
    ]);
  }
}

const amountFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

function compareDrawRows(a: AdminReportDrawRow, b: AdminReportDrawRow, field: string): number {
  switch (field) {
    case 'grossSales':
      return a.grossSales - b.grossSales;
    case 'ticketsSold':
      return a.ticketsSold - b.ticketsSold;
    default:
      return a.scheduledAt.localeCompare(b.scheduledAt);
  }
}
