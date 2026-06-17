import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { AdminPricingApi, PricingView } from '../../admin-pricing-api.service';

@Component({
  selector: 'tch-admin-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './admin-pricing.page.html',
  styleUrls: ['./admin-pricing.page.scss'],
})
export class AdminPricingPage implements OnInit {
  private readonly api = inject(AdminPricingApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly columns = ['gameCode', 'betType', 'betOption', 'odds', 'active'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly odds = signal<PricingView[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getDefaultOdds().subscribe({
      next: v => { this.odds.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  activeTone(active: boolean): AdminStatusTone {
    return active ? 'success' : 'neutral';
  }
}
