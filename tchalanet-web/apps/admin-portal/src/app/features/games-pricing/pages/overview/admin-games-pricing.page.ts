import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { webAppErrorFromNotice, webAppErrorFromProblemDetail } from '@tch/api';
import type { ApiNotice, ApiResponse, ProblemDetail } from '@tch/api';
import { AccessService } from '@tch/core/auth';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { TchConfirmDialog, type TchConfirmDialogData, TchSectionError } from '@tch/ui/components';
import {
  resourceErrorVm,
  tchMutation,
  TchAsyncReadyDirective,
  TchAsyncSource,
  TchAsyncViewComponent,
} from '@tch/web/async';
import { AdminGamesPricingApiService } from '../../data-access/admin-games-pricing-api.service';
import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';
import {
  GamesSetupIssue,
  GamesSetupIssuesComponent,
} from '../../components/games-setup-issues/games-setup-issues.component';
import {
  GamesSetupSummaryComponent,
  GamesSetupSummaryItem,
} from '../../components/games-setup-summary/games-setup-summary.component';
import { TenantGameCardError } from '../../components/tenant-game-card/tenant-game-card.component';
import { TenantGamesGridComponent } from '../../components/tenant-games-grid/tenant-games-grid.component';
import {
  GAME_SETTINGS_DIALOG_SURFACE_CONFIG,
  GameSettingsDialog,
} from '../../components/dialogs/game-settings.dialog';

interface GamesOverviewSummary {
  readonly catalogGameCount: number;
  readonly activeGameCount: number;
  readonly needsConfigCount: number;
  readonly inactiveGameCount: number;
}

const MARYAJ_GRATIS_GAME_CODES = new Set(['HT_MARYAJ_GRATIS', 'HT_MARYAJ_GRATUIT']);

@Component({
  selector: 'tch-admin-games-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    TranslatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchSectionError,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    GamesSetupIssuesComponent,
    GamesSetupSummaryComponent,
    TenantGamesGridComponent,
  ],
  templateUrl: './admin-games-pricing.page.html',
  styleUrls: ['./admin-games-pricing.page.scss'],
})
export class AdminGamesPricingPage {
  private readonly api = inject(AdminGamesPricingApiService);
  private readonly dialog = inject(MatDialog);
  private readonly access = inject(AccessService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  readonly fromSetup = computed(() => this.queryParamMap().get('from') === 'setup');
  readonly setupReturnFragment = 'setup-required-title';
  readonly canManageGames = computed(() => this.access.can({ permission: 'game-pricing.update' }));
  readonly gamesResponseResource = this.api.getGamesPricingResponseResource({
    suppressShellFeedback: true,
  });
  readonly gamesResource: TchAsyncSource<readonly TenantGamePricingView[]> = {
    status: () => this.gamesResponseResource.status(),
    value: () => this.gamesResponseResource.value()?.data,
    error: () => this.gamesResponseResource.error(),
  };
  readonly gamesError = resourceErrorVm(this.gamesResource, 'admin.setup.games_pricing');
  readonly games = computed(() => this.gamesResource.value() ?? []);
  readonly degradationErrors = computed(() => {
    const response = this.gamesResponseResource.value();
    if (!response?.notices?.length) return [];
    return response.notices.map(notice => this.noticeError(notice, response));
  });
  readonly matrixSummary = computed(() => this.buildOverviewSummary(this.games()));
  readonly issues = computed(() => this.buildOverviewIssues(this.games()));
  readonly actionErrors = signal<Readonly<Record<string, TenantGameCardError>>>({});
  readonly pendingGameEnabled = signal<Readonly<Record<string, boolean>>>({});
  readonly savingGames = computed<ReadonlySet<string>>(() => {
    const pending = new Set<string>();
    for (const game of this.games()) {
      if (this.enableGame.pending(game.gameCode) || this.disableGame.pending(game.gameCode)) {
        pending.add(game.gameCode);
      }
    }
    return pending;
  });
  readonly enableGame = tchMutation<string, void>({
    run: gameCode => this.api.enableGame(gameCode, { suppressShellFeedback: true }),
    source: 'admin.setup.games_pricing.enable',
    onSuccess: () => this.load(true),
    onError: (err, gameCode) => {
      this.clearPendingGameEnabled(gameCode);
      this.setActionError(gameCode, err);
      return true;
    },
  });
  readonly disableGame = tchMutation<string, void>({
    run: gameCode => this.api.disableGame(gameCode, { suppressShellFeedback: true }),
    source: 'admin.setup.games_pricing.disable',
    onSuccess: () => this.load(true),
    onError: (err, gameCode) => {
      this.clearPendingGameEnabled(gameCode);
      this.setActionError(gameCode, err);
      return true;
    },
  });
  readonly summaryItems = computed<readonly GamesSetupSummaryItem[]>(() => {
    const summary = this.matrixSummary();

    return [
      {
        labelKey: 'admin.gamesPricing.summary.compact.games',
        value: summary.catalogGameCount,
        helpKey: 'admin.gamesPricing.summary.systemGames.help',
      },
      {
        labelKey: 'admin.gamesPricing.summary.compact.active',
        value: summary.activeGameCount,
        helpKey: 'admin.gamesPricing.summary.activeGames.help',
      },
      {
        labelKey: 'admin.gamesPricing.summary.compact.needsConfig',
        value: summary.needsConfigCount,
        helpKey: 'admin.gamesPricing.summary.needsConfig.help',
        tone: summary.needsConfigCount > 0 ? 'warning' : 'success',
      },
    ];
  });

  load(preserveActionFeedback = false): void {
    this.actionErrors.set({});
    if (!preserveActionFeedback) this.pendingGameEnabled.set({});
    this.gamesResponseResource.reload();
  }

  onActivate(gameCode: string): void {
    if (!this.canManageGames()) return;
    this.clearActionError(gameCode);
    this.executeEnableGame(gameCode);
  }

  onDisable(gameCode: string): void {
    if (!this.canManageGames()) return;
    this.clearActionError(gameCode);
    this.confirmDisableGame(gameCode);
  }

  onConfigure(gameCode: string): void {
    if (!this.canManageGames()) return;
    if (MARYAJ_GRATIS_GAME_CODES.has(gameCode)) {
      void this.router.navigate(['/app/admin/maryaj-gratis'], {
        queryParams: this.setupFlowQueryParams(),
        fragment: 'game',
      });
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
      visibleInPos: game.visibleInPos,
      displayOrder: 0,
      minStake: game.limits.minStake,
      maxStake: game.limits.maxStake,
      availabilityEnabled: false,
      availabilityDays: null,
      startLocalTime: null,
      endLocalTime: null,
      readyForSale: game.readiness.status === 'READY',
      betOptions: game.odds,
      betOptionGroups: game.oddsGroups,
    };

    const ref = this.dialog.open(GameSettingsDialog, {
      data: { game: dialogGame },
      ...GAME_SETTINGS_DIALOG_SURFACE_CONFIG,
    });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.load();
    });
  }

  onIssueAction(issue: GamesSetupIssue): void {
    this.onConfigure(issue.gameCode);
  }

  private setActionError(gameCode: string, err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    const error = this.errorCopy(problem, `admin.setup.games_pricing.${gameCode}`, 'section');
    this.actionErrors.update(current => ({
      ...current,
      [gameCode]: error,
    }));
  }

  private confirmDisableGame(gameCode: string): void {
    const game = this.games().find(candidate => candidate.gameCode === gameCode);
    const name = game?.gameName ?? gameCode;
    this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.gamesPricing.confirm.disableTitle', { name }),
          message: this.translate.instant('admin.gamesPricing.confirm.disableMessage'),
          confirmLabel: this.translate.instant('admin.gamesPricing.confirm.disableAction'),
          cancelLabel: this.translate.instant('common.cancel'),
          destructive: true,
          icon: 'block',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.confirmed) this.executeDisableGame(gameCode);
      });
  }

  private executeEnableGame(gameCode: string): void {
    this.setPendingGameEnabled(gameCode, true);
    this.enableGame.execute(gameCode, { key: gameCode });
  }

  private executeDisableGame(gameCode: string): void {
    this.setPendingGameEnabled(gameCode, false);
    this.disableGame.execute(gameCode, { key: gameCode });
  }

  private setPendingGameEnabled(gameCode: string, enabled: boolean): void {
    this.pendingGameEnabled.update(current => ({ ...current, [gameCode]: enabled }));
  }

  private clearPendingGameEnabled(gameCode: string): void {
    this.pendingGameEnabled.update(current => {
      if (current[gameCode] === undefined) return current;
      const next = { ...current };
      delete next[gameCode];
      return next;
    });
  }

  private clearActionError(gameCode: string): void {
    this.actionErrors.update(current => {
      if (!current[gameCode]) return current;
      const next = { ...current };
      delete next[gameCode];
      return next;
    });
  }

  protected setupFlowQueryParams(): Record<string, string> | undefined {
    return this.fromSetup() ? { from: 'setup' } : undefined;
  }

  private buildOverviewSummary(games: readonly TenantGamePricingView[]): GamesOverviewSummary {
    return {
      catalogGameCount: games.filter(game => game.catalogStatus === 'AVAILABLE').length,
      activeGameCount: games.filter(game => game.tenantStatus === 'ACTIVE').length,
      needsConfigCount: games.filter(game => game.tenantStatus === 'NEEDS_CONFIG').length,
      inactiveGameCount: games.filter(game => game.tenantStatus === 'INACTIVE').length,
    };
  }

  private buildOverviewIssues(games: readonly TenantGamePricingView[]): readonly GamesSetupIssue[] {
    const issues: GamesSetupIssue[] = [];

    for (const game of games) {
      if (game.catalogStatus !== 'AVAILABLE' || game.tenantStatus === 'UNAVAILABLE') continue;

      if (
        game.tenantStatus === 'NEEDS_CONFIG' ||
        !this.hasGameStakeConfig(game) ||
        !this.hasGamePricingConfig(game)
      ) {
        issues.push({
          gameCode: game.gameCode,
          gameName: game.gameName,
          messageKey: this.hasGameStakeConfig(game)
            ? 'admin.gamesPricing.issues.missingPricing'
            : 'admin.gamesPricing.issues.missingStake',
          actionLabelKey: 'admin.gamesPricing.issues.configure',
          action: 'configure-game',
          tone: 'danger',
        });
        continue;
      }
    }

    return issues.slice(0, 6);
  }

  private hasGameStakeConfig(game: TenantGamePricingView): boolean {
    return game.limits.minStake !== null && game.limits.maxStake !== null;
  }

  private hasGamePricingConfig(game: TenantGamePricingView): boolean {
    return (
      game.odds.length > 0 && game.odds.every(odd => odd.odds !== null && odd.odds !== undefined)
    );
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

  private noticeError(notice: ApiNotice, response: ApiResponse<readonly TenantGamePricingView[]>) {
    const normalized = webAppErrorFromNotice(
      notice,
      response.trace,
      'admin.setup.games_pricing',
      'section',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return {
      severity: normalized.severity,
      title: copy.title,
      message: copy.message,
    } satisfies TenantGameCardError;
  }
}
