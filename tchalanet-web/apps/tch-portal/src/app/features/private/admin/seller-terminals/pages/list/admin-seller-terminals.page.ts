import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { AdminListStatusOption, AdminListSurface, TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../../shared/admin-ui/admin-status-pill.component';
import {
  SellerTerminalApi,
  SellerTerminalSummaryRow,
  SellerTerminalsSummary,
  SellerTerminalStatus,
} from '../../../seller-terminal-api.service';
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
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
  ],
  templateUrl: './admin-seller-terminals.page.html',
  styleUrls: ['./admin-seller-terminals.page.scss'],
})
export class AdminSellerTerminalsPage implements OnInit {
  private readonly api = inject(SellerTerminalApi);
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

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly items = signal<SellerTerminalSummaryRow[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<SellerTerminalStatus | ''>('');
  readonly summary = signal<SellerTerminalsSummary | null>(null);

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));
  readonly statusOptions: readonly AdminListStatusOption[] = [
    { value: 'ACTIVE', label: 'Actif' },
    { value: 'INACTIVE', label: 'Inactif' },
    { value: 'BLOCKED', label: 'Bloqué' },
    { value: 'DISABLED', label: 'Désactivé' },
  ];

  ngOnInit(): void {
    this.loadSummary();
    this.loadPage();
  }

  private loadSummary(): void {
    this.api.getSummary().subscribe({
      next: s => this.summary.set(s),
      error: () => { /* silently ignore — KPI section hides when null */ },
    });
  }

  loadPage(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);

    this.api
      .list({
        q: this.searchQuery() || undefined,
        status: this.statusFilter() || undefined,
        page: this.page(),
        size: 20,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: res => {
          this.items.set(res.items);
          this.total.set(res.totalElements);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.error.set(this.errorViewModel(err, 'admin.sellerTerminal.list'));
          this.loading.set(false);
        },
      });
  }

  onSearch(q: string): void {
    this.searchQuery.set(q);
    this.page.set(0);
    this.loadPage();
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.statusFilter.set('');
    this.page.set(0);
    this.loadPage();
  }

  onStatusFilter(status: string): void {
    this.statusFilter.set((status || '') as SellerTerminalStatus | '');
    this.page.set(0);
    this.loadPage();
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update(p => p - 1);
      this.loadPage();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update(p => p + 1);
      this.loadPage();
    }
  }

  openPOS(row: SellerTerminalSummaryRow): void {
    void this.router.navigate(['/app/admin/tickets/sell', row.id.value]);
  }

  openBlock(row: SellerTerminalSummaryRow): void {
    const ref = this.dialog.open(BlockSellerTerminalDialog, { data: row, width: '480px' });
    ref.afterClosed().subscribe((result?: { reload: boolean }) => {
      if (result?.reload) this.loadPage();
    });
  }

  unblock(row: SellerTerminalSummaryRow): void {
    this.actionError.set(null);
    this.api.unblock(row.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => this.loadPage(),
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
      if (result?.reload) this.loadPage();
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
        next: () => this.loadPage(),
        error: err => {
          this.actionError.set(this.errorViewModel(err, 'admin.sellerTerminal.disable'));
        },
      });
    });
  }

  statusLabel(row: SellerTerminalSummaryRow): string {
    if (row.status === 'BLOCKED') return 'Bloqué';
    if (row.pinResetRequired) return 'PIN à remettre';
    if (row.status === 'PENDING') return 'En attente';
    if (row.status === 'INACTIVE' || row.status === 'DISABLED') return 'Inactif';
    if (!row.lastSeenAt) return 'Actif · jamais connecté';
    return 'Actif';
  }

  statusTone(row: SellerTerminalSummaryRow): AdminStatusTone {
    if (row.status === 'BLOCKED') return 'danger';
    if (row.pinResetRequired) return 'warning';
    if (row.status === 'PENDING') return 'warning';
    if (row.status === 'INACTIVE' || row.status === 'DISABLED') return 'neutral';
    return 'success';
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
