import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import type { TchSearchOption } from '@tch/ui/components';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';
import { Observable } from 'rxjs';

import { ReportMetricCardComponent } from '../../components/report-metric-card/report-metric-card.component';
import { SellerReportFilterBarComponent } from '../../components/seller-report-filter-bar/seller-report-filter-bar.component';
import {
  AdminReportSellerTerminalRow,
  AdminReportSellerTerminals,
  AdminReportsApi,
} from '../../data-access/admin-reports-api.service';
import { exportReportCsv, exportReportPdf } from '../../utils/report-export.util';

const DEFAULT_CURRENCY = 'HTG';
const SELLER_SORT_VALUES = [
  'grossSales,DESC',
  'grossSales,ASC',
  'sellerCommission,DESC',
  'sellerCommission,ASC',
  'netRevenueEstimated,DESC',
  'netRevenueEstimated,ASC',
  'averageGrossSalesPerDraw,DESC',
  'averageGrossSalesPerDraw,ASC',
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
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatIconModule,
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
  readonly sortOptions: readonly { value: SellerReportSort; labelKey: string }[] = [
    { value: 'grossSales,DESC', labelKey: 'admin.reports.sellers.sort.salesDesc' },
    { value: 'grossSales,ASC', labelKey: 'admin.reports.sellers.sort.salesAsc' },
    { value: 'sellerCommission,DESC', labelKey: 'admin.reports.sellers.sort.commissionDesc' },
    { value: 'sellerCommission,ASC', labelKey: 'admin.reports.sellers.sort.commissionAsc' },
    { value: 'netRevenueEstimated,DESC', labelKey: 'admin.reports.sellers.sort.netDesc' },
    { value: 'netRevenueEstimated,ASC', labelKey: 'admin.reports.sellers.sort.netAsc' },
    { value: 'averageGrossSalesPerDraw,DESC', labelKey: 'admin.reports.sellers.sort.averageDrawDesc' },
    { value: 'averageGrossSalesPerDraw,ASC', labelKey: 'admin.reports.sellers.sort.averageDrawAsc' },
  ];
  readonly query = computed(() => ({
    sellerTerminalLimit: 100,
    from: this.fromFilter(),
    to: this.toFilter(),
    sellerTerminalIds: this.selectedSellers().map(seller => seller.id),
    refresh: this.refreshTick(),
  }));
  readonly sellers = this.api.sellerTerminalsResource(this.query, { suppressShellFeedback: true });
  readonly sellersError = resourceErrorVm(this.sellers, 'admin.reports.sellers');
  readonly currency = DEFAULT_CURRENCY;

  reload(): void {
    this.refreshTick.update(value => value + 1);
  }

  onFromFilter(value: string): void {
    this.fromFilter.set(value || this.today);
  }

  onToFilter(value: string): void {
    this.toFilter.set(value || this.fromFilter());
  }

  onSellersFilter(value: readonly TchSearchOption[]): void {
    this.selectedSellers.set(value);
  }

  onSortFilter(value: string): void {
    if (isSellerReportSort(value)) {
      this.sortFilter.set(value);
    }
  }

  readonly searchSellers = (query: string): Observable<readonly TchSearchOption[]> =>
    this.api.searchSellerTerminals(query, { suppressShellFeedback: true });

  amountDisplay(amount: number): string {
    return amount.toFixed(2);
  }

  commissionRatePercent(vm: AdminReportSellerTerminals): number {
    if (vm.summary.grossSales === 0) return 0;
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

  sortedRows(rows: readonly AdminReportSellerTerminalRow[]): readonly AdminReportSellerTerminalRow[] {
    const [field, direction] = this.sortFilter().split(',') as [string, 'ASC' | 'DESC'];
    const factor = direction === 'ASC' ? 1 : -1;
    return [...rows].sort((a, b) => compareSellerRows(a, b, field) * factor);
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
    case 'sellerCommission':
      return a.sellerCommission - b.sellerCommission;
    case 'netRevenueEstimated':
      return a.netRevenueEstimated - b.netRevenueEstimated;
    case 'averageGrossSalesPerDraw':
      return a.averageGrossSalesPerDraw - b.averageGrossSalesPerDraw;
    default:
      return a.grossSales - b.grossSales;
  }
}
