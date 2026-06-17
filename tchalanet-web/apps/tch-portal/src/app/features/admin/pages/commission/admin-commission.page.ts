import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminDataTable,
  AdminMobileCardList,
  AdminStatusBadge,
  AdminEmptyState,
  AdminConfirmDialog,
  type AdminConfirmDialogData,
} from '@tch/ui/components';

import {
  CommissionAdminApi,
  CommissionOverview,
  SellerCommissionRow,
} from '../../commission-admin.api.service';
import {
  SetCommissionRateDialog,
  type SetRateDialogData,
} from './set-commission-rate.dialog';

const PAGE_SIZE = 100;

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly overview: CommissionOverview;
      readonly sellers: readonly SellerCommissionRow[];
    };

const DISPLAYED_COLUMNS = ['vendor', 'rate', 'source', 'actions'] as const;

@Component({
  selector: 'tch-admin-commission-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminDataTable,
    AdminMobileCardList,
    AdminStatusBadge,
    AdminEmptyState,
  ],
  templateUrl: './admin-commission.page.html',
  styleUrl: './admin-commission.page.scss',
})
export class AdminCommissionPage {
  private readonly api = inject(CommissionAdminApi);
  private readonly dialog = inject(MatDialog);

  readonly displayedColumns = DISPLAYED_COLUMNS;

  private readonly refresh$ = new Subject<void>();

  readonly state = toSignal(
    this.refresh$.pipe(
      startWith(undefined as void),
      switchMap(() =>
        this.api.overview().pipe(
          switchMap(overview =>
            this.api.listSellers({ size: PAGE_SIZE }).pipe(
              map(page => ({ status: 'ready', overview, sellers: page.items }) as PageState),
            ),
          ),
          catchError(() => of({ status: 'error' } as PageState)),
          startWith({ status: 'loading' } as PageState),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  editDefaultRate(): void {
    const vm = this.state();
    if (vm.status !== 'ready') return;

    const data: SetRateDialogData = {
      title: 'Taux de commission par défaut',
      currentRate: vm.overview.tenantDefaultRate,
      label: 'Nouveau taux défaut',
    };
    this.dialog
      .open(SetCommissionRateDialog, { data, width: '360px' })
      .afterClosed()
      .subscribe(result => {
        if (result) this.api.setDefaultRate(result.rate).subscribe({ next: () => this.refresh$.next() });
      });
  }

  editSellerRate(row: SellerCommissionRow): void {
    const data: SetRateDialogData = {
      title: `Commission — ${row.displayName}`,
      currentRate: row.commissionRate,
      label: 'Taux personnalisé',
    };
    this.dialog
      .open(SetCommissionRateDialog, { data, width: '360px' })
      .afterClosed()
      .subscribe(result => {
        if (result) this.api.setSellerRate(row.sellerTerminalId, result.rate).subscribe({ next: () => this.refresh$.next() });
      });
  }

  resetSellerRate(row: SellerCommissionRow): void {
    const data: AdminConfirmDialogData = {
      title: 'Remettre au taux défaut',
      message: `Le taux de ${row.displayName} reviendra au taux par défaut du tenant.`,
      confirmLabel: 'Confirmer',
    };
    this.dialog
      .open(AdminConfirmDialog, { data, width: '400px' })
      .afterClosed()
      .subscribe(confirmed => {
        if (confirmed) this.api.resetSellerRate(row.sellerTerminalId).subscribe({ next: () => this.refresh$.next() });
      });
  }
}
