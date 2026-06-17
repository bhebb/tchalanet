import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import {
  AdminCommissionApi,
  CommissionOverviewView,
  SellerTerminalCommissionRow,
} from '../../admin-commission-api.service';
import { SetDefaultRateDialog } from './dialogs/set-default-rate.dialog';
import { SetSellerRateDialog } from './dialogs/set-seller-rate.dialog';

@Component({
  selector: 'tch-admin-commission-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
  ],
  templateUrl: './admin-commission.page.html',
  styleUrls: ['./admin-commission.page.scss'],
})
export class AdminCommissionPage implements OnInit {
  private readonly api = inject(AdminCommissionApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly sellerColumns = ['terminalCode', 'displayName', 'status', 'commissionRate', 'source', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly overview = signal<CommissionOverviewView | null>(null);
  readonly sellers = signal<SellerTerminalCommissionRow[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.getOverview().subscribe({
      next: v => { this.overview.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });

    this.api.listSellers().subscribe({
      next: v => this.sellers.set(v),
      error: () => {},
    });
  }

  openSetDefaultRate(): void {
    const current = this.overview()?.tenantDefaultRate ?? 0;
    const ref = this.dialog.open(SetDefaultRateDialog, { data: { current }, width: '420px' });
    ref.afterClosed().subscribe((rate: number | undefined) => {
      if (rate == null) return;
      this.api.setDefaultRate(rate).subscribe({
        next: () => {
          this.snackBar.open('Taux par défaut mis à jour.', 'OK', { duration: 3000 });
          this.load();
        },
        error: () => this.snackBar.open('Erreur lors de la mise à jour.', 'OK', { duration: 4000 }),
      });
    });
  }

  openSetSellerRate(row: SellerTerminalCommissionRow): void {
    const ref = this.dialog.open(SetSellerRateDialog, { data: { row }, width: '420px' });
    ref.afterClosed().subscribe((rate: number | undefined) => {
      if (rate == null) return;
      this.api.setSellerRate(row.id.value, rate).subscribe({
        next: () => {
          this.snackBar.open('Taux vendeur mis à jour.', 'OK', { duration: 3000 });
          this.load();
        },
        error: () => this.snackBar.open('Erreur lors de la mise à jour.', 'OK', { duration: 4000 }),
      });
    });
  }

  resetSellerRate(row: SellerTerminalCommissionRow): void {
    this.api.resetSellerRate(row.id.value).subscribe({
      next: () => {
        this.snackBar.open(`${row.displayName} revenu au taux par défaut.`, 'OK', { duration: 3000 });
        this.load();
      },
      error: () => this.snackBar.open('Erreur lors de la réinitialisation.', 'OK', { duration: 4000 }),
    });
  }
}
