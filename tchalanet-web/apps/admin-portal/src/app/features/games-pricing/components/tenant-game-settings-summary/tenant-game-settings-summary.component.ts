import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';

const MARYAJ_GRATIS_GAME_CODES = new Set(['HT_MARYAJ_GRATIS', 'HT_MARYAJ_GRATUIT']);

type TenantGameAvailabilityState = 'available' | 'attention' | 'unavailable';
type TenantGameSettingsSummaryPresentation = 'row' | 'summary';
type TenantGameSettingsActionVariant = 'flat' | 'stroked';

@Component({
  selector: 'tch-tenant-game-settings-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, RouterLink, TranslatePipe],
  templateUrl: './tenant-game-settings-summary.component.html',
  styleUrls: ['./tenant-game-settings-summary.component.scss'],
})
export class TenantGameSettingsSummaryComponent {
  private readonly translate = inject(TranslateService);

  readonly game = input.required<TenantGamePricingView>();
  readonly presentation = input<TenantGameSettingsSummaryPresentation>('row');
  readonly actionLabelKey = input('admin.gamesPricing.card.action.configure');
  readonly actionVariant = input<TenantGameSettingsActionVariant>('flat');
  readonly actionTestId = input<string | null>(null);
  readonly canManage = input(true);
  readonly saving = input(false);
  readonly availabilityRoute = input<string | null>(null);

  readonly configure = output<TenantGamePricingView>();

  onConfigure(): void {
    const game = this.game();
    if (!this.canManage() || game.tenantStatus === 'UNAVAILABLE') return;
    this.configure.emit(game);
  }

  stakeLabel(game: TenantGamePricingView): string {
    if (game.tenantStatus === 'UNAVAILABLE') return this.t('admin.gamesPricing.card.stakeUnavailable');
    return this.hasStakeConfig(game)
      ? this.t('admin.gamesPricing.card.stakeRange', {
          min: game.limits.minStake,
          max: game.limits.maxStake,
          currency: game.limits.currency,
        })
      : this.t('admin.gamesPricing.card.stakeMissing');
  }

  pricingLabel(game: TenantGamePricingView): string {
    if (this.isMaryajGratis(game.gameCode) && this.hasPricingConfig(game)) {
      return this.t('admin.gamesPricing.card.maryajPricing');
    }

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

  availabilityLabelKey(game: TenantGamePricingView): string {
    if (game.tenantStatus === 'UNAVAILABLE' || game.tenantStatus === 'INACTIVE') {
      return 'admin.gamesPricing.card.availabilityUnavailable';
    }
    if (this.hasBlockingItems(game)) return 'admin.gamesPricing.card.availabilityNeedsConfig';
    return 'admin.gamesPricing.card.availabilityReview';
  }

  availabilityState(game: TenantGamePricingView): TenantGameAvailabilityState {
    if (game.tenantStatus === 'UNAVAILABLE' || game.tenantStatus === 'INACTIVE') {
      return 'unavailable';
    }
    if (this.hasBlockingItems(game)) return 'attention';
    return 'available';
  }

  availabilityIcon(game: TenantGamePricingView): string {
    const state = this.availabilityState(game);
    if (state === 'available') return 'event_available';
    if (state === 'attention') return 'warning';
    return 'event_busy';
  }

  formatAmount(value: number | null, currency: string): string {
    if (value === null) return this.t('admin.maryajGratis.game.notDefined');
    return `${value.toLocaleString('fr')} ${currency}`;
  }

  maryajOddsSummary(game: TenantGamePricingView): string {
    const labels = game.odds.map(odd => this.gainOptionLabel(odd.pricingVariantCode, odd.label));
    return labels.length ? labels.join(' + ') : this.t('common.not_available');
  }

  pricingProfileSummary(game: TenantGamePricingView): string {
    return game.pricingProfileLabel
      ? this.t(game.pricingProfileLabel)
      : this.t('admin.maryajGratis.game.standard');
  }

  gainOptionLabel(pricingVariantCode: string | null, fallback: string): string {
    if (pricingVariantCode === 'MARRIAGE_EXACT_ORDER') {
      return this.t('admin.maryajGratis.game.exact');
    }
    if (pricingVariantCode === 'MARRIAGE_REVERSE_ALLOWED') {
      return this.t('admin.maryajGratis.game.reverse');
    }
    return fallback;
  }

  hasBlockingItems(game: TenantGamePricingView): boolean {
    return !this.hasStakeConfig(game) || !this.hasPricingConfig(game);
  }

  private hasStakeConfig(game: TenantGamePricingView): boolean {
    const l = game.limits;
    return l.minStake !== null && l.maxStake !== null;
  }

  private hasPricingConfig(game: TenantGamePricingView): boolean {
    return game.odds.length > 0 && game.odds.every(odd => odd.odds !== null && odd.odds !== undefined);
  }

  private isMaryajGratis(gameCode: string): boolean {
    return MARYAJ_GRATIS_GAME_CODES.has(gameCode);
  }

  private pricingProfileLabel(profile: string): string {
    const label = profile.startsWith('admin.') ? this.t(profile) : profile;
    const fieldLabel = this.t('admin.gamesPricing.card.fact.payouts');
    return label.toLocaleLowerCase().startsWith(fieldLabel.toLocaleLowerCase())
      ? label.slice(fieldLabel.length).trim()
      : label;
  }

  private t(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
  }
}
