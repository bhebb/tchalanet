import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';

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
} from '../../components/tenant-game-card/tenant-game-card.component';
import { GameSettingsDialog } from '../../../pages/games/dialogs/game-settings.dialog';

type PageState = 'loading' | 'ready' | 'error';

interface GamesOverviewSummary {
  readonly catalogGameCount: number;
  readonly activeGameCount: number;
  readonly needsConfigCount: number;
  readonly inactiveGameCount: number;
}

interface GamesOverviewIssue {
  readonly gameCode: string;
  readonly gameName: string;
  readonly message: string;
  readonly actionLabel: string;
  readonly action: 'configure-game';
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
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly games = signal<TenantGamePricingView[]>([]);
  readonly matrixSummary = signal<GamesOverviewSummary | null>(null);
  readonly issues = signal<readonly GamesOverviewIssue[]>([]);
  readonly actionErrors = signal<Readonly<Record<string, TenantGameCardError>>>({});
  readonly summaryItems = computed(() => {
    const summary = this.matrixSummary();
    if (!summary) return [];

    return [
      {
        label: 'Jeux système',
        value: summary.catalogGameCount,
        help: 'Jeux disponibles pour ce tenant.',
      },
      {
        label: 'Jeux actifs',
        value: summary.activeGameCount,
        help: 'Jeux activés pour la vente.',
      },
      {
        label: 'À configurer',
        value: summary.needsConfigCount,
        help: 'Jeux activés avec mise ou barème incomplet.',
        tone: summary.needsConfigCount > 0 ? 'warning' : 'success',
      },
      {
        label: 'Inactifs',
        value: summary.inactiveGameCount,
        help: 'Jeux disponibles mais non supportés actuellement.',
      },
    ];
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.actionErrors.set({});
    this.api.getGamesPricing({ suppressShellFeedback: true }).subscribe({
      next: games => {
        this.games.set(games);
        this.matrixSummary.set(this.buildOverviewSummary(games));
        this.issues.set(this.buildOverviewIssues(games));
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        const problem = (err as { error?: ProblemDetail })?.error;
        this.pageError.set(this.pageErrorViewModel(problem));
        this.pageState.set('error');
      },
    });
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

  private buildOverviewSummary(games: readonly TenantGamePricingView[]): GamesOverviewSummary {
    return {
      catalogGameCount:  games.filter(game => game.catalogStatus === 'AVAILABLE').length,
      activeGameCount:   games.filter(game => game.tenantStatus === 'ACTIVE').length,
      needsConfigCount:  games.filter(game => game.tenantStatus === 'NEEDS_CONFIG').length,
      inactiveGameCount: games.filter(game => game.tenantStatus === 'INACTIVE').length,
    };
  }

  private buildOverviewIssues(games: readonly TenantGamePricingView[]): readonly GamesOverviewIssue[] {
    const issues: GamesOverviewIssue[] = [];

    for (const game of games) {
      if (game.catalogStatus !== 'AVAILABLE' || game.tenantStatus === 'UNAVAILABLE') continue;

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
    }

    return issues.slice(0, 6);
  }

  private hasGameStakeConfig(game: TenantGamePricingView): boolean {
    return game.limits.minStake !== null && game.limits.maxStake !== null;
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
