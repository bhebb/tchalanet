import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../shared/admin-ui/admin-status-pill.component';
import {
  AdminPayoutsApi,
  PayoutRowView,
  PayoutStatus,
} from '../../admin-payouts-api.service';
import { PayoutActionDialog } from './dialogs/payout-action.dialog';

@Component({
  selector: 'tch-admin-payouts-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
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
  templateUrl: './admin-payouts.page.html',
  styleUrls: ['./admin-payouts.page.scss'],
})
export class AdminPayoutsPage implements OnInit {
  private readonly api = inject(AdminPayoutsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly columns = ['status', 'amount', 'sellerTerminalCode', 'createdAt', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = signal<PayoutRowView[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly statusFilter = signal<PayoutStatus | ''>('');

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list({
      status: this.statusFilter() || undefined,
      page: this.page(),
      size: 20,
    }).subscribe({
      next: p => {
        this.items.set(p.content);
        this.total.set(p.totalElements);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onStatusFilter(status: PayoutStatus | ''): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.load();
  }

  prevPage(): void {
    if (this.page() > 0) { this.page.update(p => p - 1); this.load(); }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) { this.page.update(p => p + 1); this.load(); }
  }

  openAction(row: PayoutRowView, action: 'block' | 'unblock' | 'cancel' | 'reverse'): void {
    const needsReason = action !== 'unblock';
    if (!needsReason) {
      this.api.unblock(row.id).subscribe({
        next: () => { this.snackBar.open('Paiement débloqué.', 'OK', { duration: 3000 }); this.load(); },
        error: () => this.snackBar.open('Erreur.', 'OK', { duration: 4000 }),
      });
      return;
    }

    const ref = this.dialog.open(PayoutActionDialog, {
      data: { action, payoutId: row.id },
      width: '420px',
    });
    ref.afterClosed().subscribe((reason: string | undefined) => {
      if (!reason) return;
      const call = action === 'block'
        ? this.api.block(row.id, reason)
        : action === 'cancel'
          ? this.api.cancel(row.id, reason)
          : this.api.reverse(row.id, reason);

      call.subscribe({
        next: () => { this.snackBar.open('Action effectuée.', 'OK', { duration: 3000 }); this.load(); },
        error: () => this.snackBar.open('Erreur lors de l\'action.', 'OK', { duration: 4000 }),
      });
    });
  }

  statusTone(status: PayoutStatus): AdminStatusTone {
    switch (status) {
      case 'APPROVED': return 'success';
      case 'PAID': return 'success';
      case 'BLOCKED': return 'danger';
      case 'CANCELLED': return 'danger';
      case 'REVERSED': return 'warning';
      default: return 'neutral';
    }
  }
}
