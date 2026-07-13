import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import type { TchSearchOption } from '@tch/ui/components';
import { ConsoleGameNamePipe } from '@tch/web/console';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';
import { Observable } from 'rxjs';

import { DrawReportFilterBarComponent } from '../../components/draw-report-filter-bar/draw-report-filter-bar.component';
import { ReportMetricCardComponent } from '../../components/report-metric-card/report-metric-card.component';
import { AdminReportDrawRow, AdminReportDraws, AdminReportsApi } from '../../data-access/admin-reports-api.service';

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
    return amount.toFixed(2);
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

  printReport(): void {
    window.print();
  }

  exportCsv(vm: AdminReportDraws): void {
    const rows = this.sortedRows(vm.rows);
    const header = ['drawId', 'scheduledAt', 'drawChannelCode', 'gameCode', 'ticketsSold', 'grossSales', 'payoutsPaid', 'netRevenueEstimated'];
    const lines = [
      header.join(','),
      ...rows.map(row => [
        row.drawId,
        row.scheduledAt,
        row.drawChannelCode ?? '',
        row.gameCode,
        row.ticketsSold,
        row.grossSales,
        row.payoutsPaid,
        row.netRevenueEstimated,
      ].map(csvCell).join(',')),
    ];
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `rapport-tirages-${vm.from}-${vm.to}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}

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

function csvCell(value: string | number): string {
  const text = String(value);
  return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}
