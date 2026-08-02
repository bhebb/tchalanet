import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { finalize } from 'rxjs';

import {
  AdminListStatusOption,
  AdminListSurface,
  TchErrorPanel,
  TchNotice,
} from '@tch/ui/components';
import {
  AdminEmptyStateComponent,
  AdminPageShellComponent,
  AdminRefreshButtonComponent,
} from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import {
  AdminCommissionApi,
  SellerTerminalCommissionRow,
  sellerTerminalCommissionId,
} from './data-access/admin-commission-api.service';
import { SetDefaultRateDialog } from './dialogs/set-default-rate.dialog';
import { SetSellerRateDialog } from './dialogs/set-seller-rate.dialog';
import { CommissionKpiStripComponent } from './components/commission-kpi-strip/commission-kpi-strip.component';
import { CommissionTableComponent } from './components/commission-table/commission-table.component';

export function adminCommissionErrorView(
  err: unknown,
  source: string,
  translate: (key: string) => string,
): ErrorViewModel {
  const problem = mapHttpErrorToProblemDetail(err);
  const normalized = webAppErrorFromProblemDetail(problem, source, 'section');
  const copy = resolveErrorFeedbackCopy(normalized, translate);
  return toErrorViewModel(normalized, copy);
}

@Component({
  selector: 'tch-admin-seller-configuration-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminEmptyStateComponent,
    AdminListSurface,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    CommissionKpiStripComponent,
    CommissionTableComponent,
    TchErrorPanel,
    TchNotice,
    TranslatePipe,
    MatButtonModule,
  ],
  templateUrl: './admin-seller-configuration.page.html',
  styleUrls: ['./admin-seller-configuration.page.scss'],
})
export class AdminSellerConfigurationPage {
  private readonly api = inject(AdminCommissionApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly searchQuery = computed(() => this.queryParamMap().get('q')?.trim() ?? '');
  readonly statusFilter = computed(() => this.queryParamMap().get('status') ?? '');
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionSuccess = signal<string | null>(null);
  readonly resettingId = signal<string | null>(null);

  readonly overviewResource = this.api.getOverviewResource({ suppressShellFeedback: true });
  readonly sellersResource = this.api.listSellersResource({ suppressShellFeedback: true });
  readonly overviewError = resourceErrorVm(this.overviewResource, 'admin.commission.overview');
  readonly sellersError = resourceErrorVm(this.sellersResource, 'admin.commission.sellers');

  readonly sellers = computed(() => this.sellersResource.value() ?? []);
  readonly filteredSellers = computed(() => {
    const query = this.searchQuery().toLocaleLowerCase();
    const status = this.statusFilter();
    return this.sellers().filter(row => {
      const matchesQuery =
        !query ||
        row.displayName.toLocaleLowerCase().includes(query) ||
        row.terminalCode.toLocaleLowerCase().includes(query);
      return matchesQuery && (!status || row.status === status);
    });
  });

  readonly statusOptions: readonly AdminListStatusOption[] = [
    { value: 'ACTIVE', label: this.translate.instant('admin.sellerTerminals.status.ACTIVE') },
    { value: 'PENDING', label: this.translate.instant('admin.sellerTerminals.status.PENDING') },
    { value: 'BLOCKED', label: this.translate.instant('admin.sellerTerminals.status.BLOCKED') },
    { value: 'DISABLED', label: this.translate.instant('admin.sellerTerminals.status.DISABLED') },
  ];

  reload(): void {
    this.actionError.set(null);
    this.overviewResource.reload();
    this.sellersResource.reload();
  }

  onSearch(query: string): void {
    this.navigateList({ q: query.trim() || null, status: this.statusFilter() || null });
  }

  onStatusFilter(status: string): void {
    this.navigateList({ q: this.searchQuery() || null, status: status || null });
  }

  resetFilters(): void {
    this.navigateList({ q: null, status: null });
  }

  openSetDefaultRate(): void {
    const current = this.overviewResource.value()?.tenantDefaultRate ?? 0;
    const ref = this.dialog.open(SetDefaultRateDialog, { data: { current }, width: '420px' });
    ref.afterClosed().subscribe((saved?: boolean) => {
      if (!saved) return;
      this.actionSuccess.set('admin.commission.notice.defaultUpdated');
      this.reload();
    });
  }

  openSetSellerRate(row: SellerTerminalCommissionRow): void {
    const ref = this.dialog.open(SetSellerRateDialog, { data: { row }, width: '420px' });
    ref.afterClosed().subscribe((saved?: boolean) => {
      if (!saved) return;
      this.actionSuccess.set('admin.commission.notice.sellerUpdated');
      this.reload();
    });
  }

  resetSellerRate(row: SellerTerminalCommissionRow): void {
    const sellerTerminalId = sellerTerminalCommissionId(row);
    this.actionError.set(null);
    this.actionSuccess.set(null);
    this.resettingId.set(sellerTerminalId);
    this.api
      .resetSellerRate(sellerTerminalId, { suppressShellFeedback: true })
      .pipe(finalize(() => this.resettingId.set(null)))
      .subscribe({
        next: () => {
          this.actionSuccess.set('admin.commission.notice.rateReset');
          this.reload();
        },
        error: err => this.actionError.set(this.errorViewModel(err, 'admin.commission.sellers')),
      });
  }

  private navigateList(params: { q: string | null; status: string | null }): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    return adminCommissionErrorView(err, source, key => this.translate.instant(key));
  }
}
