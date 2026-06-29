import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
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
import { GamesPricingSummaryComponent } from '../../components/games-pricing-summary/games-pricing-summary.component';
import { TenantGameCardComponent, TenantGameCardError } from '../../components/tenant-game-card/tenant-game-card.component';
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
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly games = signal<TenantGamePricingView[]>([]);
  readonly actionErrors = signal<Readonly<Record<string, TenantGameCardError>>>({});

  ngOnInit(): void { this.load(); }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.actionErrors.set({});
    this.api.getGamesPricing({ suppressShellFeedback: true }).subscribe({
      next: data => { this.games.set(data); this.pageState.set('ready'); },
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
