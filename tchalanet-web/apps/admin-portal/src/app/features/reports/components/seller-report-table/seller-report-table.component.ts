import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { BadgeStatus, TchStatusBadge } from '@tch/ui/components';

export interface SellerReportRowView {
  readonly id: string;
  readonly key: string;
  readonly terminalName: string;
  readonly terminalCode: string;
  readonly statusLabelKey: string;
  readonly status: BadgeStatus;
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly sellerCommission: number;
  readonly commissionRate: number;
  readonly netRevenueEstimated: number;
  readonly averageTicket: number;
}

export interface SellerReportTotalsView {
  readonly ticketsSold: number;
  readonly grossSales: number;
  readonly sellerCommission: number;
  readonly commissionRate: number;
  readonly netRevenueEstimated: number;
  readonly averageTicket: number;
}

@Component({
  selector: 'tch-seller-report-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatSortModule,
    MatTableModule,
    TchStatusBadge,
  ],
  providers: [{ provide: MatPaginatorIntl, useFactory: sellerReportPaginatorIntl }],
  templateUrl: './seller-report-table.component.html',
  styleUrls: ['./seller-report-table.component.scss'],
})
export class SellerReportTableComponent {
  readonly rows = input.required<readonly SellerReportRowView[]>();
  readonly totals = input.required<SellerReportTotalsView>();
  readonly sortActive = input.required<string>();
  readonly sortDirection = input.required<'asc' | 'desc'>();
  readonly pageIndex = input.required<number>();
  readonly pageSize = input.required<number>();
  readonly pageSizeOptions = input<readonly number[]>([10, 25, 50]);
  readonly currency = input('HTG');

  readonly sortChange = output<Sort>();
  readonly pageChange = output<PageEvent>();

  protected readonly displayedColumns = [
    'seller',
    'status',
    'tickets',
    'sales',
    'commission',
    'net',
    'averageTicket',
    'actions',
  ];

  protected readonly pagedRows = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.rows().slice(start, start + this.pageSize());
  });

  protected amountDisplay(value: number): string {
    return sellerAmountFormatter.format(value);
  }

  protected percentDisplay(value: number): string {
    return `${sellerPercentFormatter.format(value)}%`;
  }

  protected integerDisplay(value: number): string {
    return sellerIntegerFormatter.format(value);
  }
}

const sellerAmountFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const sellerPercentFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

const sellerIntegerFormatter = new Intl.NumberFormat('fr-FR', {
  maximumFractionDigits: 0,
});

function sellerReportPaginatorIntl(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();
  intl.itemsPerPageLabel = 'Lignes par page';
  intl.nextPageLabel = 'Page suivante';
  intl.previousPageLabel = 'Page précédente';
  intl.firstPageLabel = 'Première page';
  intl.lastPageLabel = 'Dernière page';
  intl.getRangeLabel = (page: number, pageSize: number, length: number) => {
    if (length <= 0 || pageSize <= 0) {
      return '0 sur 0 terminaux vendeurs';
    }
    const start = page * pageSize + 1;
    const end = Math.min(start + pageSize - 1, length);
    return `${start}–${end} sur ${length} terminaux vendeurs`;
  };
  return intl;
}
