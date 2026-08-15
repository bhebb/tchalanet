import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';
import { TchSectionErrorSeverity } from '@tch/ui/components';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import {
  consoleGameLogoUrl,
  consoleGameLogoText,
} from '@tch/web/console';
import { TenantGamePricingView, TenantGameStatus } from '../../data-access/admin-games-pricing.models';

const STATUS_TONE: Record<TenantGameStatus, AdminStatusTone> = {
  ACTIVE:       'success',
  NEEDS_CONFIG: 'warning',
  INACTIVE:     'neutral',
  UNAVAILABLE:  'danger',
};

const MARYAJ_GRATIS_GAME_CODES = new Set(['HT_MARYAJ_GRATIS', 'HT_MARYAJ_GRATUIT']);
type TenantGameListStatus = 'active' | 'attention' | 'inactive' | 'unavailable';
type TenantGameAvailabilityState = 'available' | 'attention' | 'unavailable';

export interface TenantGameCardError {
  readonly title: string;
  readonly message: string;
  readonly severity?: TchSectionErrorSeverity;
}

@Component({
  selector: 'tch-tenant-game-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, TranslatePipe, AdminStatusPillComponent],
  templateUrl: './tenant-game-card.component.html',
  styleUrls: ['./tenant-game-card.component.scss'],
})
export class TenantGameCardComponent {
  private readonly translate = inject(TranslateService);

  readonly game = input.required<TenantGamePricingView>();
  readonly actionError = input<TenantGameCardError | null>(null);
  readonly saving = input(false);

  readonly activate  = output<string>();
  readonly disable   = output<string>();
  readonly configure = output<string>();

  readonly logoUrl = computed(() => consoleGameLogoUrl(this.game().gameCode));
  readonly logoText = computed(() => consoleGameLogoText(this.game().gameCode, this.game().gameName));

  onToggle(enabled: boolean): void {
    const game = this.game();
    if (game.tenantStatus === 'UNAVAILABLE') return;
    if (enabled) this.activate.emit(game.gameCode);
    else this.disable.emit(game.gameCode);
  }

  onConfigure(): void {
    this.configure.emit(this.game().gameCode);
  }

  private hasStakeConfig(game: TenantGamePricingView): boolean {
    const l = game.limits;
    return l.minStake !== null && l.maxStake !== null;
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

  private hasBlockingItems(game: TenantGamePricingView): boolean {
    return !this.hasStakeConfig(game) || !this.hasPricingConfig(game);
  }

  private hasPricingConfig(game: TenantGamePricingView): boolean {
    return game.odds.length > 0 && game.odds.every(odd => odd.odds !== null && odd.odds !== undefined);
  }

  cardStatusTone(game: TenantGamePricingView): AdminStatusTone {
    if (game.tenantStatus !== 'UNAVAILABLE' && this.hasBlockingItems(game)) return 'warning';
    return STATUS_TONE[game.tenantStatus];
  }

  cardStatus(game: TenantGamePricingView): TenantGameListStatus {
    if (game.tenantStatus === 'UNAVAILABLE') return 'unavailable';
    if (game.tenantStatus === 'INACTIVE') return 'inactive';
    if (this.hasBlockingItems(game)) return 'attention';
    return 'active';
  }

  statusLabelKey(game: TenantGamePricingView): string {
    if (game.tenantStatus !== 'UNAVAILABLE' && this.hasBlockingItems(game)) {
      return 'admin.gamesPricing.readiness.TODO';
    }
    return `admin.gamesPricing.status.${game.tenantStatus}`;
  }

  saleEnabled(game: TenantGamePricingView): boolean {
    return game.tenantStatus === 'ACTIVE' || game.tenantStatus === 'NEEDS_CONFIG';
  }

  posLabelKey(game: TenantGamePricingView): string {
    return game.visibleInPos ? 'admin.gamesPricing.card.posYes' : 'admin.gamesPricing.card.posNo';
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

  isMaryajGratis(gameCode: string): boolean {
    return MARYAJ_GRATIS_GAME_CODES.has(gameCode);
  }

  configureLabelKey(game: TenantGamePricingView): string {
    if (this.isMaryajGratis(game.gameCode)) return 'admin.gamesPricing.card.action.maryaj';
    return this.hasBlockingItems(game)
      ? 'admin.gamesPricing.card.action.complete'
      : 'admin.gamesPricing.card.action.configure';
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
