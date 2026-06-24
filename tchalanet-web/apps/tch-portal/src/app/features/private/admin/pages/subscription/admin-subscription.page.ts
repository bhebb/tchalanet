import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../shared/admin-ui/admin-section-card.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../shared/admin-ui/admin-status-pill.component';
import {
  AdminSubscriptionApi,
  SubscriptionView,
  SubscriptionStatus,
} from '../../admin-subscription-api.service';
import { RenewSubscriptionDialog } from './dialogs/renew-subscription.dialog';
import { CancelSubscriptionDialog } from './dialogs/cancel-subscription.dialog';

@Component({
  selector: 'tch-admin-subscription-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-subscription.page.html',
  styleUrls: ['./admin-subscription.page.scss'],
})
export class AdminSubscriptionPage implements OnInit {
  private readonly api = inject(AdminSubscriptionApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly subscription = signal<SubscriptionView | null>(null);
  readonly acting = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.get().subscribe({
      next: v => { this.subscription.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  openRenew(): void {
    const ref = this.dialog.open(RenewSubscriptionDialog, { width: '420px' });
    ref.afterClosed().subscribe((newEndsAt: string | undefined) => {
      if (!newEndsAt) return;
      this.acting.set(true);
      this.api.renew(newEndsAt).subscribe({
        next: () => { this.snackBar.open('Abonnement renouvelé.', 'OK', { duration: 3000 }); this.load(); this.acting.set(false); },
        error: () => { this.snackBar.open('Erreur lors du renouvellement.', 'OK', { duration: 4000 }); this.acting.set(false); },
      });
    });
  }

  openCancel(): void {
    const ref = this.dialog.open(CancelSubscriptionDialog, { width: '420px' });
    ref.afterClosed().subscribe((reason: string | undefined) => {
      if (reason === undefined) return;
      this.acting.set(true);
      this.api.cancel(reason || undefined).subscribe({
        next: () => { this.snackBar.open('Abonnement annulé.', 'OK', { duration: 3000 }); this.load(); this.acting.set(false); },
        error: () => { this.snackBar.open('Erreur lors de l\'annulation.', 'OK', { duration: 4000 }); this.acting.set(false); },
      });
    });
  }

  suspend(): void {
    this.acting.set(true);
    this.api.suspend().subscribe({
      next: () => { this.snackBar.open('Abonnement suspendu.', 'OK', { duration: 3000 }); this.load(); this.acting.set(false); },
      error: () => { this.snackBar.open('Erreur lors de la suspension.', 'OK', { duration: 4000 }); this.acting.set(false); },
    });
  }

  resume(): void {
    this.acting.set(true);
    this.api.resume().subscribe({
      next: () => { this.snackBar.open('Abonnement repris.', 'OK', { duration: 3000 }); this.load(); this.acting.set(false); },
      error: () => { this.snackBar.open('Erreur lors de la reprise.', 'OK', { duration: 4000 }); this.acting.set(false); },
    });
  }

  statusTone(status: SubscriptionStatus): AdminStatusTone {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'TRIALING': return 'success';
      case 'PAST_DUE': return 'warning';
      case 'SUSPENDED': return 'warning';
      case 'CANCELLED': return 'danger';
      case 'EXPIRED': return 'danger';
    }
  }
}
