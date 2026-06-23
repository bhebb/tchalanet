import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';
import { GamesPricingSummaryComponent } from '../../components/games-pricing-summary/games-pricing-summary.component';
import { TenantGameCardComponent } from '../../components/tenant-game-card/tenant-game-card.component';
import { GameSettingsDialog } from '../../../pages/games/dialogs/game-settings.dialog';

type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-games-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    GamesPricingSummaryComponent,
    TenantGameCardComponent,
  ],
  templateUrl: './admin-games-pricing.page.html',
  styleUrls: ['./admin-games-pricing.page.scss'],
})
export class AdminGamesPricingPage implements OnInit {
  private readonly api = inject(AdminGamesPricingApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<string | null>(null);
  readonly games = signal<TenantGamePricingView[]>([]);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.api.getGamesPricing().subscribe({
      next: data => { this.games.set(data); this.pageState.set('ready'); },
      error: (err: unknown) => {
        this.pageError.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.pageState.set('error');
      },
    });
  }

  onActivate(gameCode: string): void {
    this.api.enableGame(gameCode).subscribe({
      next: () => { this.snackBar.open('Jeu activé.', 'OK', { duration: 3000 }); this.load(); },
      error: (err: unknown) => this.snackBar.open(
        (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 4000 }),
    });
  }

  onDisable(gameCode: string): void {
    this.api.disableGame(gameCode).subscribe({
      next: () => { this.snackBar.open('Jeu désactivé.', 'OK', { duration: 3000 }); this.load(); },
      error: (err: unknown) => this.snackBar.open(
        (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 4000 }),
    });
  }

  onConfigure(gameCode: string): void {
    const game = this.games().find(g => g.gameCode === gameCode);
    if (!game) return;

    // Reconstruct a minimal TenantGameView for the dialog
    const dialogGame = {
      gameCode: game.gameCode,
      catalogName: game.gameName,
      displayName: game.gameName,
      category: null,
      enabled: game.tenantStatus === 'ACTIVE' || game.tenantStatus === 'NEEDS_CONFIG',
      visibleInPos: true,
      displayOrder: 0,
      minStake: game.limits.minStake,
      maxStake: game.limits.maxStake,
      availabilityEnabled: false,
      availabilityDays: null,
      startLocalTime: null,
      endLocalTime: null,
      readyForSale: game.readiness.status === 'READY',
    };

    const ref = this.dialog.open(GameSettingsDialog, { data: { game: dialogGame }, width: '480px' });
    ref.afterClosed().subscribe(ok => { if (ok) this.load(); });
  }
}
