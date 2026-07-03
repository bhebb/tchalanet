import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { AdminListStatusOption, AdminListSurface, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import {
  SellerTerminalApi,
  SellerTerminalSummaryRow,
  SellerTerminalStatus,
} from '../../../data-access/seller-terminal-api.service';
import { BlockSellerTerminalDialog } from './dialogs/block-seller-terminal.dialog';
import { ResetPinDialog } from './dialogs/reset-pin.dialog';
import { ConfirmDisableDialog } from './dialogs/confirm-disable.dialog';
import { SellerTerminalLimitsDialog } from './dialogs/seller-terminal-limits.dialog';

@Component({
  selector: 'tch-admin-seller-terminals-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    AdminPageShellComponent,
    AdminListSurface,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
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

  readonly displayedColumns = [
    'terminalCode',
    'displayName',
    'email',
    'phoneNumber',
    'status',
    'commissionRate',
    'lastSeenAt',
    'actions',
  ];

  readonly actionError = signal<ErrorViewModel | null>(null);
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly page = computed(() => numberParam(this.queryParamMap().get('page'), 0));
  readonly size = computed(() => numberParam(this.queryParamMap().get('size'), DEFAULT_PAGE_SIZE));
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
  readonly totalPages = computed(() => this.sellerPage()?.totalPages || 1);
  readonly hasNext = computed(() => this.sellerPage()?.hasNext ?? false);
  readonly hasPrevious = computed(() => this.sellerPage()?.hasPrevious ?? false);
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

  prevPage(): void {
    if (this.hasPrevious()) {
      this.navigateList({ page: Math.max(0, this.page() - 1) });
    }
  }

  nextPage(): void {
    if (this.hasNext()) {
      this.navigateList({ page: this.page() + 1 });
    }
  }

  openPOS(row: SellerTerminalSummaryRow): void {
    void this.router.navigate(['/app/admin/pos/sale', row.id.value]);
  }

  openBlock(row: SellerTerminalSummaryRow): void {
    const ref = this.dialog.open(BlockSellerTerminalDialog, { data: row, width: '480px' });
    ref.afterClosed().subscribe((result?: { reload: boolean }) => {
      if (result?.reload) this.reload();
    });
  }

  unblock(row: SellerTerminalSummaryRow): void {
    this.actionError.set(null);
    this.api.unblock(row.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => this.reload(),
      error: err => {
        this.actionError.set(this.errorViewModel(err, 'admin.sellerTerminal.unblock'));
      },
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
    this.dialog.open(SellerTerminalLimitsDialog, { data: row, width: '780px' });
  }

  openDisable(row: SellerTerminalSummaryRow): void {
    const ref = this.dialog.open(ConfirmDisableDialog, { data: row, width: '400px' });
    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.actionError.set(null);
      this.api.disable(row.id.value, { suppressShellFeedback: true }).subscribe({
        next: () => this.reload(),
        error: err => {
          this.actionError.set(this.errorViewModel(err, 'admin.sellerTerminal.disable'));
        },
      });
    });
  }

  statusLabel(row: SellerTerminalSummaryRow): string {
    if (row.pinResetRequired) {
      return this.translate.instant('admin.sellerTerminals.list.statusLabel.pinResetRequired');
    }
    if (row.status === 'ACTIVE' && !row.lastSeenAt) {
      return this.translate.instant('admin.sellerTerminals.list.statusLabel.activeNeverSeen');
    }
    return this.translate.instant(`admin.sellerTerminals.status.${row.status}`);
  }

  statusTone(row: SellerTerminalSummaryRow): AdminStatusTone {
    if (row.status === 'BLOCKED') return 'danger';
    if (row.pinResetRequired) return 'warning';
    if (row.status === 'PENDING') return 'warning';
    if (row.status === 'DISABLED') return 'neutral';
    return 'success';
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
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}

const DEFAULT_PAGE_SIZE = 20;
const SELLER_TERMINAL_STATUSES = ['PENDING', 'ACTIVE', 'BLOCKED', 'DISABLED'] as const;

function isSellerTerminalStatus(value: string | null): value is SellerTerminalStatus {
  return SELLER_TERMINAL_STATUSES.includes(value as SellerTerminalStatus);
}

function numberParam(value: string | null, fallback: number): number {
  if (value === null || value.trim() === '') return fallback;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}
