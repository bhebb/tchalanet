import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';
import {
  TenantGameCardComponent,
  TenantGameCardError,
  TenantGameChannelStats,
} from '../../components/tenant-game-card/tenant-game-card.component';
import { GameSettingsDialog } from '../../../pages/games/dialogs/game-settings.dialog';
import {
  AdminDrawSalesMatrixApi,
  ChannelGameSetupView,
  TenantDrawSalesMatrixView,
} from '../../../draw-sales-matrix/data-access/admin-draw-sales-matrix-api.service';

type PageState = 'loading' | 'ready' | 'error';

interface GamesOverviewSummary {
  readonly supportedGameCount: number;
  readonly activeGameCount: number;
  readonly readyChannelCount: number;
  readonly missingStakeCombinationCount: number;
}

interface GamesOverviewIssue {
  readonly gameCode: string;
  readonly gameName: string;
  readonly message: string;
  readonly actionLabel: string;
  readonly action: 'configure-game' | 'open-channel-matrix';
  readonly tone: 'warning' | 'danger';
}

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
    TenantGameCardComponent,
  ],
  templateUrl: './admin-games-pricing.page.html',
  styleUrls: ['./admin-games-pricing.page.scss'],
})
export class AdminGamesPricingPage implements OnInit {
  private readonly api = inject(AdminGamesPricingApiService);
  private readonly matrixApi = inject(AdminDrawSalesMatrixApi);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly games = signal<TenantGamePricingView[]>([]);
  readonly gameChannelStats = signal<Readonly<Record<string, TenantGameChannelStats>>>({});
  readonly matrixSummary = signal<GamesOverviewSummary | null>(null);
  readonly issues = signal<readonly GamesOverviewIssue[]>([]);
  readonly actionErrors = signal<Readonly<Record<string, TenantGameCardError>>>({});
  readonly summaryItems = computed(() => {
    const summary = this.matrixSummary();
    if (!summary) return [];

    return [
      {
        label: 'Jeux supportés',
        value: summary.supportedGameCount,
        help: 'Jeux disponibles dans le catalogue tenant.',
      },
      {
        label: 'Jeux actifs',
        value: summary.activeGameCount,
        help: 'Jeux activés pour la vente.',
      },
      {
        label: 'Canaux prêts à vendre',
        value: summary.readyChannelCount,
        help: 'Canaux actifs et configurés.',
      },
      {
        label: 'Mises à compléter',
        value: summary.missingStakeCombinationCount,
        help: 'Combinaisons jeu/canal avec mise non configurée.',
        tone: summary.missingStakeCombinationCount > 0 ? 'warning' : 'success',
      },
    ];
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.actionErrors.set({});
    forkJoin({
      games:  this.api.getGamesPricing({ suppressShellFeedback: true }),
      matrix: this.matrixApi.getMatrix({ suppressShellFeedback: true }),
    }).subscribe({
      next: ({ games, matrix }) => {
        this.games.set(games);
        this.gameChannelStats.set(this.buildGameChannelStats(matrix));
        this.matrixSummary.set(this.buildOverviewSummary(games, matrix));
        this.issues.set(this.buildOverviewIssues(games, matrix));
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        const problem = (err as { error?: ProblemDetail })?.error;
        this.pageError.set(this.pageErrorViewModel(problem));
        this.pageState.set('error');
      },
    });
  }

  channelStats(gameCode: string): TenantGameChannelStats {
    return this.gameChannelStats()[gameCode] ?? {
      offeredChannelCount:       0,
      activeChannelCount:        0,
      readyChannelCount:         0,
      missingStakeChannelCount:  0,
      missingLimitChannelCount:  0,
    };
  }

  onActivate(gameCode: string): void {
    this.clearActionError(gameCode);
    this.api.enableGame(gameCode, { suppressShellFeedback: true }).subscribe({
      next: () => this.load(),
      error: (err: unknown) => this.setActionError(gameCode, err),
    });
  }

  onDisable(gameCode: string): void {
    this.clearActionError(gameCode);
    this.api.disableGame(gameCode, { suppressShellFeedback: true }).subscribe({
      next: () => this.load(),
      error: (err: unknown) => this.setActionError(gameCode, err),
    });
  }

  onConfigure(gameCode: string): void {
    if (gameCode === 'HT_MARYAJ_GRATUIT') {
      void this.router.navigate(['/app/admin/maryaj-gratis'], { fragment: 'game' });
      return;
    }

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
      betOptions: game.odds,
    };

    const ref = this.dialog.open(GameSettingsDialog, { data: { game: dialogGame }, width: '480px' });
    ref.afterClosed().subscribe(ok => { if (ok) this.load(); });
  }

  onIssueAction(issue: GamesOverviewIssue): void {
    if (issue.action === 'open-channel-matrix') {
      void this.router.navigate(['/app/admin/games/channel-matrix']);
      return;
    }

    this.onConfigure(issue.gameCode);
  }

  actionError(gameCode: string): TenantGameCardError | null {
    return this.actionErrors()[gameCode] ?? null;
  }

  private setActionError(gameCode: string, err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    const error = this.errorCopy(problem, `admin.setup.games_pricing.${gameCode}`, 'section');
    this.actionErrors.update(current => ({
      ...current,
      [gameCode]: error,
    }));
  }

  private clearActionError(gameCode: string): void {
    this.actionErrors.update(current => {
      if (!current[gameCode]) return current;
      const next = { ...current };
      delete next[gameCode];
      return next;
    });
  }

  private buildOverviewSummary(
    games: readonly TenantGamePricingView[],
    matrix: TenantDrawSalesMatrixView,
  ): GamesOverviewSummary {
    return {
      supportedGameCount:            games.filter(game => game.catalogStatus === 'AVAILABLE').length,
      activeGameCount:               games.filter(game => game.tenantStatus === 'ACTIVE').length,
      readyChannelCount:             matrix.summary.activeChannelCount,
      missingStakeCombinationCount:  matrix.summary.missingStakeConfigCount,
    };
  }

  private buildGameChannelStats(matrix: TenantDrawSalesMatrixView): Readonly<Record<string, TenantGameChannelStats>> {
    const stats: Record<string, TenantGameChannelStats> = {};

    for (const provider of matrix.providers) {
      for (const slot of provider.slots) {
        if (!slot.channel?.active) continue;

        for (const game of slot.games) {
          const current = stats[game.gameCode] ?? {
            offeredChannelCount:       0,
            activeChannelCount:        0,
            readyChannelCount:         0,
            missingStakeChannelCount:  0,
            missingLimitChannelCount:  0,
          };

          stats[game.gameCode] = this.addChannelGameStats(current, game);
        }
      }
    }

    return stats;
  }

  private buildOverviewIssues(
    games: readonly TenantGamePricingView[],
    matrix: TenantDrawSalesMatrixView,
  ): readonly GamesOverviewIssue[] {
    const stats = this.buildGameChannelStats(matrix);
    const activeChannelTotal = matrix.summary.activeChannelCount;
    const issues: GamesOverviewIssue[] = [];

    for (const game of games) {
      if (game.catalogStatus !== 'AVAILABLE' || game.tenantStatus === 'UNAVAILABLE') continue;

      const gameStats = stats[game.gameCode] ?? {
        offeredChannelCount:       0,
        activeChannelCount:        0,
        readyChannelCount:         0,
        missingStakeChannelCount:  0,
        missingLimitChannelCount:  0,
      };

      if (game.tenantStatus === 'NEEDS_CONFIG' || !this.hasGameStakeConfig(game)) {
        issues.push({
          gameCode:    game.gameCode,
          gameName:    game.gameName,
          message:     'Mise non configurée pour ce jeu.',
          actionLabel: 'Configurer',
          action:      'configure-game',
          tone:        'danger',
        });
        continue;
      }

      if (gameStats.activeChannelCount === 0 && game.tenantStatus === 'ACTIVE') {
        issues.push({
          gameCode:    game.gameCode,
          gameName:    game.gameName,
          message:     'Ce jeu est actif, mais il n’est vendu sur aucun canal.',
          actionLabel: 'Voir les canaux',
          action:      'open-channel-matrix',
          tone:        'warning',
        });
        continue;
      }

      if (gameStats.missingStakeChannelCount > 0) {
        issues.push({
          gameCode:    game.gameCode,
          gameName:    game.gameName,
          message:     `Mise manquante sur ${gameStats.missingStakeChannelCount} canal${gameStats.missingStakeChannelCount > 1 ? 'aux' : ''}.`,
          actionLabel: 'Compléter',
          action:      'open-channel-matrix',
          tone:        'warning',
        });
        continue;
      }

      if (
        game.tenantStatus === 'ACTIVE' &&
        activeChannelTotal > 1 &&
        gameStats.activeChannelCount > 0 &&
        gameStats.activeChannelCount < activeChannelTotal
      ) {
        issues.push({
          gameCode:    game.gameCode,
          gameName:    game.gameName,
          message:     `Actif sur ${gameStats.activeChannelCount} canal${gameStats.activeChannelCount > 1 ? 'aux' : ''} seulement.`,
          actionLabel: 'Voir les canaux',
          action:      'open-channel-matrix',
          tone:        'warning',
        });
      }
    }

    return issues.slice(0, 6);
  }

  private hasGameStakeConfig(game: TenantGamePricingView): boolean {
    return game.limits.minStake !== null && game.limits.maxStake !== null;
  }

  private addChannelGameStats(
    current: TenantGameChannelStats,
    game: ChannelGameSetupView,
  ): TenantGameChannelStats {
    if (!game.offeredOnChannel) return current;

    const active = game.enabledForTenant && game.enabledOnChannel;
    const missingStake = active && (game.minStake === null || game.maxStake === null);
    const missingLimit = active && !game.limits.configured;

    return {
      offeredChannelCount:       current.offeredChannelCount + 1,
      activeChannelCount:        current.activeChannelCount + (active ? 1 : 0),
      readyChannelCount:         current.readyChannelCount + (game.saleReady ? 1 : 0),
      missingStakeChannelCount:  current.missingStakeChannelCount + (missingStake ? 1 : 0),
      missingLimitChannelCount:  current.missingLimitChannelCount + (missingLimit ? 1 : 0),
    };
  }

  private pageErrorViewModel(problem: ProblemDetail | undefined): ErrorViewModel {
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.categories.unexpected.title'),
        message: this.translate.instant('common.errors.categories.unexpected.message'),
      };
    }

    const normalized = webAppErrorFromProblemDetail(problem, 'admin.setup.games_pricing', 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }

  private errorCopy(
    problem: ProblemDetail | undefined,
    source: string,
    surface: 'page' | 'section',
  ): TenantGameCardError {
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.categories.unexpected.title'),
        message: this.translate.instant('common.errors.categories.unexpected.message'),
      };
    }

    const normalized = webAppErrorFromProblemDetail(problem, source, surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return {
      severity: normalized.severity,
      title: copy.title,
      message: copy.message,
    };
  }
}
