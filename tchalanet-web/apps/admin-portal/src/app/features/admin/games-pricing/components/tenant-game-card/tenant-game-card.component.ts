import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { TchSectionErrorSeverity } from '@tch/ui/components';
import { AdminStatusTone } from '@tch/ui/console';
import {
  ConsoleGameCardActionEvent,
  ConsoleGameCardComponent,
  ConsoleGameCardView,
  consoleGameLogoText,
} from '@tch/web/console';
import { TenantGamePricingView, TenantGameStatus, ReadinessStatus } from '../../data-access/admin-games-pricing.models';

const STATUS_TONE: Record<TenantGameStatus, AdminStatusTone> = {
  ACTIVE:       'success',
  NEEDS_CONFIG: 'warning',
  INACTIVE:     'neutral',
  UNAVAILABLE:  'danger',
};

const READINESS_BADGE: Record<ReadinessStatus, ConsoleGameCardView['badgeTone']> = {
  READY:   'ready',
  TODO:    'warning',
  BLOCKED: 'blocked',
};

export interface TenantGameCardError {
  readonly title: string;
  readonly message: string;
  readonly severity?: TchSectionErrorSeverity;
}

@Component({
  selector: 'tch-tenant-game-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConsoleGameCardComponent],
  templateUrl: './tenant-game-card.component.html',
})
export class TenantGameCardComponent {
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly game = input.required<TenantGamePricingView>();
  readonly actionError = input<TenantGameCardError | null>(null);

  readonly activate  = output<string>();
  readonly disable   = output<string>();
  readonly configure = output<string>();

  readonly card = computed<ConsoleGameCardView>(() => {
    const game = this.game();
    return {
      id: game.gameCode,
      code: game.gameCode,
      name: game.gameName,
      logoText: consoleGameLogoText(game.gameCode, game.gameName),
      statusLabel: this.t(`admin.gamesPricing.status.${game.tenantStatus}`),
      statusTone: STATUS_TONE[game.tenantStatus],
      badgeLabel: this.t(`admin.gamesPricing.readiness.${game.readiness.status}`),
      badgeTone: READINESS_BADGE[game.readiness.status],
      unavailable: game.tenantStatus === 'UNAVAILABLE',
      unavailableLabel: this.t('admin.gamesPricing.card.unavailable'),
      summaryItems: game.tenantStatus === 'UNAVAILABLE' ? [] : [
        { icon: 'casino', label: this.t('admin.gamesPricing.card.systemGame', { name: game.gameName }) },
        { icon: 'payments', label: this.stakeLabel(game), warning: !this.hasStakeConfig(game) },
        { icon: 'price_change', label: this.pricingLabel(game), warning: game.odds.length === 0 },
        { icon: 'shield', label: this.limitLabel(game), warning: game.limits.maxPerDraw === null },
      ],
      actions: this.primaryActions(game),
      secondaryActions: game.tenantStatus === 'UNAVAILABLE' ? [] : [
        ...(game.gameCode === 'HT_MARYAJ_GRATUIT'
          ? [{ id: 'maryaj-gratis', label: this.t('admin.gamesPricing.card.action.maryaj'), icon: 'redeem' }]
          : []),
        { id: 'limits', label: this.t('admin.gamesPricing.card.action.limits'), icon: 'shield' },
        { id: 'pricing', label: this.t('admin.gamesPricing.card.action.pricing'), icon: 'format_list_numbered' },
      ],
    };
  });

  onCardAction(event: ConsoleGameCardActionEvent): void {
    switch (event.action.id) {
      case 'activate':
        this.activate.emit(event.row.code);
        break;
      case 'disable':
        this.disable.emit(event.row.code);
        break;
      case 'configure':
        this.configure.emit(event.row.code);
        break;
      case 'maryaj-gratis':
        void this.router.navigate(['/app/admin/maryaj-gratis'], { fragment: 'game' });
        break;
      case 'limits':
        void this.router.navigate(['/app/admin/limits']);
        break;
      case 'pricing':
        void this.router.navigate(['/app/admin/pricing']);
        break;
    }
  }

  private hasStakeConfig(game: TenantGamePricingView): boolean {
    const l = game.limits;
    return l.minStake !== null && l.maxStake !== null;
  }

  private stakeLabel(game: TenantGamePricingView): string {
    if (game.tenantStatus === 'UNAVAILABLE') return this.t('admin.gamesPricing.card.stakeUnavailable');
    return this.hasStakeConfig(game)
      ? this.t('admin.gamesPricing.card.stakeConfigured')
      : this.t('admin.gamesPricing.card.stakeMissing');
  }

  private pricingLabel(game: TenantGamePricingView): string {
    const oddsCount = game.odds.length;
    if (oddsCount === 0) return this.t('admin.gamesPricing.card.pricingMissing');
    const profile = game.pricingProfileLabel;
    const countLabel = this.t('admin.gamesPricing.card.pricingOptions', { count: oddsCount });
    return profile
      ? this.t('admin.gamesPricing.card.pricingOptionsWithProfile', {
          profile: this.pricingProfileLabel(profile),
          count: oddsCount,
        })
      : countLabel;
  }

  private limitLabel(game: TenantGamePricingView): string {
    return game.limits.maxPerDraw === null
      ? this.t('admin.gamesPricing.card.limitMissing')
      : this.t('admin.gamesPricing.card.limitConfigured');
  }

  private primaryActions(game: TenantGamePricingView): ConsoleGameCardView['actions'] {
    switch (game.tenantStatus) {
      case 'ACTIVE':
        return [
          { id: 'configure', label: this.t('admin.gamesPricing.card.action.configure') },
          { id: 'disable', label: this.t('admin.gamesPricing.card.action.disable'), tone: 'danger' },
        ];
      case 'NEEDS_CONFIG':
        return [
          { id: 'configure', label: this.t('admin.gamesPricing.card.action.configure'), tone: 'primary' },
          { id: 'activate', label: this.t('admin.gamesPricing.card.action.activate'), disabled: true },
        ];
      case 'INACTIVE':
        return [
          { id: 'activate', label: this.t('admin.gamesPricing.card.action.reactivate'), tone: 'primary' },
          { id: 'configure', label: this.t('admin.gamesPricing.card.action.edit') },
        ];
      case 'UNAVAILABLE':
        return [{ id: 'unavailable', label: this.t('admin.gamesPricing.card.action.unavailable'), disabled: true }];
    }
  }

  private pricingProfileLabel(profile: string): string {
    return profile === 'Barème standard'
      ? this.t('admin.gamesPricing.card.standardPricingProfile')
      : profile;
  }

  private t(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
  }
}
