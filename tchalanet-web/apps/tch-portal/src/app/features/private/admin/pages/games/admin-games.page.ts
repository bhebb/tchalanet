import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import {
  GamesAdminApiService,
  TenantGameView,
  CatalogGameView,
} from '../../games-admin-api.service';
import { GameSettingsDialog } from './dialogs/game-settings.dialog';

@Component({
  selector: 'tch-admin-games-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTabsModule,
  ],
  templateUrl: './admin-games.page.html',
  styleUrls: ['./admin-games.page.scss'],
})
export class AdminGamesPage implements OnInit {
  private readonly api = inject(GamesAdminApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly gameColumns = ['gameCode', 'displayName', 'enabled', 'settings'];
  readonly catalogColumns = ['gameCode', 'displayName', 'category', 'description', 'activate'];

  readonly loadingGames = signal(false);
  readonly errorGames = signal<string | null>(null);
  readonly games = signal<TenantGameView[]>([]);

  readonly loadingCatalog = signal(false);
  readonly errorCatalog = signal<string | null>(null);
  readonly catalog = signal<CatalogGameView[]>([]);

  ngOnInit(): void {
    this.loadGames();
    this.loadCatalog();
  }

  protected loadGames(): void {
    this.loadingGames.set(true);
    this.api.listEnabledGames().subscribe({
      next: v => { this.games.set(v); this.loadingGames.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorGames.set(pd?.title ?? 'Erreur.');
        this.loadingGames.set(false);
      },
    });
  }

  protected loadCatalog(): void {
    this.loadingCatalog.set(true);
    this.api.listCatalogGames().subscribe({
      next: v => { this.catalog.set(v); this.loadingCatalog.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorCatalog.set(pd?.title ?? 'Erreur.');
        this.loadingCatalog.set(false);
      },
    });
  }

  isEnabled(gameCode: string): boolean {
    return this.games().some(g => g.gameCode === gameCode && g.enabled);
  }

  toggleGame(game: TenantGameView): void {
    const op = game.enabled ? this.api.disableGame(game.gameCode) : this.api.enableGame(game.gameCode);
    op.subscribe({
      next: () => {
        this.snackBar.open(`Jeu ${game.gameCode} ${game.enabled ? 'désactivé' : 'activé'}.`, 'OK', { duration: 3000 });
        this.loadGames();
      },
      error: () => this.snackBar.open('Erreur.', 'OK', { duration: 4000 }),
    });
  }

  enableFromCatalog(game: CatalogGameView): void {
    this.api.enableGame(game.gameCode).subscribe({
      next: () => {
        this.snackBar.open(`Jeu ${game.gameCode} activé.`, 'OK', { duration: 3000 });
        this.loadGames();
      },
      error: () => this.snackBar.open("Erreur lors de l'activation.", 'OK', { duration: 4000 }),
    });
  }

  openSettings(game: TenantGameView): void {
    const ref = this.dialog.open(GameSettingsDialog, { data: { game }, width: '440px' });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.loadGames();
    });
  }
}
