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
import { MatSnackBar } from '@angular/material/snack-bar';

import { AdminListStatusOption, AdminListSurface, TchErrorPanel, TchLoading } from '@tch/ui/components';
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
  private readonly snackBar = inject(MatSnackBar);

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
  readonly error = signal<string | null>(null);
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

    this.api
      .list({
        q: this.searchQuery() || undefined,
        status: this.statusFilter() || undefined,
        page: this.page(),
        size: 20,
      })
      .subscribe({
        next: res => {
          this.items.set(res.items);
          this.total.set(res.total);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const pd = (err as { error?: { title?: string } })?.error;
          this.error.set(pd?.title ?? 'Erreur de chargement.');
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
    this.api.unblock(row.id.value).subscribe({
      next: () => {
        this.snackBar.open(`${row.displayName} débloqué.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors du déblocage.', 'OK', { duration: 4000 }),
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
      this.api.disable(row.id.value).subscribe({
        next: () => {
          this.snackBar.open(`${row.displayName} désactivé.`, 'OK', { duration: 3000 });
          this.loadPage();
        },
        error: () => this.snackBar.open('Erreur lors de la désactivation.', 'OK', { duration: 4000 }),
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
}
