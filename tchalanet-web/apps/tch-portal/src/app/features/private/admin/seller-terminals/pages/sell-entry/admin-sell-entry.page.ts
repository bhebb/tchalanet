import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { SellerTerminalApi, SellerTerminalSummaryRow } from '../../../seller-terminal-api.service';

@Component({
  selector: 'tch-admin-sell-entry-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
  ],
  templateUrl: './admin-sell-entry.page.html',
  styleUrls: ['./admin-sell-entry.page.scss'],
})
export class AdminSellEntryPage implements OnInit {
  private readonly api = inject(SellerTerminalApi);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly terminals = signal<readonly SellerTerminalSummaryRow[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list({ status: 'ACTIVE', page: 0, size: 100 }).subscribe({
      next: page => {
        this.terminals.set(page.items);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; detail?: string } })?.error;
        this.error.set(pd?.title ?? pd?.detail ?? 'Impossible de charger les terminaux de vente.');
        this.loading.set(false);
      },
    });
  }
}
