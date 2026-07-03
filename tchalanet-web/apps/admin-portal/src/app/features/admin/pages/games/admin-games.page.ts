import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import {
  ConsoleGameActionEvent,
  ConsoleGameRow,
  ConsoleGamesTableComponent,
} from '@tch/web/console';
import {
  GamesAdminApiService,
  TenantGameView,
  CatalogGameView,
} from '../../data-access/games-admin-api.service';
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
    ConsoleGamesTableComponent,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
  ],
  templateUrl: './admin-games.page.html',
  styleUrls: ['./admin-games.page.scss'],
})
export class AdminGamesPage implements OnInit {
  private readonly api = inject(GamesAdminApiService);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly loadingGames = signal(false);
  readonly errorGames = signal<ErrorViewModel | null>(null);
  readonly games = signal<TenantGameView[]>([]);

  readonly loadingCatalog = signal(false);
  readonly errorCatalog = signal<ErrorViewModel | null>(null);
  readonly catalog = signal<CatalogGameView[]>([]);
  readonly gameRows = signal<readonly ConsoleGameRow[]>([]);
  readonly catalogRows = signal<readonly ConsoleGameRow[]>([]);

  ngOnInit(): void {
    this.loadGames();
    this.loadCatalog();
  }

  protected loadGames(): void {
    this.loadingGames.set(true);
    this.errorGames.set(null);
    this.api.listEnabledGames({ suppressShellFeedback: true }).subscribe({
      next: v => {
        this.games.set(v);
        this.gameRows.set(v.map(game => this.toTenantGameRow(game)));
        this.loadingGames.set(false);
      },
      error: (err: unknown) => {
        this.errorGames.set(this.errorViewModel(err, 'admin.games.enabled'));
        this.loadingGames.set(false);
      },
    });
  }

  protected loadCatalog(): void {
    this.loadingCatalog.set(true);
    this.errorCatalog.set(null);
    this.api.listCatalogGames({ suppressShellFeedback: true }).subscribe({
      next: v => {
        this.catalog.set(v);
        this.catalogRows.set(v.map(game => this.toCatalogGameRow(game)));
        this.loadingCatalog.set(false);
      },
      error: (err: unknown) => {
        this.errorCatalog.set(this.errorViewModel(err, 'admin.games.catalog'));
        this.loadingCatalog.set(false);
      },
    });
  }

  isEnabled(game: CatalogGameView): boolean {
    return game.enabledForTenant;
  }

  toggleGame(game: TenantGameView): void {
    this.errorGames.set(null);
    const op = game.enabled
      ? this.api.disableGame(game.gameCode, { suppressShellFeedback: true })
      : this.api.enableGame(game.gameCode, { suppressShellFeedback: true });
    op.subscribe({
      next: () => {
        this.loadGames();
      },
      error: err => this.errorGames.set(this.errorViewModel(err, `admin.games.enabled.${game.gameCode}`)),
    });
  }

  enableFromCatalog(game: CatalogGameView): void {
    this.errorCatalog.set(null);
    this.api.enableGame(game.gameCode, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.loadGames();
        this.loadCatalog();
      },
      error: err => this.errorCatalog.set(this.errorViewModel(err, `admin.games.catalog.${game.gameCode}`)),
    });
  }

  openSettings(game: TenantGameView): void {
    const ref = this.dialog.open(GameSettingsDialog, { data: { game }, width: '440px' });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.loadGames();
    });
  }

  onTenantGameAction(event: ConsoleGameActionEvent): void {
    const game = this.games().find(item => item.gameCode === event.row.id);
    if (!game) return;
    switch (event.action.id) {
      case 'toggle':
        this.toggleGame(game);
        break;
      case 'settings':
        this.openSettings(game);
        break;
    }
  }

  onCatalogGameAction(event: ConsoleGameActionEvent): void {
    const game = this.catalog().find(item => item.gameCode === event.row.id);
    if (!game || event.action.id !== 'enable') return;
    this.enableFromCatalog(game);
  }

  private toTenantGameRow(game: TenantGameView): ConsoleGameRow {
    return {
      id: game.gameCode,
      code: game.gameCode,
      name: game.displayName ?? game.catalogName,
      category: game.category,
      sortOrder: game.displayOrder,
      statusLabel: game.enabled ? 'Actif' : 'Inactif',
      statusTone: game.enabled ? 'success' : 'neutral',
      supporting: game.readyForSale ? 'Prêt à vendre' : 'Configuration incomplète',
      actions: [
        {
          id: 'toggle',
          label: game.enabled ? 'Désactiver' : 'Activer',
          icon: game.enabled ? 'block' : 'check_circle',
          tone: game.enabled ? 'danger' : 'primary',
        },
        { id: 'settings', label: 'Paramètres', icon: 'tune', variant: 'icon' },
      ],
    };
  }

  private toCatalogGameRow(game: CatalogGameView): ConsoleGameRow {
    return {
      id: game.gameCode,
      code: game.gameCode,
      name: game.name,
      category: game.category,
      statusLabel: game.enabledForTenant ? 'Activé' : game.catalogActive ? 'Disponible' : 'Inactif',
      statusTone: game.enabledForTenant ? 'success' : game.catalogActive ? 'info' : 'neutral',
      supporting: game.disabledReason,
      actions: game.enabledForTenant
        ? []
        : [{
            id: 'enable',
            label: 'Activer',
            icon: 'add',
            tone: 'primary',
            variant: 'button',
          }],
    };
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
