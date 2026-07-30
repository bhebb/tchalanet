import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { BadgeStatus } from '@tch/ui/components';
import type { TchSearchOption } from '@tch/ui/components';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';
import { Observable } from 'rxjs';

import { ReportMetricCardComponent } from '../../components/report-metric-card/report-metric-card.component';
import { SellerReportFilterBarComponent } from '../../components/seller-report-filter-bar/seller-report-filter-bar.component';
import {
  SellerReportRowView,
  SellerReportTableComponent,
  SellerReportTotalsView,
} from '../../components/seller-report-table/seller-report-table.component';
import {
  AdminReportSellerTerminalRow,
  AdminReportSellerTerminals,
  AdminReportsApi,
} from '../../data-access/admin-reports-api.service';
import { exportReportCsv, exportReportPdf } from '../../utils/report-export.util';

const DEFAULT_CURRENCY = 'HTG';
const SELLER_SORT_VALUES = [
  'ticketsSold,DESC',
  'ticketsSold,ASC',
  'grossSales,DESC',
  'grossSales,ASC',
  'sellerCommission,DESC',
  'sellerCommission,ASC',
  'netRevenueEstimated,DESC',
  'netRevenueEstimated,ASC',
  'averageTicket,DESC',
  'averageTicket,ASC',
] as const;

type SellerReportSort = typeof SELLER_SORT_VALUES[number];

function isSellerReportSort(value: string): value is SellerReportSort {
  return SELLER_SORT_VALUES.includes(value as SellerReportSort);
}

@Component({
  selector: 'tch-admin-report-sellers-page',
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
    ReportMetricCardComponent,
    SellerReportFilterBarComponent,
    SellerReportTableComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
  ],
  templateUrl: './report-sellers.page.html',
  styleUrls: ['./report-sellers.page.scss'],
})
export class AdminReportSellersPage {
  private readonly api = inject(AdminReportsApi);

  readonly today = new Date().toISOString().slice(0, 10);
  readonly refreshTick = signal(0);
  readonly fromFilter = signal(this.today);
  readonly toFilter = signal(this.today);
  readonly selectedSellers = signal<readonly TchSearchOption[]>([]);
  readonly sortFilter = signal<SellerReportSort>('grossSales,DESC');
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly pageSizeOptions = [10, 25, 50] as const;
  readonly sortOptions: readonly { value: SellerReportSort; labelKey: string }[] = [
    { value: 'ticketsSold,DESC', labelKey: 'admin.reports.sellers.sort.ticketsDesc' },
    { value: 'ticketsSold,ASC', labelKey: 'admin.reports.sellers.sort.ticketsAsc' },
    { value: 'grossSales,DESC', labelKey: 'admin.reports.sellers.sort.salesDesc' },
    { value: 'grossSales,ASC', labelKey: 'admin.reports.sellers.sort.salesAsc' },
    { value: 'sellerCommission,DESC', labelKey: 'admin.reports.sellers.sort.commissionDesc' },
    { value: 'sellerCommission,ASC', labelKey: 'admin.reports.sellers.sort.commissionAsc' },
    { value: 'netRevenueEstimated,DESC', labelKey: 'admin.reports.sellers.sort.netDesc' },
    { value: 'netRevenueEstimated,ASC', labelKey: 'admin.reports.sellers.sort.netAsc' },
    { value: 'averageTicket,DESC', labelKey: 'admin.reports.sellers.sort.averageTicketDesc' },
    { value: 'averageTicket,ASC', labelKey: 'admin.reports.sellers.sort.averageTicketAsc' },
  ];
  readonly query = computed(() => ({
    sellerTerminalLimit: 500,
    from: this.fromFilter(),
    to: this.toFilter(),
    sellerTerminalIds: this.selectedSellers().map(seller => seller.id),
    refresh: this.refreshTick(),
  }));
  readonly sellers = this.api.sellerTerminalsResource(this.query, { suppressShellFeedback: true });
  readonly sellersError = resourceErrorVm(this.sellers, 'admin.reports.sellers');
  readonly currency = DEFAULT_CURRENCY;
  readonly sortActive = computed(() => this.sortFilter().split(',')[0]);
  readonly sortDirection = computed(() => this.sortFilter().endsWith(',ASC') ? 'asc' : 'desc');

  reload(): void {
    this.refreshTick.update(value => value + 1);
  }

  onFromFilter(value: string): void {
    this.fromFilter.set(value || this.today);
    this.resetPage();
  }

  onToFilter(value: string): void {
    this.toFilter.set(value || this.fromFilter());
    this.resetPage();
  }

  onSellersFilter(value: readonly TchSearchOption[]): void {
    this.selectedSellers.set(value);
    this.resetPage();
  }

  onSortFilter(value: string): void {
    if (isSellerReportSort(value)) {
      this.sortFilter.set(value);
      this.resetPage();
    }
  }

  onTableSort(sort: Sort): void {
    if (!sort.active || !sort.direction) return;
    const value = `${sort.active},${sort.direction.toUpperCase()}`;
    this.onSortFilter(value);
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  readonly searchSellers = (query: string): Observable<readonly TchSearchOption[]> =>
    this.api.searchSellerTerminals(query, { suppressShellFeedback: true });

  amountDisplay(amount: number): string {
    return amountFormatter.format(amount);
  }

  commissionRatePercent(vm: AdminReportSellerTerminals): number {
    if (vm.summary == null || vm.summary.grossSales === 0) return 0;
    return (vm.summary.sellerCommission / vm.summary.grossSales) * 100;
  }

  shortSellerId(row: AdminReportSellerTerminalRow): string {
    const id = row.sellerTerminalId;
    return id.length <= 8 ? id : id.slice(0, 8);
  }

  sellerLabel(row: AdminReportSellerTerminalRow): string {
    return this.selectedSellers().find(seller => seller.id === row.sellerTerminalId)?.title
      ?? row.displayName
      ?? row.terminalCode
      ?? this.shortSellerId(row);
  }

  sellerSubtitle(row: AdminReportSellerTerminalRow): string | null {
    return this.selectedSellers().find(seller => seller.id === row.sellerTerminalId)?.subtitle
      ?? row.terminalCode
      ?? null;
  }

  sellerStatus(row: AdminReportSellerTerminalRow): string {
    return row.status ?? this.selectedSellers().find(seller => seller.id === row.sellerTerminalId)?.badge ?? '-';
  }

  sellerRowKey(row: AdminReportSellerTerminalRow): string {
    return `${row.sellerTerminalId}:${row.refDate}`;
  }

  sellerRows(rows: readonly AdminReportSellerTerminalRow[]): readonly SellerReportRowView[] {
    return this.sortedRows(rows).map(row => ({
      id: row.sellerTerminalId,
      key: this.sellerRowKey(row),
      terminalName: this.sellerLabel(row),
      terminalCode: this.sellerSubtitle(row) ?? '-',
      statusLabelKey: sellerStatusLabelKey(row.status),
      status: sellerStatusBadge(row.status),
      ticketsSold: row.ticketsSold,
      grossSales: row.grossSales,
      sellerCommission: row.sellerCommission,
      commissionRate: percentage(row.sellerCommission, row.grossSales),
      netRevenueEstimated: row.netRevenueEstimated,
      averageTicket: average(row.grossSales, row.ticketsSold),
    }));
  }

  sellerTotals(vm: AdminReportSellerTerminals): SellerReportTotalsView {
    if (vm.summary == null) {
      return {
        ticketsSold: 0,
        grossSales: 0,
        sellerCommission: 0,
        commissionRate: 0,
        netRevenueEstimated: 0,
        averageTicket: 0,
      };
    }
    return {
      ticketsSold: vm.summary.ticketsSold,
      grossSales: vm.summary.grossSales,
      sellerCommission: vm.summary.sellerCommission,
      commissionRate: percentage(vm.summary.sellerCommission, vm.summary.grossSales),
      netRevenueEstimated: vm.summary.netRevenueEstimated,
      averageTicket: average(vm.summary.grossSales, vm.summary.ticketsSold),
    };
  }

  private sortedRows(rows: readonly AdminReportSellerTerminalRow[]): readonly AdminReportSellerTerminalRow[] {
    const [field, direction] = this.sortFilter().split(',') as [string, 'ASC' | 'DESC'];
    const factor = direction === 'ASC' ? 1 : -1;
    return [...rows].sort((a, b) => compareSellerRows(a, b, field) * factor);
  }

  private resetPage(): void {
    this.pageIndex.set(0);
  }

  exportCsv(vm: AdminReportSellerTerminals): void {
    const header = [
      'sellerTerminalId',
      'terminalCode',
      'displayName',
      'refDate',
      'status',
      'ticketsSold',
      'grossSales',
      'sellerCommission',
      'netRevenueEstimated',
      'drawCount',
      'averageGrossSalesPerDraw',
    ];
    exportReportCsv(`rapport-vendeurs-${vm.from}-${vm.to}.csv`, [
      header,
      ...this.sortedRows(vm.rows).map(row => [
        row.sellerTerminalId,
        row.terminalCode ?? '',
        row.displayName ?? '',
        row.refDate,
        this.sellerStatus(row),
        row.ticketsSold,
        row.grossSales,
        row.sellerCommission,
        row.netRevenueEstimated,
        row.drawCount,
        row.averageGrossSalesPerDraw,
      ]),
    ]);
  }

  exportPdf(): void {
    exportReportPdf();
  }
}

function compareSellerRows(a: AdminReportSellerTerminalRow, b: AdminReportSellerTerminalRow, field: string): number {
  switch (field) {
    case 'ticketsSold':
      return a.ticketsSold - b.ticketsSold;
    case 'sellerCommission':
      return a.sellerCommission - b.sellerCommission;
    case 'netRevenueEstimated':
      return a.netRevenueEstimated - b.netRevenueEstimated;
    case 'averageTicket':
      return average(a.grossSales, a.ticketsSold) - average(b.grossSales, b.ticketsSold);
    default:
      return a.grossSales - b.grossSales;
  }
}

function sellerStatusBadge(status: string | null): BadgeStatus {
  switch ((status ?? '').toUpperCase()) {
    case 'ACTIVE':
      return 'ready';
    case 'BLOCKED':
    case 'SUSPENDED':
      return 'blocked';
    case 'DISABLED':
    case 'INACTIVE':
      return 'missing';
    default:
      return 'missing';
  }
}

function sellerStatusLabelKey(status: string | null): string {
  switch ((status ?? '').toUpperCase()) {
    case 'ACTIVE':
      return 'admin.reports.sellers.status.active';
    case 'BLOCKED':
    case 'SUSPENDED':
      return 'admin.reports.sellers.status.blocked';
    case 'DISABLED':
    case 'INACTIVE':
      return 'admin.reports.sellers.status.disabled';
    default:
      return 'admin.reports.sellers.status.disabled';
  }
}

function percentage(part: number, total: number): number {
  return total === 0 ? 0 : (part / total) * 100;
}

function average(total: number, count: number): number {
  return count === 0 ? 0 : total / count;
}

const amountFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
