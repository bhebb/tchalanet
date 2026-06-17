import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../private/shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  SellerTerminalApi,
  SellerTerminalSummaryRow,
  SellerTerminalStatus,
} from '../../seller-terminal-api.service';
import { CreateSellerTerminalDialog } from './dialogs/create-seller-terminal.dialog';
import { ResetPinDialog } from './dialogs/reset-pin.dialog';
import { ConfirmDisableDialog } from './dialogs/confirm-disable.dialog';

@Component({
  selector: 'tch-admin-terminals-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './admin-terminals.page.html',
  styleUrls: ['./admin-terminals.page.scss'],
})
export class AdminTerminalsPage implements OnInit {
  private readonly api = inject(SellerTerminalApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = [
    'terminalCode',
    'displayName',
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

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));

  ngOnInit(): void {
    this.loadPage();
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

  onStatusFilter(status: SellerTerminalStatus | ''): void {
    this.statusFilter.set(status);
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

  openCreate(): void {
    const ref = this.dialog.open(CreateSellerTerminalDialog, { width: '600px' });
    ref.afterClosed().subscribe((result?: { reload: boolean }) => {
      if (result?.reload) this.loadPage();
    });
  }

  block(row: SellerTerminalSummaryRow): void {
    this.api.block(row.id.value, 'Bloqué par un administrateur').subscribe({
      next: () => {
        this.snackBar.open(`${row.displayName} bloqué.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors du blocage.', 'OK', { duration: 4000 }),
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
    this.dialog.open(ResetPinDialog, { data: row, width: '420px' });
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

  statusTone(status: SellerTerminalStatus): AdminStatusTone {
    if (status === 'ACTIVE') return 'success';
    if (status === 'BLOCKED') return 'danger';
    if (status === 'DISABLED') return 'danger';
    return 'neutral';
  }
}
