import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TCH_DEFAULT_PAGE_SIZE, mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import {
  AdminListStatusOption,
  AdminListSurface,
  TchErrorPanel,
  TchNotice,
} from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminRefreshButtonComponent } from '@tch/ui/console';
import { TchPaginationComponent } from '@tch/ui/console';
import { TchAsyncReadyDirective, TchAsyncViewComponent, resourceErrorVm } from '@tch/web/async';
import {
  SellerTerminalApi,
  SellerTerminalSummaryRow,
  SellerTerminalStatus,
} from '../../data-access/seller-terminal-api.service';
import { SellerTerminalKpiStripComponent } from '../../components/seller-terminal-kpi-strip/seller-terminal-kpi-strip.component';
import { SellerTerminalTableComponent } from '../../components/seller-terminal-table/seller-terminal-table.component';
import { BlockSellerTerminalDialog } from './dialogs/block-seller-terminal.dialog';
import { ResetPinDialog } from './dialogs/reset-pin.dialog';
import { ConfirmDisableDialog } from './dialogs/confirm-disable.dialog';
import { ConfirmUnblockDialog } from './dialogs/confirm-unblock.dialog';
import { SellerTerminalDialogResult } from './dialogs/seller-terminal-dialog-result';

@Component({
  selector: 'tch-admin-seller-terminals-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    AdminListSurface,
    AdminEmptyStateComponent,
    TchPaginationComponent,
    TchErrorPanel,
    TchNotice,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    SellerTerminalKpiStripComponent,
    SellerTerminalTableComponent,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-seller-terminals.page.html',
  styleUrls: ['./admin-seller-terminals.page.scss'],
})
export class AdminSellerTerminalsPage {
  private readonly api = inject(SellerTerminalApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionSuccess = signal<string | null>(null);
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly page = computed(() => numberParam(this.queryParamMap().get('page'), 0));
  readonly size = computed(() => numberParam(this.queryParamMap().get('size'), TCH_DEFAULT_PAGE_SIZE));
  readonly searchQuery = computed(() => this.queryParamMap().get('q')?.trim() ?? '');
  readonly statusFilter = computed<SellerTerminalStatus | ''>(() => {
    const status = this.queryParamMap().get('status');
    return isSellerTerminalStatus(status) ? status : '';
  });
  readonly sellers = this.api.listResource(
    () => ({
      q: this.searchQuery() || undefined,
      status: this.statusFilter() || undefined,
      page: this.page(),
      size: this.size(),
    }),
    { suppressShellFeedback: true },
  );
  readonly sellersError = resourceErrorVm(this.sellers, 'admin.sellerTerminal.list');
  readonly sellerPage = computed(() => this.sellers.value() ?? null);
  readonly items = computed(() => this.sellerPage()?.items ?? []);
  readonly totalElements = computed(() => this.sellerPage()?.totalElements ?? 0);
  readonly summaryResource = this.api.getSummaryResource({ suppressShellFeedback: true });
  readonly summary = computed(() => this.summaryResource.value() ?? null);

  readonly statusOptions: readonly AdminListStatusOption[] = [
    { value: 'ACTIVE', label: this.translate.instant('admin.sellerTerminals.status.ACTIVE') },
    { value: 'PENDING', label: this.translate.instant('admin.sellerTerminals.status.PENDING') },
    { value: 'BLOCKED', label: this.translate.instant('admin.sellerTerminals.status.BLOCKED') },
    { value: 'DISABLED', label: this.translate.instant('admin.sellerTerminals.status.DISABLED') },
  ];

  reload(): void {
    this.actionError.set(null);
    this.summaryResource.reload();
    this.sellers.reload();
  }

  onSearch(q: string): void {
    this.navigateList({ q: q.trim() || null, page: null });
  }

  resetFilters(): void {
    this.navigateList({ q: null, status: null, page: null });
  }

  onStatusFilter(status: string): void {
    this.navigateList({ status: isSellerTerminalStatus(status) ? status : null, page: null });
  }

  onPageChange(page: number): void {
    this.navigateList({ page: page > 0 ? page : null });
  }

  onSizeChange(size: number): void {
    this.navigateList({ size: size !== TCH_DEFAULT_PAGE_SIZE ? size : null, page: null });
  }

  openPOS(row: SellerTerminalSummaryRow): void {
    void this.router.navigate(['/app/admin/pos/sale', row.id.value]);
  }

  openBlock(row: SellerTerminalSummaryRow): void {
    this.actionSuccess.set(null);
    const ref = this.dialog.open(BlockSellerTerminalDialog, { data: row, width: '480px' });
    ref.afterClosed().subscribe((result?: SellerTerminalDialogResult) => {
      if (!result?.reload) return;
      this.actionSuccess.set(result.noticeKey);
      this.reload();
    });
  }

  unblock(row: SellerTerminalSummaryRow): void {
    const ref = this.dialog.open(ConfirmUnblockDialog, { data: row, width: '400px' });
    ref.afterClosed().subscribe((unblocked?: boolean) => {
      if (unblocked) this.reload();
    });
  }

  openResetPin(row: SellerTerminalSummaryRow): void {
    const ref = this.dialog.open(ResetPinDialog, {
      data: row,
      width: '480px',
      disableClose: true,
    });
    ref.afterClosed().subscribe((result?: { reload: boolean }) => {
      if (result?.reload) this.reload();
    });
  }

  openLimits(row: SellerTerminalSummaryRow): void {
    void this.router.navigate(['/app/admin/seller-terminals', row.id.value]);
  }

  openDisable(row: SellerTerminalSummaryRow): void {
    const ref = this.dialog.open(ConfirmDisableDialog, { data: row, width: '400px' });
    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.reload();
    });
  }

  private navigateList(params: {
    readonly q?: string | null;
    readonly status?: SellerTerminalStatus | null;
    readonly page?: number | null;
    readonly size?: number | null;
  }): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const normalized = webAppErrorFromProblemDetail(
      mapHttpErrorToProblemDetail(err),
      source,
      'page',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}

const SELLER_TERMINAL_STATUSES = ['PENDING', 'ACTIVE', 'BLOCKED', 'DISABLED'] as const;

function isSellerTerminalStatus(value: string | null): value is SellerTerminalStatus {
  return SELLER_TERMINAL_STATUSES.includes(value as SellerTerminalStatus);
}

function numberParam(value: string | null, fallback: number): number {
  if (value === null || value.trim() === '') return fallback;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}
